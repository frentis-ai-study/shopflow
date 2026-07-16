package com.shopflow.frontend.client;

import java.io.Serializable;

/**
 * 백엔드 세션 릴레이(BFF 쿠키 릴레이) 상태. 프론트가 사용자 대신 백엔드에 로그인해 받은
 * {@code JSESSIONID}·{@code XSRF-TOKEN} 쿠키 값을 프론트 자신의 HttpSession에 보관하고,
 * 이후 백엔드 호출마다 그대로 실어 보낸다. 프론트↔백엔드 인증 전파 방식(ADR-0011의 미결
 * 사항)에 대한 구체 구현이다.
 */
public record BackendSession(String jsessionId, String xsrfToken, String email, String displayName)
        implements Serializable {

    public BackendSession withXsrfToken(String newToken) {
        return new BackendSession(jsessionId, newToken, email, displayName);
    }
}
