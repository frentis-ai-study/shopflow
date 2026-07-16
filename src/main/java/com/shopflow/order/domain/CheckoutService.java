package com.shopflow.order.domain;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.repository.SellerRepository;
import com.shopflow.common.domain.Address;
import com.shopflow.common.error.DomainException;
import com.shopflow.delivery.domain.DeliveryService;
import com.shopflow.inventory.domain.ReservationService;
import com.shopflow.inventory.domain.StockReservation;
import com.shopflow.common.domain.Money;
import com.shopflow.order.domain.OrderService.LineSpec;
import com.shopflow.order.domain.OrderService.PlacedOrder;
import com.shopflow.order.domain.OrderService.SubOrderRef;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.payment.domain.IdempotencyStore;
import com.shopflow.payment.domain.PaymentGateway.PaymentResult;
import com.shopflow.payment.domain.PaymentService;
import com.shopflow.payment.repository.PaymentIdempotencyRepository;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import com.shopflow.product.domain.ProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 체크아웃 오케스트레이션(UC-09). 단일 트랜잭션으로 금액검증→멱등→선점→결제→주문/하위주문
 * →배송 생성을 처리한다. 실패 경로에서 선점을 해제하고 멱등 상태를 정리한다.
 */
@Service
public class CheckoutService {

    public enum Result { CREATED, EXISTING, REJECTED }

    public record CheckoutResult(Result result, Long orderId, long totalKrw, String message) {
    }

    private final CartService cartService;
    private final ProductService productService;
    private final SellerRepository sellers;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final IdempotencyStore idempotencyStore;
    private final PaymentIdempotencyRepository idempotencyRepo;
    private final OrderService orderService;
    private final DeliveryService deliveryService;
    private final OrderRepository orderRepository;

    public CheckoutService(CartService cartService, ProductService productService,
                           SellerRepository sellers, ReservationService reservationService,
                           PaymentService paymentService, IdempotencyStore idempotencyStore,
                           PaymentIdempotencyRepository idempotencyRepo, OrderService orderService,
                           DeliveryService deliveryService, OrderRepository orderRepository) {
        this.cartService = cartService;
        this.productService = productService;
        this.sellers = sellers;
        this.reservationService = reservationService;
        this.paymentService = paymentService;
        this.idempotencyStore = idempotencyStore;
        this.idempotencyRepo = idempotencyRepo;
        this.orderService = orderService;
        this.deliveryService = deliveryService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public CheckoutResult checkout(Long buyerId, Address address, String idempotencyKey) {
        // 멱등 조기 확인: 완료된 요청의 재시도는 장바구니 상태와 무관하게 기존 주문을 반환한다
        // (성공 후 장바구니가 비워지므로 빈 장바구니 검사보다 먼저 처리, SC-004).
        var prior = idempotencyRepo.findById(idempotencyKey);
        if (prior.isPresent()) {
            return switch (prior.get().getStatus()) {
                case DONE -> {
                    Long priorOrderId = prior.get().getOrderId();
                    long priorTotal = orderRepository.findById(priorOrderId)
                            .map(Order::getTotalKrw)
                            .orElse(0L);
                    yield new CheckoutResult(Result.EXISTING, priorOrderId, priorTotal, "이미 처리된 주문입니다");
                }
                case STARTED -> throw DomainException.conflict("결제가 이미 진행 중입니다");
                case FAILED -> throw DomainException.conflict("이전 결제가 실패했습니다. 다시 시도해 주세요");
            };
        }

        List<CartItem> items = cartService.items(buyerId);
        if (items.isEmpty()) {
            throw DomainException.badRequest("장바구니가 비어 있습니다");
        }

        // 서버측 금액 계산·검증 + 라인 스펙(스냅샷) 구성 (FR-019)
        List<LineSpec> lines = new ArrayList<>();
        Money total = Money.ZERO;
        for (CartItem item : items) {
            Product product = productService.get(item.getProductId());
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw DomainException.conflict("판매 중이 아닌 상품이 있습니다: " + product.getName());
            }
            Seller seller = sellers.findById(product.getSellerId())
                    .orElseThrow(() -> DomainException.conflict("판매자 정보를 찾을 수 없습니다"));
            if (!seller.isActive()) {
                throw DomainException.conflict("판매 정지된 판매자의 상품이 있습니다");
            }
            lines.add(new LineSpec(product.getId(), seller.getId(), seller.getStoreName(),
                    product.getName(), product.getPriceKrw(), item.getQuantity()));
            total = total.plus(Money.won(product.getPriceKrw()).times(item.getQuantity()));
        }
        String requestHash = requestHash(buyerId, total.amountKrw(), lines);

        // 멱등 시작(독립 트랜잭션). 재요청은 기존 결과 반환(SC-004).
        IdempotencyStore.BeginResult begin;
        try {
            begin = idempotencyStore.begin(idempotencyKey, requestHash);
        } catch (org.springframework.dao.DataIntegrityViolationException concurrent) {
            // 동시 삽입 경합에서 진 요청 — 중복 처리 방지(이중결제 0)
            throw DomainException.conflict("결제가 이미 진행 중입니다");
        }
        switch (begin.outcome()) {
            case ALREADY_DONE -> {
                return new CheckoutResult(Result.EXISTING, begin.orderId(), total.amountKrw(), "이미 처리된 주문입니다");
            }
            case IN_PROGRESS -> throw DomainException.conflict("결제가 이미 진행 중입니다");
            case PREVIOUSLY_FAILED -> throw DomainException.conflict("이전 결제가 실패했습니다. 다시 시도해 주세요");
            case STARTED -> { /* 우리가 소유 — 진행 */ }
        }

        try {
            return doCheckout(buyerId, address, idempotencyKey, total.amountKrw(), lines);
        } catch (RuntimeException e) {
            idempotencyStore.markFailed(idempotencyKey);
            throw e;
        }
    }

    /** 선점→결제→주문/배송 생성. checkout()의 트랜잭션에서 실행된다(같은 빈 호출이지만 checkout이 @Transactional). */
    private CheckoutResult doCheckout(Long buyerId, Address address, String idempotencyKey,
                                      long total, List<LineSpec> lines) {
        // 1) 재고 선점(부족 시 conflict → 롤백)
        List<StockReservation> reservations = new ArrayList<>();
        for (LineSpec line : lines) {
            reservations.add(reservationService.reserve(line.productId(), buyerId, line.quantity()));
        }

        // 2) 결제
        PaymentResult payResult = paymentService.authorize(idempotencyKey, total, "buyer:" + buyerId);
        if (!payResult.isApproved()) {
            // 거절: 선점 해제·실패 결제 기록·멱등 FAILED. (예외 던지지 않고 커밋)
            reservations.forEach(reservationService::release);
            paymentService.record(null, idempotencyKey, total, payResult);
            idempotencyRepo.findById(idempotencyKey).ifPresent(rec -> rec.markFailed());
            return new CheckoutResult(Result.REJECTED, null, total, payResult.reason());
        }

        // 3) 주문 생성(판매자별 분리·스냅샷) + 선점 확정 차감 + 배송 생성
        PlacedOrder placed = orderService.place(buyerId, address, lines);
        reservations.forEach(reservationService::confirm);
        for (SubOrderRef ref : placed.subOrders()) {
            deliveryService.createPending(ref.subOrderId());
        }
        paymentService.record(placed.orderId(), idempotencyKey, total, payResult);
        idempotencyRepo.findById(idempotencyKey).ifPresent(rec -> rec.markDone(placed.orderId()));
        cartService.clear(buyerId);

        return new CheckoutResult(Result.CREATED, placed.orderId(), placed.totalKrw(), "결제가 완료되었습니다");
    }

    private String requestHash(Long buyerId, long total, List<LineSpec> lines) {
        int h = Objects.hash(buyerId, total);
        for (LineSpec l : lines) {
            h = 31 * h + Objects.hash(l.productId(), l.unitPriceKrw(), l.quantity());
        }
        return Integer.toHexString(h);
    }
}
