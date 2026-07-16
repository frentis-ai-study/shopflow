package com.shopflow.unit.payment;

import com.shopflow.payment.adapter.MockPaymentGateway;
import com.shopflow.payment.domain.PaymentGateway.PaymentRequest;
import com.shopflow.payment.domain.PaymentGateway.PaymentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 모의 결제 게이트웨이 규칙 — 금액 끝자리 7이면 거절(결정적). */
class MockGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void 일반_금액은_승인() {
        PaymentResult r = gateway.authorize(new PaymentRequest("k1", 1500, "ref"));
        assertThat(r.isApproved()).isTrue();
        assertThat(r.gatewayRef()).isNotBlank();
    }

    @Test
    void 끝자리_7은_거절() {
        PaymentResult r = gateway.authorize(new PaymentRequest("k2", 1007, "ref"));
        assertThat(r.isApproved()).isFalse();
        assertThat(r.reason()).isNotBlank();
    }

    @Test
    void beforeAuthorize_훅이_호출된다() {
        boolean[] called = {false};
        gateway.beforeAuthorize = () -> called[0] = true;
        gateway.authorize(new PaymentRequest("k3", 1000, "ref"));
        assertThat(called[0]).isTrue();
    }
}
