package com.shopflow.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 가입→인증 흐름 통합 테스트(US3, FR-001~005).
 *
 * <p>실행 중인 PostgreSQL(compose, dev-ports)에 연결해 Flyway 마이그레이션·JPA 스키마 검증·
 * Spring 컨텍스트·실제 가입 흐름을 검증한다. 재실행 가능하도록 이메일을 매 실행 유니크로 만든다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIT {

    @Autowired
    MockMvc mvc;

    private String uniqueEmail() {
        return "buyer-" + UUID.randomUUID() + "@shop.com";
    }

    @Test
    void 가입은_201_중복은_409() throws Exception {
        String email = uniqueEmail();
        String body = """
                {"email":"%s","password":"password123","displayName":"구매자"}
                """.formatted(email);

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
        mvc.perform(post("/api/seller").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());
    }
}
