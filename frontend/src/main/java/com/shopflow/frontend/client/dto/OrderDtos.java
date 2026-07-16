package com.shopflow.frontend.client.dto;

import java.util.List;

/** 백엔드 OrderQueryController 응답 미러. */
public class OrderDtos {

    public record Summary(Long id, long totalKrw, String status, String placedAt) {
    }

    public record SubOrderView(Long subOrderId, String sellerStoreName, long subtotalKrw, String deliveryStatus) {
    }

    public record Detail(Long id, long totalKrw, String status, List<SubOrderView> subOrders) {
    }

    private OrderDtos() {
    }
}
