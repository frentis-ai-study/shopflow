package com.shopflow.inventory.scheduler;

import com.shopflow.inventory.domain.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 재고 선점 TTL 만료 스윕(SC-005). 60초 주기로 만료 선점을 해제한다.
 * 상태 전이가 조건부(HELD→RELEASED)라 다중 인스턴스에서도 이중 감소하지 않는다.
 */
@Component
public class ExpirySweeper {

    private final ReservationService reservationService;

    public ExpirySweeper(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${shopflow.reservation.sweep-interval-ms:60000}")
    public void sweep() {
        reservationService.sweepExpired();
    }
}
