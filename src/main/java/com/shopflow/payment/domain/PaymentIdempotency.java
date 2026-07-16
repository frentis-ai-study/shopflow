package com.shopflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 멱등 레코드. key 유니크로 "정확히 한 번"을 저장소 수준에서 강제(FR-018, SC-004). */
@Entity
@Table(name = "payment_idempotency")
public class PaymentIdempotency {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentIdempotency() {
    }

    public PaymentIdempotency(String key, String requestHash, Instant createdAt) {
        this.key = key;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.STARTED;
        this.createdAt = createdAt;
    }

    public void markDone(Long orderId) {
        this.status = IdempotencyStatus.DONE;
        this.orderId = orderId;
    }

    public void markFailed() {
        this.status = IdempotencyStatus.FAILED;
    }

    public String getKey() {
        return key;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Long getOrderId() {
        return orderId;
    }
}
