package com.shopflow.account.domain;

/** 판매자 유형(ADR-0009). */
public enum SellerType {
    INDIVIDUAL,       // 개인
    SOLE_PROPRIETOR,  // 개인사업자
    CORPORATION;      // 법인

    /** 사업자등록번호·대표자가 필수인 유형인가. */
    public boolean requiresBusinessInfo() {
        return this != INDIVIDUAL;
    }
}
