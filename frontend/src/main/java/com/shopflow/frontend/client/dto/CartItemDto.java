package com.shopflow.frontend.client.dto;

/** 백엔드 CartController.ItemView 응답 미러. */
public record CartItemDto(Long id, Long productId, int quantity) {
}
