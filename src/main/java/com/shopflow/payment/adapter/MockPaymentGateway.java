package com.shopflow.payment.adapter;

import com.shopflow.payment.domain.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 모의 결제 게이트웨이(ADR-0002). 결정적 규칙으로 승인/거절을 시뮬레이션한다.
 *
 * <p><b>규칙</b>: 금액의 끝자리가 7이면(amountKrw % 10 == 7) 거절, 그 외 승인.
 * 테스트는 상품 가격을 조정해 승인/거절 경로를 결정적으로 재현한다(검토 보고서 #8).
 * {@link #beforeAuthorize}에 동작을 주입해 동시성 테스트의 블로킹을 재현할 수 있다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    /** 테스트용 훅(기본 no-op). 동시성 테스트에서 latch 등을 주입. */
    public volatile Runnable beforeAuthorize = () -> {
    };

    @Override
    public PaymentResult authorize(PaymentRequest request) {
        beforeAuthorize.run();
        if (request.amountKrw() % 10 == 7) {
            return PaymentResult.rejected("결제가 거절되었습니다(모의 규칙: 금액 끝자리 7)");
        }
        return PaymentResult.approved("MOCK-" + UUID.randomUUID());
    }
}
