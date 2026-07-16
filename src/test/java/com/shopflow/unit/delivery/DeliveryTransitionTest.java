package com.shopflow.unit.delivery;

import com.shopflow.common.error.DomainException;
import com.shopflow.delivery.domain.Delivery;
import com.shopflow.delivery.domain.DeliveryStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 배송 상태 전이 규칙 — 허용/금지(R4, FR-011). */
class DeliveryTransitionTest {

    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void 생성시_PENDING() {
        assertThat(new Delivery(1L).getStatus()).isEqualTo(DeliveryStatus.PENDING);
    }

    @Test
    void PENDING에서_SHIPPING_그리고_DELIVERED() {
        Delivery d = new Delivery(1L);
        d.ship(NOW);
        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SHIPPING);
        d.deliver(NOW);
        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    void 건너뛰기_PENDING에서_DELIVERED_금지() {
        Delivery d = new Delivery(1L);
        assertThatThrownBy(() -> d.deliver(NOW)).isInstanceOf(DomainException.class);
    }

    @Test
    void 역행_재배송중_금지() {
        Delivery d = new Delivery(1L);
        d.ship(NOW);
        assertThatThrownBy(() -> d.ship(NOW)).isInstanceOf(DomainException.class);
    }
}
