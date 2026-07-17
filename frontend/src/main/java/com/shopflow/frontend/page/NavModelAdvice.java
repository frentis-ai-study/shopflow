package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
import com.shopflow.frontend.client.BackendSession;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.security.FrontendSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 모든 화면 공통 — 로그인 상태·장바구니 개수를 nav 프래그먼트에 제공한다. */
@ControllerAdvice
public class NavModelAdvice {

    private final RestApiClient api;

    public NavModelAdvice(RestApiClient api) {
        this.api = api;
    }

    @ModelAttribute("loggedIn")
    public boolean loggedIn(HttpServletRequest request) {
        return FrontendSession.isLoggedIn(request);
    }

    @ModelAttribute("userDisplayName")
    public String userDisplayName(HttpServletRequest request) {
        BackendSession session = FrontendSession.get(request);
        return session == null ? null : session.displayName();
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpServletRequest request) {
        BackendSession session = FrontendSession.get(request);
        if (session == null) {
            return 0;
        }
        try {
            return api.cartItems(session).stream().mapToInt(i -> i.quantity()).sum();
        } catch (ApiException e) {
            return 0;
        }
    }
}
