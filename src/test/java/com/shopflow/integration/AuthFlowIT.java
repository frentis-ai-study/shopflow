package com.shopflow.integration;

import com.shopflow.support.PostgresContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 가입→인증 흐름 통합 테스트(US3, FR-001~005). */
@AutoConfigureMockMvc
class AuthFlowIT extends PostgresContainerBase {

    @Autowired
    MockMvc mvc;

    @Test
    void 가입은_201_중복은_409() throws Exception {
        String body = """
                {"email":"buyer@shop.com","password":"password123","displayName":"구매자"}
                """;
        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void 잘못된_이메일은_400() throws Exception {
        String body = """
                {"email":"bad","password":"password123","displayName":"x"}
                """;
        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비로그인_판매자등록은_거부() throws Exception {
        String body = """
                {"sellerType":"INDIVIDUAL","storeName":"내 상점"}
                """;
        // 인증 없이 보호 엔드포인트 접근 → 401/403
        mvc.perform(post("/api/seller").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());
    }
}
