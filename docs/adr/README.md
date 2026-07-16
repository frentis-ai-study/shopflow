# 아키텍처 결정 기록 (ADR)

이 디렉터리는 ShopFlow의 아키텍처적으로 유의미한 결정을 [MADR](https://adr.github.io/madr/)
형식으로 기록한다. 각 ADR은 되돌리기 비용이 있고 대안이 존재했던 결정을 맥락·결정·결과와
함께 남긴다. 모든 문서는 한글로 작성한다(헌장 원칙 VI).

## 상태 표기

- `제안(Proposed)` — 논의 중
- `수용(Accepted)` — 채택되어 유효
- `대체됨(Superseded)` — 이후 ADR로 대체
- `폐기(Deprecated)` — 더 이상 유효하지 않음

## 목록

| ADR | 제목 | 상태 | 관련 헌장 원칙 |
|---|---|---|---|
| [0001](./0001-server-side-rendering-spring-thymeleaf.md) | 서버사이드 렌더링(Spring Boot + Thymeleaf) 채택 | 수용 | V |
| [0002](./0002-payment-external-delegation-mock-gateway.md) | 결제는 외부 위임, MVP는 모의 게이트웨이 | 수용 | II |
| [0003](./0003-idempotent-payment-exactly-once.md) | 멱등키로 정확히 한 번 결제 | 수용 | III, VII |
| [0004](./0004-stock-reservation-ttl.md) | 결제 전 재고 선점 + TTL 자동 해제 | 수용 | III, VII |
| [0005](./0005-per-seller-order-splitting.md) | 멀티셀러 주문을 판매자별 이행 단위로 분리 | 수용 | VII |
| [0006](./0006-money-fixed-precision.md) | 금액은 고정소수점(정수 최소단위)으로 처리 | 수용 | III |
| [0007](./0007-issue-branch-pr-workflow.md) | 이슈 → 브랜치 → PR 워크플로 강제 | 수용 | VIII |

## 미결정 → 해소됨

- **동시성 제어 방식**(낙관적 vs 비관적 락) — ✅ 해소됨. `/speckit-plan`의
  [research.md](../../specs/001-marketplace-core/research.md) **R1**에서 **원자적 조건부
  UPDATE**(행 잠금 암묵)로 확정. ADR-0004의 세부 후속 결정으로 research에 기록.
