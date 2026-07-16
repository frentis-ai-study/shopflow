package com.shopflow.integration;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.account.domain.UserService;
import com.shopflow.common.domain.Address;
import com.shopflow.order.domain.CartService;
import com.shopflow.order.domain.CheckoutService;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 동시 결제 초과판매 0 — SC-003. 재고 1을 두 구매자가 동시에 결제. */
@SpringBootTest
@ActiveProfiles("test")
class OversellConcurrencyIT {

    @Autowired UserService userService;
    @Autowired SellerService sellerService;
    @Autowired ProductService productService;
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;

    @Test
    void 재고1_동시결제_한명만_성공() throws Exception {
        // 재고 1 상품
        long sellerUid = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자").getId();
        Seller seller = sellerService.register(sellerUid, SellerType.INDIVIDUAL, "store-" + UUID.randomUUID(),
                null, null, null, null);
        Product product = productService.register(seller.getId(), "한정판-" + UUID.randomUUID(), null, 1000, 1, null);

        long buyer1 = userService.signup("b1-" + UUID.randomUUID() + "@shop.com", "password123", "구매1").getId();
        long buyer2 = userService.signup("b2-" + UUID.randomUUID() + "@shop.com", "password123", "구매2").getId();
        cartService.addItem(buyer1, product.getId(), 1);
        cartService.addItem(buyer2, product.getId(), 1);

        Address addr = new Address("수령", "주소", "010");
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Runnable r1 = () -> attempt(buyer1, addr, start, success, conflict);
        Runnable r2 = () -> attempt(buyer2, addr, start, success, conflict);
        pool.submit(r1);
        pool.submit(r2);
        start.countDown(); // 동시 시작
        pool.shutdown();
        //noinspection ResultOfMethodCallIgnored
        pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        // 정확히 한 명만 성공, 초과판매 0
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        Product after = productService.get(product.getId());
        assertThat(after.getStock()).isEqualTo(0);
        assertThat(after.getReserved()).isEqualTo(0);
        assertThat(after.availableStock()).isGreaterThanOrEqualTo(0); // 음수 절대 없음
    }

    private void attempt(long buyerId, Address addr, CountDownLatch start,
                         AtomicInteger success, AtomicInteger conflict) {
        try {
            start.await();
            checkoutService.checkout(buyerId, addr, UUID.randomUUID().toString());
            success.incrementAndGet();
        } catch (Exception e) {
            conflict.incrementAndGet();
        }
    }
}
