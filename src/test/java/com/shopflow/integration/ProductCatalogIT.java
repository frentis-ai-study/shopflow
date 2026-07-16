package com.shopflow.integration;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.account.domain.User;
import com.shopflow.account.domain.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 상품 등록·조회·판매상태·소유권 통합 테스트(US2, 실 DB). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCatalogIT {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired SellerService sellerService;

    private long newSeller(String store) {
        User u = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자");
        Seller s = sellerService.register(u.getId(), SellerType.INDIVIDUAL, store, null, null, null, null);
        return u.getId();
    }

    @Test
    void 등록한_상품이_목록에_노출되고_판매중지시_사라진다() throws Exception {
        long sellerUserId = newSeller("스토어-" + UUID.randomUUID());
        String uniqueName = "특별사과-" + UUID.randomUUID();

        String created = mvc.perform(post("/api/seller/products")
                        .with(user(String.valueOf(sellerUserId)).roles("SELLER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"신선","priceKrw":1500,"stock":5}
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = com.jayway.jsonpath.JsonPath.parse(created).read("$.id", Long.class);

        // 목록 검색에 노출
        mvc.perform(get("/api/products").param("q", uniqueName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(uniqueName));

        // 판매중지 → 목록에서 사라짐
        mvc.perform(post("/api/seller/products/" + productId + "/status")
                        .with(user(String.valueOf(sellerUserId)).roles("SELLER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF_SALE\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/products").param("q", uniqueName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 타_판매자_상품_수정은_403() throws Exception {
        long ownerUserId = newSeller("owner-" + UUID.randomUUID());
        long otherUserId = newSeller("other-" + UUID.randomUUID());

        String created = mvc.perform(post("/api/seller/products")
                        .with(user(String.valueOf(ownerUserId)).roles("SELLER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"내상품\",\"priceKrw\":1000,\"stock\":3}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = com.jayway.jsonpath.JsonPath.parse(created).read("$.id", Long.class);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/seller/products/" + productId)
                        .with(user(String.valueOf(otherUserId)).roles("SELLER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"탈취\",\"priceKrw\":1,\"imageUrl\":null}"))
                .andExpect(status().isForbidden());
    }
}
