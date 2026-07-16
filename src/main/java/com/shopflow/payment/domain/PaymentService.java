package com.shopflow.payment.domain;

import com.shopflow.payment.domain.PaymentGateway.PaymentRequest;
import com.shopflow.payment.domain.PaymentGateway.PaymentResult;
import com.shopflow.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** 결제 처리 — 게이트웨이 호출 + 결제 기록(성공/실패 모두, 검토 보고서 #5). */
@Service
public class PaymentService {

    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final Clock clock;

    public PaymentService(PaymentGateway gateway, PaymentRepository payments, Clock clock) {
        this.gateway = gateway;
        this.payments = payments;
        this.clock = clock;
    }

    /** 게이트웨이 승인 요청(멱등키 전달). */
    public PaymentResult authorize(String idempotencyKey, long amountKrw, String orderRef) {
        return gateway.authorize(new PaymentRequest(idempotencyKey, amountKrw, orderRef));
    }

    /** 결제 결과를 기록. 실패 결제도 order_id=null로 남긴다. */
    @Transactional
    public Payment record(Long orderId, String idempotencyKey, long amountKrw, PaymentResult result) {
        Payment payment = new Payment(orderId, idempotencyKey, amountKrw,
                result.status(), result.gatewayRef(), Instant.now(clock));
        return payments.save(payment);
    }
}
