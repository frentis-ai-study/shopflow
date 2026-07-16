package com.shopflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Spring Data REST 노출 정책(ADR-0011, 검토 보고서 #3/#6).
 *
 * <p>핵심 원칙: 자동 노출을 최소화한다. 리포지토리는 {@code @RepositoryRestResource}로 명시
 * 노출한 것만 REST 리소스가 되며(application.yml detection-strategy=annotated), 무결성이 걸린
 * 애그리거트(products/orders/payments)의 <b>쓰기는 커스텀 REST 컨트롤러로만</b> 수행한다.
 * 리포지토리 REST 노출은 조회 위주로 제한하고, 상세 노출 메서드는 각 리포지토리에서 통제한다.
 */
@Configuration
public class RestExposureConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        // id를 응답에 노출(프론트가 리소스 식별에 사용)
        config.setExposeRepositoryMethodsByDefault(false);
        // 기본 CORS는 프론트 인스턴스(별도 오리진) 대비. 실제 허용 오리진은 배포 설정에서 주입.
        // cors.addMapping("/api/**").allowedOriginPatterns("*").allowedMethods("*");
    }
}
