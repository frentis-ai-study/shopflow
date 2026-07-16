package com.shopflow.account.rest;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.common.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 판매자 입점(프로필 등록) REST. 로그인한 사용자가 판매자 역할을 취득한다(ADR-0009). */
@RestController
@RequestMapping("/api/seller")
public class SellerRegistrationController {

    private final SellerService sellerService;

    public SellerRegistrationController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    public record RegisterRequest(
            @NotNull SellerType sellerType,
            @NotBlank String storeName,
            String businessRegistrationNumber,
            String representativeName,
            String contactPhone,
            String contactEmail) {
    }

    public record SellerResponse(Long sellerId, String storeName, String status) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SellerResponse register(@RequestBody RegisterRequest req) {
        Long userId = CurrentUser.requireId();
        Seller seller = sellerService.register(userId, req.sellerType(), req.storeName(),
                req.businessRegistrationNumber(), req.representativeName(),
                req.contactPhone(), req.contactEmail());
        return new SellerResponse(seller.getId(), seller.getStoreName(), seller.getStatus().name());
    }
}
