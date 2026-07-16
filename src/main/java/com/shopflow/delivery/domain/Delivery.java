package com.shopflow.delivery.domain;

import com.shopflow.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 배송 이행 단위 — SubOrder와 1:1(ADR-0008). 결제 완료 시 PENDING으로 생성. */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false, unique = true)
    private Long subOrderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected Delivery() {
    }

    public Delivery(Long subOrderId) {
        this.subOrderId = subOrderId;
        this.status = DeliveryStatus.PENDING;
    }

    /** 배송중 전이(PENDING→SHIPPING). 허용 전이만(FR-011). */
    public void ship(Instant now) {
        if (status != DeliveryStatus.PENDING) {
            throw DomainException.conflict("배송대기 상태에서만 배송중으로 전이할 수 있습니다");
        }
        this.status = DeliveryStatus.SHIPPING;
        this.shippedAt = now;
    }

    /** 배송완료 전이(SHIPPING→DELIVERED). 완료 시각 기록 → 향후 정산 트리거. */
    public void deliver(Instant now) {
        if (status != DeliveryStatus.SHIPPING) {
            throw DomainException.conflict("배송중 상태에서만 배송완료로 전이할 수 있습니다");
        }
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getSubOrderId() {
        return subOrderId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }
}
