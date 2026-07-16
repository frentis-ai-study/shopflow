package com.shopflow.inventory.domain;

/** 재고 선점 상태. HELD → CONFIRMED(확정 차감) / RELEASED(해제·만료). */
public enum ReservationStatus {
    HELD,
    CONFIRMED,
    RELEASED
}
