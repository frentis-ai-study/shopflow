# Phase 1 데이터 모델: 멀티셀러 핵심 커머스

**기능**: [spec.md](./spec.md) · **리서치**: [research.md](./research.md) · **관련 이슈**: #2

사양의 핵심 엔티티(spec §핵심 엔티티)와 리서치 결정을 반영한 도메인 모델. 금액은 정수 원
(`long`), 상태는 명시적 enum. 관계·검증·상태 전이를 정의한다.

---

## 엔티티 개요 (ER 요약)

```text
User 1─1 Seller            (SELLER 역할 시 판매자 프로필)
Seller 1─* Product         (판매자 소유)
User 1─1 Cart 1─* CartItem  (구매자 장바구니)
User 1─* Order              (구매자 주문)
Order 1─* SubOrder       (판매자별 하위주문 = 정산 단위)
SubOrder *─1 User(seller)
SubOrder 1─* OrderLine   (스냅샷 라인)
SubOrder 1─1 Delivery    (배송 이행 단위)
Order 1─1 Payment
Product 1─* StockReservation
Payment 1─1 PaymentIdempotency(key)
```

> User는 역할(구매자/판매자)을 복수 보유(FR-004). "판매자"는 상품·하위주문을 소유한 User.
>
> **바운디드 컨텍스트**: 계정 / 상품 / 주문(장바구니 포함) / 결제 / 재고 / 배송 / 공통.
> SubOrder는 **주문 컨텍스트의 정산 단위**(판매자별 금액), 배송 상태·시각은 **배송 컨텍스트의
> Delivery**가 담당한다(관심사 분리, ADR-0008).

---

## User (사용자)

| 필드 | 타입 | 설명·검증 |
|---|---|---|
| id | Long (PK) | 식별자 |
| email | String (UNIQUE) | 이메일 형식·중복 불가 (FR-002) |
| passwordHash | String | BCrypt 해시 (원문 저장 금지) |
| displayName | String | 표시명 |
| roles | Set<Role> | {BUYER, SELLER} 복수 보유 (FR-004) |
| createdAt | Instant | 생성 시각 |

- **Role**(enum): `BUYER`, `SELLER`.
- 검증: email 형식·유일, 비밀번호 정책(길이 등).

## Seller (판매자 프로필) — 계정 컨텍스트

`SELLER` 역할을 가진 User의 판매 활동 정보. User와 1:1. (ADR-0009)

| 필드 | 타입 | 설명·검증 |
|---|---|---|
| id | Long (PK) | 판매자 식별자 |
| userId | Long (FK→User, UNIQUE) | 소유 사용자(1:1) |
| sellerType | SellerType | 개인/개인사업자/법인 |
| storeName | String | 스토어 표시명(구매자 노출), 필수 |
| businessRegistrationNumber | String? | 사업자등록번호 — 사업자·법인이면 필수, 개인이면 없음 |
| representativeName | String? | 대표자명 — 사업자·법인이면 필수 |
| contactPhone | String | 연락처 |
| contactEmail | String | 연락 이메일 |
| status | SellerStatus | `ACTIVE`(입점중)/`SUSPENDED`(정지) |
| createdAt | Instant | 생성 시각 |

- **SellerType**(enum): `INDIVIDUAL`(개인) / `SOLE_PROPRIETOR`(개인사업자) / `CORPORATION`(법인).
- **SellerStatus**(enum): `ACTIVE`, `SUSPENDED`. `SUSPENDED`면 신규 상품 등록·판매 제한.
- **검증**:
  - `sellerType != INDIVIDUAL`이면 `businessRegistrationNumber`·`representativeName` 필수.
  - 사업자등록번호는 형식(자릿수) 검증, 중복 불가.
- 정산 계좌·세금계산서 정보는 **정산 기능(후속 이슈)** 에서 확장한다.

## Product (상품)

| 필드 | 타입 | 설명·검증 |
|---|---|---|
| id | Long (PK) | 식별자 |
| sellerId | Long (FK→Seller) | 소유 판매자 (FR-009 권한 기준) |
| name | String | 이름, 필수 |
| description | String | 설명 |
| priceKrw | long | 판매가(정수 원), ≥ 0 |
| stock | int | 총 재고 수량, ≥ 0 |
| reserved | int | 선점 중 수량, 0 ≤ reserved ≤ stock |
| imageUrl | String | 이미지 참조(주소), 선택 |
| status | ProductStatus | `ON_SALE`/`OFF_SALE` (FR-008) |
| version | long | 낙관적 버전(보조) |

- **가용 재고** = `stock - reserved`. 선점·차감은 이 값을 기준으로 판정(R1).
- **ProductStatus**(enum): `ON_SALE`(판매중), `OFF_SALE`(판매중지 — 목록/검색 미노출).
- 검증: priceKrw·stock 음수 불가; `OFF_SALE`은 구매자 조회에서 제외.

## Cart / CartItem (장바구니)

**Cart**

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 식별자 |
| buyerId | Long (FK→User) | 소유 구매자(1인 1카트) |

**CartItem**

| 필드 | 타입 | 설명·검증 |
|---|---|---|
| id | Long (PK) | 식별자 |
| cartId | Long (FK→Cart) | 소속 장바구니 |
| productId | Long (FK→Product) | 담은 상품 |
| quantity | int | 수량 ≥ 1 |

- 담기 시점 재고 초과는 안내만, 확정 판정은 체크아웃 선점에서(UC-08/09).

## Order (주문)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 주문 식별자(주문번호) |
| buyerId | Long (FK→User) | 구매자 |
| totalKrw | long | 총액(정수 원), 서버 산출 (FR-019) |
| shippingAddress | Address(값) | 배송지(수령인·주소·연락처) |
| status | OrderStatus | 하위주문 집계 상태 |
| placedAt | Instant | 결제완료 시각 |

- **OrderStatus**(enum, 집계): `PAID`, `PARTIALLY_SHIPPED`, `SHIPPED`, `COMPLETED`.
  하위주문에 연결된 Delivery 상태들의 집계로 산출(R4). (예: 모든 Delivery가 DELIVERED →
  `COMPLETED`.)
- 한 번의 결제 = 하나의 Order(멱등, R3).

## SubOrder (하위주문 = 판매자별 정산·주문 단위) — 주문 컨텍스트

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 하위주문 식별자 |
| orderId | Long (FK→Order) | 소속 주문 |
| sellerId | Long (FK→Seller) | 담당 판매자 |
| subtotalKrw | long | 이 판매자 분 소계(정수 원) — 향후 정산 기준 |

- SubOrder는 **판매자별 금액·주문 단위**만 책임진다. 배송 상태·시각은 Delivery(배송 컨텍스트)가
  담당한다(관심사 분리, ADR-0008).
- 판매자별 조회·정산 기준(SC-006, FR-010/FR-021)은 SubOrder를 통한다.

## Delivery (배송 이행 단위) — 배송 컨텍스트

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 배송 식별자 |
| subOrderId | Long (FK→SubOrder) | 대상 하위주문(1:1) |
| status | DeliveryStatus | 배송 상태 |
| shippedAt | Instant? | 배송중 전이 시각 |
| deliveredAt | Instant? | 배송완료 시각(향후 정산 트리거) |

- **DeliveryStatus**(enum): `PENDING`(배송대기) → `SHIPPING`(배송중) → `DELIVERED`(배송완료).
  결제 완료 시 각 SubOrder에 대해 `PENDING` Delivery가 생성된다.
- **상태 전이 규칙**(R4, FR-011/FR-022):
  - 허용: `PENDING→SHIPPING`, `SHIPPING→DELIVERED`.
  - 금지: 건너뛰기(`PENDING→DELIVERED`), 역행(`SHIPPING→PENDING` 등).
  - 각 전이는 감사 로그 기록(전이 주체·시각·이전/이후).
  - 소유 판매자(SubOrder.sellerId)만 전이 가능(FR-009).
- 향후 송장·추적번호·부분배송은 이 컨텍스트에서 확장한다.

## OrderLine (주문 라인 — 상품 스냅샷) — 주문 컨텍스트

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 식별자 |
| subOrderId | Long (FK→SubOrder) | 소속 하위주문 |
| productId | Long (FK→Product) | 참조 상품 |
| productNameSnapshot | String | 결제 시점 상품명 스냅샷 |
| unitPriceKrwSnapshot | long | 결제 시점 단가 스냅샷 (FR-023) |
| quantity | int | 수량 ≥ 1 |

- 결제 시점 가격·이름을 스냅샷으로 고정 → 이후 상품 변경이 주문에 영향 없음(FR-023, UC-06).

## Payment (결제)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 식별자 |
| orderId | Long (FK→Order) | 대상 주문 |
| idempotencyKey | String (UNIQUE) | 서버 발급 UUID (R3) |
| amountKrw | long | 결제 금액(서버 검증값) |
| status | PaymentStatus | 결제 상태 |
| gatewayRef | String? | 게이트웨이 참조 |
| createdAt | Instant | 시각 |

- **PaymentStatus**(enum): `APPROVED`(승인), `REJECTED`(거절).
- 카드 원본 정보 필드 없음(FR-024, 원칙 II).

## PaymentIdempotency (멱등 레코드)

| 필드 | 타입 | 설명 |
|---|---|---|
| key | String (PK/UNIQUE) | 멱등키(UUID) |
| requestHash | String | 요청 지문(중복 판정 보조) |
| status | String | 처리 상태 |
| orderId | Long? | 생성된 주문 참조 |
| createdAt | Instant | 시각 |

- 동일 키 재요청 시 기존 `orderId`·결과 반환, 재처리 안 함(FR-018, SC-004).

## StockReservation (재고 선점)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 식별자 |
| productId | Long (FK→Product) | 대상 상품 |
| buyerId | Long (FK→User) | 선점 구매자 |
| quantity | int | 선점 수량 ≥ 1 |
| status | ReservationStatus | 선점 상태 |
| expiresAt | Instant | 만료 시각(생성 + 10분, R2) |
| createdAt | Instant | 시각 |

- **ReservationStatus**(enum): `HELD`(선점중) → `CONFIRMED`(확정 차감) / `RELEASED`(해제).
- **수명주기**(R1/R2, FR-017):
  - 생성: 조건부 UPDATE로 `product.reserved += qty` 성공 시 `HELD`, `expiresAt = now+10m`.
  - 결제 성공: `HELD→CONFIRMED`, `product.stock -= qty` 및 `reserved -= qty`(확정 차감).
  - 결제 실패/취소: `HELD→RELEASED`, `reserved -= qty`(복원).
  - 만료: 스윕이 `HELD` 중 `expiresAt < now`를 `RELEASED`로, `reserved -= qty`(SC-005).

---

## 값 객체·공통

- **Money**: 정수 원(`long`) 래핑 값객체. 덧셈·곱셈만 허용, 음수·오버플로 방어(R7).
- **Address**: 수령인, 주소, 연락처(임베디드 값).

## 불변식 (Invariants)

1. `0 ≤ product.reserved ≤ product.stock` (초과 판매 불가, SC-003).
2. 하나의 `idempotencyKey`는 최대 하나의 `Order`를 생성한다(SC-004).
3. `Delivery` 상태 전이는 허용 경로만(R4). `SubOrder`는 1:1 `Delivery`를 가진다.
4. `OrderLine`의 단가·상품명은 결제 후 불변(FR-023).
5. 금액 필드는 모두 정수 원, 음수 불가(R7).
