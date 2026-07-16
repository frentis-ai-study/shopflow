package com.shopflow.common.error;

import org.springframework.http.HttpStatus;

/** 도메인 규칙 위반 예외의 기반. 한글 메시지와 HTTP 상태를 함께 담는다. */
public class DomainException extends RuntimeException {

    private final HttpStatus status;

    public DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    /** 잘못된 요청(검증 실패 등). */
    public static DomainException badRequest(String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, message);
    }

    /** 권한 없음(타 판매자 자원 접근 등, FR-009). */
    public static DomainException forbidden(String message) {
        return new DomainException(HttpStatus.FORBIDDEN, message);
    }

    /** 리소스 없음. */
    public static DomainException notFound(String message) {
        return new DomainException(HttpStatus.NOT_FOUND, message);
    }

    /** 충돌(재고 부족·상태 충돌 등). */
    public static DomainException conflict(String message) {
        return new DomainException(HttpStatus.CONFLICT, message);
    }
}
