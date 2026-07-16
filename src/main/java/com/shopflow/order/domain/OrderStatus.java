package com.shopflow.order.domain;

/** 주문 전체 상태 — 하위주문 배송(Delivery) 상태들의 집계. */
public enum OrderStatus {
    PAID,
    PARTIALLY_SHIPPED,
    SHIPPED,
    COMPLETED
}
