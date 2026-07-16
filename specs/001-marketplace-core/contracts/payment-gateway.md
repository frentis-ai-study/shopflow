# 계약: PaymentGateway 어댑터

**기능**: [spec.md](../spec.md) · **리서치**: [research.md](../research.md) R5 · **ADR**: [0002](../../../docs/adr/0002-payment-external-delegation-mock-gateway.md)

결제를 도메인에서 분리하는 어댑터 계약. MVP는 `MockPaymentGateway` 구현을 사용하고, 후속에
실 PSP 구현을 같은 인터페이스로 추가한다(도메인 변경 없음).

## 인터페이스 (개념)

```text
interface PaymentGateway {
  PaymentResult authorize(PaymentRequest request)
}
```

### PaymentRequest (입력)

| 필드 | 타입 | 설명 |
|---|---|---|
| idempotencyKey | String | 서버 발급 UUID. 게이트웨이도 이 키로 재시도 안전 처리 (R3) |
| amountKrw | long | 서버가 검증한 결제 금액(정수 원) (FR-019) |
| orderRef | String | 주문 참조(추적용) |

> 카드 원본 정보(PAN/CVV)는 계약에 포함하지 않는다(FR-024, 원칙 II).

### PaymentResult (출력)

| 필드 | 타입 | 설명 |
|---|---|---|
| status | enum | `APPROVED` / `REJECTED` |
| gatewayRef | String? | 게이트웨이 거래 참조(승인 시) |
| reason | String? | 거절 사유(거절 시, 한글 안내용) |

## 동작 계약

1. **멱등성**: 동일 `idempotencyKey`로 여러 번 호출되어도 결과는 정확히 한 번의 승인 효과.
   (도메인 측 멱등 저장과 이중 방어 — R3)
2. **결정성(모의)**: `MockPaymentGateway`는 규칙 기반으로 성공/실패를 결정한다. 예)
   - 금액이 규칙에 맞으면 `APPROVED` + 가짜 `gatewayRef` 반환.
   - 지정된 실패 트리거(예: 특정 금액/플래그)면 `REJECTED` + 사유.
   - 이 규칙으로 결제 실패 경로(UC-09 대안 흐름)를 결정적으로 테스트한다.
3. **부작용 없음(외부)**: 모의 구현은 실제 금전 이동을 하지 않는다.
4. **예외**: 게이트웨이 오류는 도메인에서 결제 실패로 처리(선점 해제·주문 미생성).

## 테스트 관점 (원칙 I)

- 단위: 모의 게이트웨이의 승인/거절 규칙, 멱등 재호출 결과 동일성.
- 통합: `/checkout`에서 승인→주문 생성, 거절→주문 미생성·재고 복원 경로.
