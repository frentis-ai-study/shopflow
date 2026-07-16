package com.shopflow.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 CSRF 토큰은 BREACH 보호를 위해 지연 해석(deferred)된다 — 아무도 읽지
 * 않으면 {@code XSRF-TOKEN} 쿠키가 응답에 실리지 않는다. REST 백엔드는 서버 렌더 폼이 없어
 * 이 지연 해석을 트리거할 계기가 없으므로, 매 요청마다 토큰을 즉시 읽어 쿠키가 항상 나가도록
 * 강제한다(Spring Security 공식 권고 패턴 — SPA/BFF 클라이언트 대응).
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // 강제 즉시 해석 → 응답에 쿠키 설정
        }
        filterChain.doFilter(request, response);
    }
}
