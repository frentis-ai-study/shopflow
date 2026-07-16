package com.shopflow.inventory.domain;

import com.shopflow.common.error.DomainException;
import com.shopflow.inventory.repository.StockReservationRepository;
import com.shopflow.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 재고 선점/확정/해제(ADR-0004). 상품 재고는 네이티브 조건부 UPDATE로 원자 처리하고,
 * 선점 레코드 상태 전이도 조건부로 원자화해 이중 처리를 막는다.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    static final Duration TTL = Duration.ofMinutes(10);

    private final ProductRepository products;
    private final StockReservationRepository reservations;
    private final Clock clock;

    public ReservationService(ProductRepository products, StockReservationRepository reservations, Clock clock) {
        this.products = products;
        this.reservations = reservations;
        this.clock = clock;
    }

    /** 선점. 가용재고 부족이면 409(초과판매 방지, SC-003). */
    @Transactional
    public StockReservation reserve(Long productId, Long buyerId, int quantity) {
        int updated = products.reserveStock(productId, quantity);
        if (updated == 0) {
            throw DomainException.conflict("재고가 부족합니다(품절)");
        }
        Instant now = Instant.now(clock);
        StockReservation reservation =
                new StockReservation(productId, buyerId, quantity, now, now.plus(TTL));
        return reservations.save(reservation);
    }

    /** 확정 차감(결제 성공). HELD일 때만 실재고 차감. */
    @Transactional
    public void confirm(StockReservation reservation) {
        int moved = reservations.transition(reservation.getId(),
                ReservationStatus.HELD, ReservationStatus.CONFIRMED);
        if (moved == 1) {
            products.confirmStock(reservation.getProductId(), reservation.getQuantity());
        }
    }

    /** 해제(결제 실패/취소). HELD일 때만 reserved 복원. */
    @Transactional
    public void release(StockReservation reservation) {
        int moved = reservations.transition(reservation.getId(),
                ReservationStatus.HELD, ReservationStatus.RELEASED);
        if (moved == 1) {
            products.releaseStock(reservation.getProductId(), reservation.getQuantity());
        }
    }

    /** 만료 스윕: HELD & 만료된 선점을 해제해 재고 복원(SC-005). */
    @Transactional
    public int sweepExpired() {
        Instant now = Instant.now(clock);
        List<StockReservation> expired =
                reservations.findByStatusAndExpiresAtBefore(ReservationStatus.HELD, now);
        int released = 0;
        for (StockReservation r : expired) {
            int moved = reservations.transition(r.getId(),
                    ReservationStatus.HELD, ReservationStatus.RELEASED);
            if (moved == 1) {
                products.releaseStock(r.getProductId(), r.getQuantity());
                released++;
            }
        }
        if (released > 0) {
            log.info("만료 선점 {}건 해제(재고 복원)", released);
        }
        return released;
    }
}
