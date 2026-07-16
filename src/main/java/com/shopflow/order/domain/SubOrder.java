package com.shopflow.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 하위주문 — 판매자별 정산·주문 단위(배송 상태는 Delivery가 담당, ADR-0008). */
@Entity
@Table(name = "sub_orders")
public class SubOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_store_name_snapshot", nullable = false)
    private String sellerStoreNameSnapshot;

    @Column(name = "subtotal_krw", nullable = false)
    private long subtotalKrw;

    protected SubOrder() {
    }

    public SubOrder(Long orderId, Long sellerId, String sellerStoreNameSnapshot, long subtotalKrw) {
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.sellerStoreNameSnapshot = sellerStoreNameSnapshot;
        this.subtotalKrw = subtotalKrw;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getSellerStoreNameSnapshot() {
        return sellerStoreNameSnapshot;
    }

    public long getSubtotalKrw() {
        return subtotalKrw;
    }
}
