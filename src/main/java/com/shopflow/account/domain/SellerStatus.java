package com.shopflow.account.domain;

/** 판매자 입점 상태. SUSPENDED면 신규 상품 등록·판매 제한. */
public enum SellerStatus {
    ACTIVE,
    SUSPENDED
}
