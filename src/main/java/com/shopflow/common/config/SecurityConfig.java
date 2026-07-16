package com.shopflow.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * 보안 설정(원칙 II). 비밀번호는 BCrypt로 해시한다.
 *
 * <p>세션 기반 인증을 쓰는 동안 CSRF는 쿠키 기반 토큰({@code XSRF-TOKEN} 쿠키 +
 * {@code X-XSRF-TOKEN} 헤더)으로 강제한다. 프론트(별도 인스턴스, ADR-0011)가 쿠키에서 토큰을
 * 읽어 헤더로 되돌려 보내는 표준 패턴이며, 세션 상태변경 엔드포인트(체크아웃·장바구니 등)를
 * 보호한다.
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
                // 세션 기반 폼 로그인 (프론트 제어 레이어가 BFF로 소비, ADR-0011).
                // 리다이렉트 대신 상태코드만 반환 — 프론트가 백엔드 세션 쿠키를 릴레이하는
                // 서버 간(server-to-server) 호출 패턴에 적합하다.
                .formLogin(form -> form.loginProcessingUrl("/login").permitAll()
                        .successHandler((req, res, auth) -> res.setStatus(org.springframework.http.HttpStatus.OK.value()))
                        .failureHandler((req, res, ex) -> res.setStatus(org.springframework.http.HttpStatus.UNAUTHORIZED.value())))
                .logout(logout -> logout.logoutUrl("/logout").permitAll()
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(org.springframework.http.HttpStatus.OK.value())))
                // REST 백엔드: 미인증 접근은 로그인 페이지 리다이렉트(302) 대신 401을 반환한다.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                org.springframework.http.HttpStatus.UNAUTHORIZED)))
                // 쿠키 기반 CSRF(HttpOnly=false로 프론트 JS가 읽어 X-XSRF-TOKEN 헤더로 재전송).
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));

        return http.build();
    }
}
