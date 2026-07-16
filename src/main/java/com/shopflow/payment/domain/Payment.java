package com.shopflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 결제 시도·결과 기록. 실패 결제도 남긴다(order_id nullable, 검토 보고서 #5). */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "amount_krw", nullable = false)
    private long amountKrw;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "gateway_ref")
    private String gatewayRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(Long orderId, String idempotencyKey, long amountKrw,
                   PaymentStatus status, String gatewayRef, Instant createdAt) {
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.amountKrw = amountKrw;
        this.status = status;
        this.gatewayRef = gatewayRef;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
