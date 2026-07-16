package com.shopflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ShopFlow 애플리케이션 진입점.
 *
 * <p>백엔드는 단일 배포 모듈러 모놀리스(ADR-0010 v2). 컨텍스트별 패키지
 * (account/product/order/payment/inventory/delivery/common)로 구성한다.
 * 재고 선점 만료 스윕을 위해 스케줄링을 활성화한다.
 */
@SpringBootApplication
@EnableScheduling
public class ShopFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopFlowApplication.class, args);
    }
}
