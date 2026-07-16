package com.shopflow.common.domain;

import java.util.Objects;

/**
 * 금액 값객체 — 정수 원(KRW). 이진 부동소수점을 쓰지 않는다(ADR-0006).
 *
 * <p>덧셈·수량 곱셈만 허용하고, 음수·오버플로를 방어한다.
 */
public record Money(long amountKrw) {

    public static final Money ZERO = new Money(0L);

    public Money {
        if (amountKrw < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다: " + amountKrw);
        }
    }

    public static Money won(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "더할 금액이 null입니다");
        return new Money(Math.addExact(this.amountKrw, other.amountKrw));
    }

    /** 단가 × 수량. 수량은 0 이상. */
    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 음수일 수 없습니다: " + quantity);
        }
        return new Money(Math.multiplyExact(this.amountKrw, quantity));
    }

    @Override
    public String toString() {
        return amountKrw + "원";
    }
}
