package com.shopflow.frontend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/** 보호된 화면(장바구니·체크아웃·주문·판매자)에 비로그인 접근 시 /login으로 유도(PRG). */
public class AuthRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (FrontendSession.isLoggedIn(request)) {
            return true;
        }
        String next = request.getRequestURI();
        String loginUrl = UriComponentsBuilder.fromPath("/login").queryParam("next", next).build().toUriString();
        response.sendRedirect(loginUrl);
        return false;
    }
}
