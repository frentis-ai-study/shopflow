package com.shopflow.delivery.domain;

/** 배송 상태. PENDING(배송대기) → SHIPPING(배송중) → DELIVERED(배송완료). */
public enum DeliveryStatus {
    PENDING,
    SHIPPING,
    DELIVERED
}
