package com.shopflow.frontend.client.dto;

import java.util.List;

/** 백엔드 SellerRegistrationController / SellerOrderQueryController 응답 미러. */
public class SellerDtos {

    public record SellerResponse(Long sellerId, String storeName, String status) {
    }

    public record LineView(String productName, long unitPriceKrw, int quantity) {
    }

    public record SellerSubOrderView(Long subOrderId, Long orderId, long subtotalKrw,
                                     String deliveryStatus, List<LineView> lines) {
    }

    public record DeliveryView(Long subOrderId, String status) {
    }

    private SellerDtos() {
    }
}
