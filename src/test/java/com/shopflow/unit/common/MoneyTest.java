package com.shopflow.unit.common;

import com.shopflow.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Money 값객체 단위 테스트 (원칙 I). */
class MoneyTest {

    @Test
    void 음수_금액은_거부된다() {
        assertThatThrownBy(() -> new Money(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 덧셈은_금액을_더한다() {
        assertThat(Money.won(1000).plus(Money.won(500)))
                .isEqualTo(Money.won(1500));
    }

    @Test
    void 곱셈은_단가에_수량을_곱한다() {
        assertThat(Money.won(1000).times(3)).isEqualTo(Money.won(3000));
    }

    @Test
    void 수량0_곱셈은_0원() {
        assertThat(Money.won(1000).times(0)).isEqualTo(Money.ZERO);
    }

    @Test
    void 곱셈_수량이_음수면_거부된다() {
        assertThatThrownBy(() -> Money.won(1000).times(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 덧셈_오버플로는_예외() {
        assertThatThrownBy(() -> new Money(Long.MAX_VALUE).plus(Money.won(1)))
                .isInstanceOf(ArithmeticException.class);
    }
}
