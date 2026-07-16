package com.shopflow.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 베이스 — PostgreSQL Testcontainer를 띄우고 datasource를 동적 바인딩한다.
 * 동시성·TTL·멱등 등 DB 의존 로직을 결정적으로 검증한다(원칙 I, 검토 보고서 #8).
 */
@SpringBootTest
@Testcontainers
public abstract class PostgresContainerBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("shopflow")
                    .withUsername("shopflow")
                    .withPassword("shopflow");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
