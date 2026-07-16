package com.shopflow.integration;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.account.domain.UserService;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 체크아웃 통합 테스트(US1): 해피패스·서버 금액검증·멱등·거절·빈 장바구니. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutIT {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired SellerService sellerService;
    @Autowired ProductService productService;

    private long createBuyer() {
        return userService.signup("b-" + UUID.randomUUID() + "@shop.com", "password123", "구매자").getId();
    }

    private long createProduct(long priceKrw, int stock) {
        long uid = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자").getId();
        Seller s = sellerService.register(uid, SellerType.INDIVIDUAL, "store-" + UUID.randomUUID(),
                null, null, null, null);
        Product p = productService.register(s.getId(), "상품-" + UUID.randomUUID(), null, priceKrw, stock, null);
        return p.getId();
    }

    private void addToCart(long buyerId, long productId, int qty) throws Exception {
        mvc.perform(post("/api/cart/items").with(user(String.valueOf(buyerId)).roles("BUYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + qty + "}"))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions checkout(long buyerId, String key) throws Exception {
        return mvc.perform(post("/api/checkout").with(user(String.valueOf(buyerId)).roles("BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"수령인\",\"address\":\"서울\",\"phone\":\"010\",\"idempotencyKey\":\"" + key + "\"}"));
    }

    @Test
    void 해피패스_주문생성_재고차감_서버금액() throws Exception {
        long productId = createProduct(1000, 3);
        long buyerId = createBuyer();
        addToCart(buyerId, productId, 2);

        checkout(buyerId, UUID.randomUUID().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("CREATED"))
                .andExpect(jsonPath("$.totalKrw").value(2000)); // 서버 계산(1000*2)

        // 재고 3 → 1 (확정 차감)
        assertThat(productService.get(productId).getStock()).isEqualTo(1);
        assertThat(productService.get(productId).getReserved()).isEqualTo(0);
    }

    @Test
    void 멱등_재시도는_이중결제_없이_기존주문_반환() throws Exception {
        long productId = createProduct(1000, 5);
        long buyerId = createBuyer();
        addToCart(buyerId, productId, 1);
        String key = UUID.randomUUID().toString();

        String first = checkout(buyerId, key).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = JsonPath.parse(first).read("$.orderId", Long.class);

        // 같은 키 재요청 → 200 EXISTING, 같은 주문
        checkout(buyerId, key)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("EXISTING"))
                .andExpect(jsonPath("$.orderId").value(orderId));

        // 재고는 한 번만 차감(5→4)
        assertThat(productService.get(productId).getStock()).isEqualTo(4);
    }

    @Test
    void 결제거절시_402_재고복원_주문없음() throws Exception {
        long productId = createProduct(1007, 3); // 끝자리 7 → 모의 거절
        long buyerId = createBuyer();
        addToCart(buyerId, productId, 1);

        checkout(buyerId, UUID.randomUUID().toString())
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.result").value("REJECTED"));

        // 재고 원복(3), 선점 0
        assertThat(productService.get(productId).getStock()).isEqualTo(3);
        assertThat(productService.get(productId).getReserved()).isEqualTo(0);
    }

    @Test
    void 빈_장바구니_결제는_400() throws Exception {
        long buyerId = createBuyer();
        checkout(buyerId, UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());
    }
}
