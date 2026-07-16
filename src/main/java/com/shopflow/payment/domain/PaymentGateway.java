package com.shopflow.payment.domain;

/**
 * 결제 어댑터 포트(ADR-0002). MVP는 MockPaymentGateway, 후속에 실 PSP 구현을 같은 계약으로 추가.
 * 카드 원본 정보는 계약에 포함하지 않는다(FR-024).
 */
public interface PaymentGateway {

    PaymentResult authorize(PaymentRequest request);

    record PaymentRequest(String idempotencyKey, long amountKrw, String orderRef) {
    }

    record PaymentResult(PaymentStatus status, String gatewayRef, String reason) {
        public static PaymentResult approved(String gatewayRef) {
            return new PaymentResult(PaymentStatus.APPROVED, gatewayRef, null);
        }

        public static PaymentResult rejected(String reason) {
            return new PaymentResult(PaymentStatus.REJECTED, null, reason);
        }

        public boolean isApproved() {
            return status == PaymentStatus.APPROVED;
        }
    }
}
