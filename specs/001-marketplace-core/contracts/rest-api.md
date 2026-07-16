# 계약: 백엔드 REST API (구현 반영, T066)

**기능**: [spec.md](../spec.md) · **관련 이슈**: #2 · ADR-0011(REST 백엔드), ADR-0010 v2

백엔드가 노출하는 실제 REST 엔드포인트. 프론트엔드(별도 인스턴스)가 이 계약을 소비한다.
리소스는 `/api` 하위(프론트 경로와 격리). 인증은 세션 기반(로그인 후 세션 쿠키). 미인증 접근은
**401**, 권한 없음은 **403**을 반환한다(리다이렉트 없음).

인증 표기: 🔓 공개 / 🔒 로그인 / 🛒 구매자 / 🏪 판매자(활성).

## 계정·인증

| 메서드 | 경로 | 인증 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/api/signup` | 🔓 | `{email, password, displayName}` | 201 `{userId, email, displayName}` / 409 중복 / 400 형식 |
| POST | `/login` | 🔓 | form: `username`(email), `password` | 세션 시작(200) / 401 |
| POST | `/logout` | 🔒 | — | 세션 종료 |
| POST | `/api/seller` | 🔒 | `{sellerType, storeName, businessRegistrationNumber?, representativeName?, contactPhone?, contactEmail?}` | 201 `{sellerId, storeName, status}` / 400 유형별 검증 |

## 상품

| 메서드 | 경로 | 인증 | 설명 | 응답 |
|---|---|---|---|---|
| GET | `/api/products?q=` | 🔓 | 판매중 상품 목록·이름 검색 | 200 `[{id, name, description, priceKrw, inStock, imageUrl, status, sellerId}]` |
| GET | `/api/products/{id}` | 🔓 | 상품 상세 | 200 ProductView / 404 |
| GET | `/api/seller/products` | 🏪 | 내 상품 목록 | 200 `[ProductView]` |
| POST | `/api/seller/products` | 🏪 | 등록 `{name, description?, priceKrw, stock, imageUrl?}` | 201 ProductView |
| PUT | `/api/seller/products/{id}` | 🏪 | 수정 `{name, description?, priceKrw, imageUrl?}` | 200 / 403 타 판매자 |
| PUT | `/api/seller/products/{id}/stock` | 🏪 | 재고 변경 `{stock}` | 200 / 409 선점 미만 하향 |
| POST | `/api/seller/products/{id}/status` | 🏪 | 판매상태 `{status: ON_SALE\|OFF_SALE}` | 200 |

## 장바구니

| 메서드 | 경로 | 인증 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/api/cart` | 🛒 | — | 200 `[{id, productId, quantity}]` |
| POST | `/api/cart/items` | 🛒 | `{productId, quantity}` | 201 (동일 상품 누적) |
| PUT | `/api/cart/items/{itemId}` | 🛒 | `{quantity}` | 200 |
| POST | `/api/cart/items/{itemId}/delete` | 🛒 | — | 204 |

## 체크아웃·결제

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/checkout` | 🛒 | 멱등키 발급 → `{idempotencyKey}` |
| POST | `/api/checkout` | 🛒 | `{recipient, address, phone, idempotencyKey}` → 결제 |

**POST `/api/checkout` 응답**:
- `201` `{result:"CREATED", orderId, totalKrw, message}` — 결제 성공, 주문 생성
- `200` `{result:"EXISTING", orderId, ...}` — 동일 멱등키 재요청(이중결제 0, SC-004)
- `402` `{result:"REJECTED", message}` — 결제 거절(선점 해제·재고 복원)
- `409` — 재고 부족(품절), 결제 진행 중
- `400` — 빈 장바구니

> 금액은 서버가 장바구니·상품가로 계산·검증한다(클라이언트 금액 미수신, FR-019).

## 주문 조회

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/orders` | 🛒 | 200 `[{id, totalKrw, status, ...}]` (본인만) |
| GET | `/api/orders/{id}` | 🛒 | 200 `{id, totalKrw, status, subOrders:[{subOrderId, sellerStoreName, subtotalKrw, deliveryStatus}]}` / 403 타인 |

## 판매자 주문·배송

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/seller/orders/{subOrderId}/ship` | 🏪 | 배송중 전이(PENDING→SHIPPING), 소유 판매자만 |
| POST | `/api/seller/orders/{subOrderId}/deliver` | 🏪 | 배송완료 전이(SHIPPING→DELIVERED) |

## 오류 응답 형식

`ProblemDetail`(RFC 7807) 기반 — `{status, title, detail}`. detail은 한글 메시지, 민감정보 비노출.

## 미구현/후속(범위 밖)

- 백엔드↔프론트 CSRF/토큰 정책 상세(현재 API CSRF 비활성 — 프론트 분리 시 재정의)
- 영속 감사 원장, 정산·환불·취소, 알림·배송추적(검토 보고서 계층 2/3)
