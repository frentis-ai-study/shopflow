package com.shopflow.frontend.security;

import com.shopflow.frontend.client.BackendSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** 프론트 HttpSession에 {@link BackendSession}(백엔드 릴레이 상태)을 보관하는 헬퍼. */
public final class FrontendSession {

    private static final String ATTR = "backendSession";

    private FrontendSession() {
    }

    public static BackendSession get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (BackendSession) session.getAttribute(ATTR);
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return get(request) != null;
    }

    public static void put(HttpServletRequest request, BackendSession session) {
        request.getSession(true).setAttribute(ATTR, session);
    }

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(ATTR);
        }
    }
}
