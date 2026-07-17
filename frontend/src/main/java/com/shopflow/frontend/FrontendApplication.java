package com.shopflow.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShopFlow 프론트엔드(BFF) 애플리케이션 진입점.
 *
 * <p>백엔드와 별도 인스턴스로 배포한다(ADR-0010 v2). 화면 렌더·폼·세션·플로우를 담당하고,
 * 도메인 데이터·규칙은 백엔드 REST API({@link com.shopflow.frontend.client.RestApiClient})를
 * 통해서만 다룬다(ADR-0011). 이 인스턴스 자체는 도메인 로직을 갖지 않는다.
 */
@SpringBootApplication
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
