package com.shopflow.delivery.rest;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.delivery.domain.Delivery;
import com.shopflow.delivery.domain.DeliveryService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 판매자 배송 전이 REST(UC-12). 소유 판매자만(FR-009). */
@RestController
public class DeliveryOpsController {

    private final DeliveryService deliveryService;
    private final SellerService sellerService;

    public DeliveryOpsController(DeliveryService deliveryService, SellerService sellerService) {
        this.deliveryService = deliveryService;
        this.sellerService = sellerService;
    }

    public record DeliveryView(Long subOrderId, String status) {
        static DeliveryView of(Delivery d) {
            return new DeliveryView(d.getSubOrderId(), d.getStatus().name());
        }
    }

    @PostMapping("/api/seller/orders/{subOrderId}/ship")
    public DeliveryView ship(@PathVariable Long subOrderId) {
        return DeliveryView.of(deliveryService.ship(subOrderId, currentSellerId()));
    }

    @PostMapping("/api/seller/orders/{subOrderId}/deliver")
    public DeliveryView deliver(@PathVariable Long subOrderId) {
        return DeliveryView.of(deliveryService.deliver(subOrderId, currentSellerId()));
    }

    private Long currentSellerId() {
        Seller seller = sellerService.requireActiveSeller(CurrentUser.requireId());
        return seller.getId();
    }
}
