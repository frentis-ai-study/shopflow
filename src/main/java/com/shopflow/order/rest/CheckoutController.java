package com.shopflow.order.rest;

import com.shopflow.common.domain.Address;
import com.shopflow.common.error.DomainException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.order.domain.CheckoutService;
import com.shopflow.order.domain.CheckoutService.CheckoutResult;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 체크아웃 REST(UC-09). 멱등키는 서버가 발급(R3). */
@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    public record CheckoutIntent(String idempotencyKey) {
    }

    public record CheckoutRequest(
            @NotBlank String recipient,
            @NotBlank String address,
            @NotBlank String phone,
            @NotBlank String idempotencyKey) {
    }

    public record CheckoutResponse(String result, Long orderId, long totalKrw, String message) {
        static CheckoutResponse of(CheckoutResult r) {
            return new CheckoutResponse(r.result().name(), r.orderId(), r.totalKrw(), r.message());
        }
    }

    /** 체크아웃 진입 — 서버가 멱등키를 발급한다. */
    @GetMapping("/api/checkout")
    public CheckoutIntent intent() {
        CurrentUser.requireId();
        return new CheckoutIntent(UUID.randomUUID().toString());
    }

    /** 결제 처리. 결과에 따라 201(생성)/200(기존)/402(거절). */
    @PostMapping("/api/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest req) {
        Long buyerId = CurrentUser.requireId();
        if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
            throw DomainException.badRequest("멱등키가 필요합니다");
        }
        Address address = new Address(req.recipient(), req.address(), req.phone());
        CheckoutResult result = checkoutService.checkout(buyerId, address, req.idempotencyKey());

        HttpStatus status = switch (result.result()) {
            case CREATED -> HttpStatus.CREATED;
            case EXISTING -> HttpStatus.OK;
            case REJECTED -> HttpStatus.PAYMENT_REQUIRED;
        };
        return ResponseEntity.status(status).body(CheckoutResponse.of(result));
    }
}
