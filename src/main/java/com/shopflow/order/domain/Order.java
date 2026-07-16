package com.shopflow.order.domain;

import com.shopflow.common.domain.Address;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 주문 — 한 번의 결제로 생성된 구매 단위. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "total_krw", nullable = false)
    private long totalKrw;

    @Embedded
    private Address shippingAddress;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    protected Order() {
    }

    public Order(Long buyerId, long totalKrw, Address shippingAddress, Instant placedAt) {
        this.buyerId = buyerId;
        this.totalKrw = totalKrw;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PAID;
        this.placedAt = placedAt;
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public long getTotalKrw() {
        return totalKrw;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
