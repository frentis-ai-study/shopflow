# 계약: HTTP 라우트 (서버사이드 렌더링)

**기능**: [spec.md](../spec.md) · **데이터 모델**: [data-model.md](../data-model.md)

ShopFlow가 노출하는 HTTP 인터페이스. 서버사이드 렌더링이므로 GET은 Thymeleaf 뷰를 반환하고,
상태 변경(POST)은 처리 후 리다이렉트(PRG 패턴)한다. 모든 상태 변경은 인증·CSRF·서버측
검증을 강제한다(원칙 II). 인증 필요 컬럼: 🔓 공개 / 🔒 로그인 / 🛒 구매자 / 🏪 판매자.

## 계정·인증 (UC-01, UC-02)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/signup` | 🔓 | 회원가입 폼 |
| POST | `/signup` | 🔓 | 계정 생성. 성공 → 로그인 페이지. 실패 → 폼+오류 |
| GET | `/login` | 🔓 | 로그인 폼 |
| POST | `/login` | 🔓 | Spring Security 폼 로그인(세션 시작) |
| POST | `/logout` | 🔒 | 로그아웃(세션 종료) |

## 상품·탐색 (UC-03, UC-04)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/` 또는 `/products` | 🔓 | 판매중 상품 목록. `?q=` 이름 검색 (FR-012) |
| GET | `/products/{id}` | 🔓 | 상품 상세(가격·재고 유무·판매자) (FR-013) |

## 판매자 상품 관리 (UC-05, UC-06, UC-07)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/seller/products` | 🏪 | 내 상품 목록 |
| GET | `/seller/products/new` | 🏪 | 상품 등록 폼 |
| POST | `/seller/products` | 🏪 | 상품 등록 (FR-006). 소유자=현재 판매자 |
| GET | `/seller/products/{id}/edit` | 🏪 | 상품 수정 폼(소유자만) |
| POST | `/seller/products/{id}` | 🏪 | 상품 정보 수정 (FR-007/FR-009) |
| POST | `/seller/products/{id}/status` | 🏪 | 판매중/판매중지 전환 (FR-008) |

- 타 판매자 상품 접근 → 403 (FR-009).

## 장바구니 (UC-08)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/cart` | 🛒 | 내 장바구니 |
| POST | `/cart/items` | 🛒 | 상품 담기(productId, quantity) (FR-014) |
| POST | `/cart/items/{itemId}` | 🛒 | 수량 변경 |
| POST | `/cart/items/{itemId}/delete` | 🛒 | 항목 삭제 |

## 체크아웃·결제 (UC-09) ⭐

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/checkout` | 🛒 | 배송지 입력 + 결제 폼. **서버가 멱등키(UUID) 발급해 숨은 필드로 전달** (R3) |
| POST | `/checkout` | 🛒 | 결제 처리. 본문: 배송지 + `idempotencyKey`. 흐름 R1~R5 |
| GET | `/orders/{id}/complete` | 🛒 | 결제 완료 안내(주문번호) |

**POST `/checkout` 계약(의미)**:
1. 서버측 금액 재계산·검증(클라이언트 금액 무시) (FR-019).
2. 멱등키 기록 시도 → 중복이면 기존 주문 결과로 리다이렉트(재처리 없음) (FR-018/SC-004).
3. 조건부 UPDATE로 재고 선점(TTL 10분). 실패 → 품절 안내(재고 불변) (FR-017/FR-020/SC-003).
4. `PaymentGateway.authorize(...)` 호출(모의) (→ [payment-gateway.md](./payment-gateway.md)).
5. 성공 → 선점 확정 차감, 주문 `PAID` 생성, 판매자별 이행 단위 분리+스냅샷 (FR-021/FR-023).
   실패 → 선점 해제, 주문 미생성, 실패 안내.
6. PRG로 완료 페이지 리다이렉트.

## 주문 조회·이행 (UC-10, UC-11, UC-12)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/orders` | 🛒 | 내 주문 목록·상태 (FR-016) |
| GET | `/orders/{id}` | 🛒 | 주문 상세(판매자별 이행 단위·상태) (FR-022) |
| GET | `/seller/orders` | 🏪 | 내게 배정된 이행 단위 목록 (FR-010, 본인 것만) |
| POST | `/seller/orders/{fulfillmentId}/ship` | 🏪 | `PAID→SHIPPING` 전이 (FR-011) |
| POST | `/seller/orders/{fulfillmentId}/deliver` | 🏪 | `SHIPPING→DELIVERED` 전이(완료 시각 기록) |

- 이행 전이는 소유 판매자만, 허용 전이만(그 외 400/403) (R4/FR-009).

## 공통 규칙

- 상태 변경은 모두 POST + CSRF 토큰 필수. 미인증 접근 → `/login` 리다이렉트.
- 검증 실패 → 동일 폼 뷰 + 필드별 오류(한글).
- 오류 응답 메시지는 사용자 친화적 한글, 민감정보 비노출.
