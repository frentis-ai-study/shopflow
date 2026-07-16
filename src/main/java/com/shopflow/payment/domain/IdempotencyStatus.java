package com.shopflow.payment.domain;

/** 멱등 처리 상태(검토 보고서 #3). STARTED(처리중) → DONE(완료) / FAILED(실패). */
public enum IdempotencyStatus {
    STARTED,
    DONE,
    FAILED
}
