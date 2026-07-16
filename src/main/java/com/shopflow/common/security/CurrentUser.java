package com.shopflow.common.security;

import com.shopflow.common.error.DomainException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 인증된 사용자 id 헬퍼. 인증 주체(principal)의 username에 사용자 id를 담는다
 * (AccountUserDetailsService 참고). 소유권 판정(FR-009)·본인 자원 조회에 사용.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** 로그인한 사용자 id. 미인증이면 403. */
    public static Long requireId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw DomainException.forbidden("로그인이 필요합니다");
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            throw DomainException.forbidden("로그인이 필요합니다");
        }
    }
}
