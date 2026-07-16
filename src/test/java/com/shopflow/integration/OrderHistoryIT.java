package com.shopflow.integration;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.account.domain.UserService;
import com.shopflow.common.domain.Address;
import com.shopflow.order.domain.CartService;
import com.shopflow.order.domain.CheckoutService;
import com.shopflow.order.domain.CheckoutService.CheckoutResult;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 구매자 주문 조회 통합(US5): 다중 판매자 주문 상세, 본인만 조회(SC-006/007). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderHistoryIT {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired SellerService sellerService;
    @Autowired ProductService productService;
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;

    private Product createProduct(long price, int stock) {
        long uid = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자").getId();
        Seller s = sellerService.register(uid, SellerType.INDIVIDUAL, "store-" + UUID.randomUUID(),
                null, null, null, null);
        return productService.register(s.getId(), "상품-" + UUID.randomUUID(), null, price, stock, null);
    }

    @Test
    void 다중판매자_주문_상세와_본인만_조회() throws Exception {
        Product p1 = createProduct(1000, 5);
        Product p2 = createProduct(2000, 5);
        long buyerId = userService.signup("b-" + UUID.randomUUID() + "@shop.com", "password123", "구매자").getId();
        cartService.addItem(buyerId, p1.getId(), 1);
        cartService.addItem(buyerId, p2.getId(), 1);
        CheckoutResult result = checkoutService.checkout(buyerId, new Address("수령", "주소", "010"),
                UUID.randomUUID().toString());

        // 내 주문 목록
        mvc.perform(get("/api/orders").with(user(String.valueOf(buyerId)).roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].totalKrw").value(3000));

        // 상세: 판매자별 하위주문 2개 + 배송 상태
        mvc.perform(get("/api/orders/" + result.orderId()).with(user(String.valueOf(buyerId)).roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subOrders.length()").value(2))
                .andExpect(jsonPath("$.subOrders[0].deliveryStatus").value("PENDING"));

        // 타인은 조회 불가
        long otherId = userService.signup("o-" + UUID.randomUUID() + "@shop.com", "password123", "타인").getId();
        mvc.perform(get("/api/orders/" + result.orderId()).with(user(String.valueOf(otherId)).roles("BUYER")))
                .andExpect(status().isForbidden());
    }
}
