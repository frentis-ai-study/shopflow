package com.shopflow.integration;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.account.domain.UserService;
import com.shopflow.inventory.domain.ReservationService;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 미결제 선점 TTL 만료 스윕 시 재고 100% 복원 — SC-005. Clock을 조작해 결정적으로 재현. */
@SpringBootTest
@ActiveProfiles("test")
class ReservationTtlSweepIT {

    @MockBean Clock clock;

    @Autowired ReservationService reservationService;
    @Autowired ProductService productService;
    @Autowired UserService userService;
    @Autowired SellerService sellerService;

    @Test
    void 만료선점은_스윕시_재고복원() {
        Instant t0 = Instant.parse("2026-07-16T00:00:00Z");
        when(clock.instant()).thenReturn(t0);

        long sellerUid = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자").getId();
        Seller seller = sellerService.register(sellerUid, SellerType.INDIVIDUAL, "store-" + UUID.randomUUID(),
                null, null, null, null);
        Product product = productService.register(seller.getId(), "상품-" + UUID.randomUUID(), null, 1000, 2, null);
        long buyerId = userService.signup("b-" + UUID.randomUUID() + "@shop.com", "password123", "구매자").getId();

        // 선점(HELD, 만료 = t0 + 10분)
        reservationService.reserve(product.getId(), buyerId, 1);
        assertThat(productService.get(product.getId()).getReserved()).isEqualTo(1);

        // 11분 경과 후 스윕 → 만료 해제, 재고 복원
        when(clock.instant()).thenReturn(t0.plus(Duration.ofMinutes(11)));
        int released = reservationService.sweepExpired();

        assertThat(released).isEqualTo(1);
        assertThat(productService.get(product.getId()).getReserved()).isZero();
        assertThat(productService.get(product.getId()).availableStock()).isEqualTo(2);
    }
}
