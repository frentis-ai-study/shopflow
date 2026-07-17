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

/** 판매자 하위주문 목록 조회 통합(UC-11, FR-010): 본인 것만 노출. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerOrderQueryIT {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired SellerService sellerService;
    @Autowired ProductService productService;
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;

    @Test
    void 판매자는_본인에게_배정된_하위주문만_본다() throws Exception {
        long sellerUid = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자").getId();
        Seller seller = sellerService.register(sellerUid, SellerType.INDIVIDUAL, "store-" + UUID.randomUUID(),
                null, null, null, null);
        Product product = productService.register(seller.getId(), "상품-" + UUID.randomUUID(), null, 1000, 5, null);

        long otherUid = userService.signup("s2-" + UUID.randomUUID() + "@shop.com", "password123", "타판매자").getId();
        Seller otherSeller = sellerService.register(otherUid, SellerType.INDIVIDUAL, "other-" + UUID.randomUUID(),
                null, null, null, null);

        long buyerId = userService.signup("b-" + UUID.randomUUID() + "@shop.com", "password123", "구매자").getId();
        cartService.addItem(buyerId, product.getId(), 2);
        CheckoutResult result = checkoutService.checkout(buyerId, new Address("수령", "주소", "010"),
                UUID.randomUUID().toString());

        mvc.perform(get("/api/seller/orders").with(user(String.valueOf(sellerUid)).roles("SELLER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(result.orderId()))
                .andExpect(jsonPath("$[0].subtotalKrw").value(2000))
                .andExpect(jsonPath("$[0].deliveryStatus").value("PENDING"));

        mvc.perform(get("/api/seller/orders").with(user(String.valueOf(otherUid)).roles("SELLER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
