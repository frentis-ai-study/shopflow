package com.shopflow.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 보안 설정 골격(원칙 II). 비밀번호는 BCrypt로 해시한다.
 *
 * <p>US3(T023)에서 로그인 처리·인가 규칙을 완성한다. 이 단계에서는 공개 리소스와
 * 비밀번호 인코더만 정의하고, 나머지는 기본 보호(인증 필요)로 둔다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 공개: 가입·로그인·상품 조회
                        .requestMatchers("/api/signup", "/login", "/error").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/products/**").permitAll()
                        // 그 외는 인증 필요 (세부 인가는 US3에서 강화)
                        .anyRequest().authenticated()
                )
                // 세션 기반 폼 로그인 (프론트 제어 레이어가 소비, ADR-0011). 상세는 T023.
                .formLogin(form -> form.loginProcessingUrl("/login").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").permitAll())
                // REST API는 상태 변경에 CSRF 토큰 필요. 개발 편의상 API 경로 CSRF는 T023에서 정책 확정.
                .csrf(AbstractHttpConfigurer::disable); // TODO(T023): CSRF 정책 확정(세션 기반 재활성)

        return http.build();
    }
}
