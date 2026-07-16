package com.shopflow.frontend.client.dto;

/** 백엔드 ProductController.ProductView 응답 미러. */
public record ProductDto(Long id, String name, String description, long priceKrw,
                         boolean inStock, String imageUrl, String status, Long sellerId) {
}
