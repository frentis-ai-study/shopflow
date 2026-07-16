package com.shopflow.frontend.client.dto;

/** 백엔드 CheckoutController 응답 미러. */
public class CheckoutDtos {

    public record Intent(String idempotencyKey) {
    }

    public record Result(String result, Long orderId, long totalKrw, String message) {
    }

    private CheckoutDtos() {
    }
}
