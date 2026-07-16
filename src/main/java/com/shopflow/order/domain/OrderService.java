package com.shopflow.order.domain;

import com.shopflow.common.domain.Address;
import com.shopflow.order.repository.OrderLineRepository;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.SubOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 주문 생성 — 판매자별 하위주문 분리·상품/판매자명 스냅샷·총액 산출(FR-021, FR-023). */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final SubOrderRepository subOrders;
    private final OrderLineRepository orderLines;
    private final Clock clock;

    public OrderService(OrderRepository orders, SubOrderRepository subOrders,
                        OrderLineRepository orderLines, Clock clock) {
        this.orders = orders;
        this.subOrders = subOrders;
        this.orderLines = orderLines;
        this.clock = clock;
    }

    /** 배송 상태 집계로 주문 상태 갱신(FR-022). */
    @Transactional
    public void updateStatus(Long orderId, OrderStatus status) {
        orders.findById(orderId).ifPresent(o -> o.changeStatus(status));
    }

    @Transactional(readOnly = true)
    public List<SubOrder> subOrdersOf(Long orderId) {
        return subOrders.findByOrderId(orderId);
    }

    /** 결제 시점 라인 스펙(스냅샷 값). */
    public record LineSpec(Long productId, Long sellerId, String sellerStoreName,
                           String productName, long unitPriceKrw, int quantity) {
        long lineTotal() {
            return unitPriceKrw * quantity;
        }
    }

    /** 판매자별 하위주문 참조. */
    public record SubOrderRef(Long subOrderId, Long sellerId) {
    }

    public record PlacedOrder(Long orderId, long totalKrw, List<SubOrderRef> subOrders) {
    }

    /** 결제 성공 시 주문·하위주문·라인 생성. */
    @Transactional
    public PlacedOrder place(Long buyerId, Address address, List<LineSpec> lines) {
        long total = lines.stream().mapToLong(LineSpec::lineTotal).sum();
        Order order = orders.save(new Order(buyerId, total, address, Instant.now(clock)));

        // 판매자별 그룹핑(입력 순서 보존)
        Map<Long, List<LineSpec>> bySeller = new LinkedHashMap<>();
        for (LineSpec line : lines) {
            bySeller.computeIfAbsent(line.sellerId(), k -> new ArrayList<>()).add(line);
        }

        List<SubOrderRef> refs = new ArrayList<>();
        for (Map.Entry<Long, List<LineSpec>> entry : bySeller.entrySet()) {
            List<LineSpec> sellerLines = entry.getValue();
            long subtotal = sellerLines.stream().mapToLong(LineSpec::lineTotal).sum();
            String storeName = sellerLines.get(0).sellerStoreName();
            SubOrder subOrder = subOrders.save(
                    new SubOrder(order.getId(), entry.getKey(), storeName, subtotal));
            for (LineSpec line : sellerLines) {
                orderLines.save(new OrderLine(subOrder.getId(), line.productId(),
                        line.productName(), line.unitPriceKrw(), line.quantity()));
            }
            refs.add(new SubOrderRef(subOrder.getId(), entry.getKey()));
        }
        return new PlacedOrder(order.getId(), total, refs);
    }
}
