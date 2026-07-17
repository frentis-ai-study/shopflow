package com.shopflow.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 주문 라인 — 결제 시점 상품명·단가 스냅샷 보존(FR-023). */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false)
    private Long subOrderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "unit_price_krw_snapshot", nullable = false)
    private long unitPriceKrwSnapshot;

    @Column(nullable = false)
    private int quantity;

    protected OrderLine() {
    }

    public OrderLine(Long subOrderId, Long productId, String productNameSnapshot,
                     long unitPriceKrwSnapshot, int quantity) {
        this.subOrderId = subOrderId;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPriceKrwSnapshot = unitPriceKrwSnapshot;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getSubOrderId() {
        return subOrderId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public long getUnitPriceKrwSnapshot() {
        return unitPriceKrwSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public long lineTotal() {
        return unitPriceKrwSnapshot * quantity;
    }
}
