package com.shopflow.frontend.client;

/** 백엔드 REST 호출 실패(4xx/5xx)를 감싸는 예외. 화면에 그대로 안내 메시지로 노출한다. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
