# 태스크: 멀티셀러 핵심 커머스 (구매·판매)

**기능 디렉터리**: `specs/001-marketplace-core` · **관련 이슈**: #2
**입력**: [plan.md](./plan.md), [spec.md](./spec.md), [data-model.md](./data-model.md),
[contracts/](./contracts/), [research.md](./research.md), [ADR](../../docs/adr/README.md)

**스택**: Java 21 · Spring Boot 3.3 (Data REST + 커스텀 REST, Data JPA, MVC+Thymeleaf 프론트
제어, Security) · PostgreSQL 16 · Flyway · Maven · JUnit 5 + Testcontainers

**테스트 정책**: 헌장 원칙 I(테스트 우선, 타협 불가)에 따라 모든 비즈니스 로직·커머스 핵심
흐름은 테스트를 먼저 작성하고 실패를 확인한 뒤 구현한다.

**경로 규칙**: 백엔드 `src/main/java/com/shopflow/<context>/{rest,domain,repository}`,
프론트 제어 `.../webmvc/{page,client}`, 리소스 `src/main/resources/...`,
테스트 `src/test/java/com/shopflow/{unit,integration,web}`.

**표기**: `[P]` = 병렬 가능(다른 파일·선행 의존 없음), `[US#]` = 사용자 스토리.

---

## Phase 1: Setup (프로젝트 초기화)

- [ ] T001 Maven 프로젝트 초기화 `pom.xml` — Spring Boot 3.3(starter-web, data-rest, data-jpa, thymeleaf, security, validation), Flyway, PostgreSQL 드라이버, Testcontainers, JUnit 5
- [ ] T002 [P] `src/main/resources/application.yml` — 포트(`BACKEND_PORT`)·DB(`POSTGRES_PORT`)·시크릿을 환경변수로 바인딩, `local`/`test` 프로파일
- [ ] T003 [P] `.envrc`(use_dev_ports) 생성 + `direnv allow` (dev-init), 포트 슬롯 확인
- [ ] T004 [P] `docker-compose.yml` — PostgreSQL 포트 `"${POSTGRES_PORT:-15432}:5432"`
- [ ] T005 [P] `src/main/java/com/shopflow/ShopFlowApplication.java` 부트스트랩
- [ ] T006 [P] `.gitignore`·빌드 확인, `README` dev 실행 절 업데이트(`./mvnw spring-boot:run`)

## Phase 2: Foundational (모든 스토리의 선행 — 완료 전 스토리 착수 불가)

- [ ] T007 `src/main/resources/db/migration/V1__init.sql` — 전체 스키마(enum 8종, 13 테이블, 인덱스, 컨텍스트 내부 FK) [schema.sql 기준]
- [ ] T008 [P] `common/domain/Money.java` — 정수 원 값객체(plus/times, 음수·오버플로 방어)
- [ ] T009 [P] `common/domain/Address.java` — 임베더블 값객체(수령인·주소·연락처)
- [ ] T010 [P] `common/error/` — 도메인 예외 + 전역 예외 핸들러(한글 메시지, 민감정보 비노출)
- [ ] T011 [P] `common/logging/` — 구조화 로깅 + correlation id 필터 (원칙 IV)
- [ ] T012 `common/config/SecurityConfig.java` — 인증·인가·CSRF 골격(폼 로그인 자리)
- [ ] T013 [P] `common/config/RestExposureConfig.java` — Spring Data REST 노출 정책(기본 비노출, 화이트리스트·쓰기 제한)
- [ ] T014 [P] `webmvc/client/RestApiClient.java` — 백엔드 REST 호출 클라이언트(RestClient) 골격
- [ ] T015 [P] `webmvc/page/` 공통 Thymeleaf 레이아웃 프래그먼트(한글)
- [ ] T016 [P] `src/test/java/com/shopflow/support/PostgresContainerBase.java` — Testcontainers 통합테스트 베이스

**Checkpoint**: 스키마·공통·보안·REST 노출 정책·프론트 클라이언트 준비 완료.

---

## Phase 3: US3 — 가입·로그인 (Priority: P1) 🎯 MVP 기반

**목표**: 방문자가 구매자/판매자로 가입·로그인하고, 보호된 작업에 인증·권한이 강제된다.
**독립 테스트**: 가입 → 로그인 → 보호 페이지 접근 가능/비로그인 차단을 검증.

### 테스트 (먼저 작성·실패 확인)
- [ ] T017 [P] [US3] `test/unit/account/EmailPasswordPolicyTest.java` — 이메일 형식·중복·비밀번호 정책
- [ ] T018 [P] [US3] `test/unit/account/SellerValidationTest.java` — 유형별 사업자등록번호·대표자 필수(개인 제외)
- [ ] T019 [P] [US3] `test/web/account/AuthWebTest.java` — 로그인·CSRF·비로그인 리다이렉트

### 구현
- [ ] T020 [P] [US3] `account/domain/User.java`, `account/domain/Role.java`
- [ ] T021 [P] [US3] `account/domain/Seller.java`, `SellerType.java`, `SellerStatus.java`
- [ ] T022 [US3] `account/repository/UserRepository.java`, `SellerRepository.java` (RestRepository, 제한 노출)
- [ ] T023 [US3] `account/domain/UserService.java`(가입·검증·BCrypt), `SellerService.java`(유형별 검증)
- [ ] T024 [US3] `common/config/SecurityConfig.java` 완성 — 폼 로그인·로그아웃·BCrypt·권한
- [ ] T025 [P] [US3] `webmvc/page/SignupPageController.java` + `templates/signup.html`
- [ ] T026 [P] [US3] `webmvc/page/LoginPageController.java` + `templates/login.html`
- [ ] T027 [US3] `test/integration/AuthFlowIT.java` — 가입→로그인→보호 페이지 접근(Testcontainers)

**Checkpoint**: 인증·권한·판매자 프로필 동작. 이후 스토리의 전제.

---

## Phase 4: US2 — 판매자 상품 등록·재고 관리 (Priority: P1)

**목표**: 판매자가 상품을 등록·수정하고 판매 상태를 전환한다.
**독립 테스트**: 상품 등록 → 구매자 목록 노출, 재고 수정 반영, 타 판매자 접근 차단.
**의존**: US3(판매자 인증).

### 테스트
- [ ] T028 [P] [US2] `test/unit/product/ProductServiceTest.java` — 등록·수정 검증, 가격·재고 음수 불가
- [ ] T029 [P] [US2] `test/web/product/ProductRestTest.java` — REST 리포지토리 노출·소유권 권한(403)

### 구현
- [ ] T030 [P] [US2] `product/domain/Product.java`, `ProductStatus.java`
- [ ] T031 [US2] `product/repository/ProductRepository.java` (RestRepository, 판매중 검색 쿼리)
- [ ] T032 [US2] `product/domain/ProductService.java` — 등록·수정 규칙, 소유권 검증
- [ ] T033 [US2] `product/rest/ProductOpsController.java` — 판매중/판매중지 전환(커스텀 REST)
- [ ] T034 [P] [US2] `webmvc/page/SellerProductPageController.java` + `templates/seller/product-*.html`
- [ ] T035 [P] [US2] `webmvc/page/CatalogPageController.java` + `templates/product-list.html`,`product-detail.html`(구매자 탐색·검색·상세)
- [ ] T036 [US2] `test/integration/ProductCatalogIT.java` — 등록→노출, 판매중지 미노출, 타 판매자 차단

**Checkpoint**: 판매 상품 존재 → 구매 흐름 전제 충족.

---

## Phase 5: US1 — 구매자 결제 (Priority: P1) ⭐ 핵심 매출

**목표**: 구매자가 장바구니→결제로 주문을 확정한다. 재고 선점·멱등 결제·판매자별 분리 보장.
**독립 테스트**: 결제 해피패스 + 초과판매 0 + 이중결제 0 + 결제실패 복원 + TTL 복원.
**의존**: US3(인증), US2(상품).

### 테스트 (먼저 작성·실패 확인)
- [ ] T037 [P] [US1] `test/unit/inventory/ReservationConcurrencyTest.java` — 조건부 UPDATE 초과판매 방지
- [ ] T038 [P] [US1] `test/unit/payment/IdempotencyTest.java` — 동일 멱등키 정확히 1회
- [ ] T039 [P] [US1] `test/unit/order/OrderSplitSnapshotTest.java` — 판매자별 분리·스냅샷·총액 산출
- [ ] T040 [P] [US1] `test/unit/payment/MockGatewayTest.java` — 승인/거절/멱등 규칙
- [ ] T041 [P] [US1] `test/integration/CheckoutHappyPathIT.java` — 주문 PAID 생성·재고 확정 차감(S2)
- [ ] T042 [P] [US1] `test/integration/OversellConcurrencyIT.java` — 동시 결제 초과판매 0(S3)
- [ ] T043 [P] [US1] `test/integration/IdempotentRetryIT.java` — 재시도 이중결제 0(S4)
- [ ] T044 [P] [US1] `test/integration/PaymentRejectRestoreIT.java` — 거절 시 재고 복원·주문 미생성(S6)
- [ ] T045 [P] [US1] `test/integration/ReservationTtlSweepIT.java` — TTL 만료 복원(S5)

### 구현 — 장바구니
- [ ] T046 [P] [US1] `order/domain/Cart.java`, `CartItem.java`
- [ ] T047 [US1] `order/repository/CartRepository.java`(RestRepository) + `order/domain/CartService.java`
- [ ] T048 [P] [US1] `webmvc/page/CartPageController.java` + `templates/cart.html`

### 구현 — 재고 선점
- [ ] T049 [P] [US1] `inventory/domain/StockReservation.java`, `ReservationStatus.java`
- [ ] T050 [US1] `inventory/domain/ReservationService.java` — 원자적 조건부 UPDATE 선점/확정/해제
- [ ] T051 [US1] `inventory/scheduler/ExpirySweeper.java` — `@Scheduled` TTL 만료 해제(SC-005)

### 구현 — 결제
- [ ] T052 [P] [US1] `payment/domain/Payment.java`, `PaymentIdempotency.java`, `PaymentStatus.java`
- [ ] T053 [US1] `payment/domain/PaymentGateway.java`(포트) + `payment/adapter/MockPaymentGateway.java`
- [ ] T054 [US1] `payment/domain/IdempotencyStore.java` + `PaymentService.java`(멱등 결제)

### 구현 — 주문·체크아웃·배송 생성
- [ ] T055 [P] [US1] `order/domain/Order.java`, `SubOrder.java`, `OrderLine.java`, `OrderStatus.java`
- [ ] T056 [US1] `order/domain/OrderService.java` — 판매자별 분리·상품 스냅샷·총액·상태 집계 + `OrderRepository`
- [ ] T057 [US1] `delivery/domain/Delivery.java`, `DeliveryStatus.java` + 결제 완료 시 SubOrder마다 PENDING 생성
- [ ] T058 [US1] `order/rest/CheckoutController.java` + `order/domain/CheckoutService.java` — 금액검증→멱등→선점→결제→주문/SubOrder→배송 생성(오케스트레이션)
- [ ] T059 [P] [US1] `webmvc/page/CheckoutPageController.java` + `templates/checkout.html`,`order-complete.html`(멱등키 발급)
- [ ] T060 [US1] 통합 확인: [quickstart](./quickstart.md) S2 해피패스 e2e 재현

**Checkpoint**: 구매→결제→주문 확정 동작. 최소 매출 루프 완성(MVP).

---

## Phase 6: US4 — 판매자 배송 처리 (Priority: P2)

**목표**: 판매자가 자기 하위주문의 배송을 배송중→배송완료로 전이한다.
**독립 테스트**: 결제된 주문을 판매자가 조회, 상태 전이가 구매자 화면·주문 집계에 반영.
**의존**: US1(주문·배송 존재).

### 테스트
- [ ] T061 [P] [US4] `test/unit/delivery/DeliveryTransitionTest.java` — 허용/금지 전이(건너뛰기·역행 거절)
- [ ] T062 [P] [US4] `test/web/delivery/DeliveryOpsWebTest.java` — 소유 판매자만 전이(403)
- [ ] T063 [P] [US4] `test/integration/DeliveryFulfillmentIT.java` — 배송중→배송완료→주문 집계·구매자 반영(S7)

### 구현
- [ ] T064 [US4] `delivery/repository/DeliveryRepository.java`(RestRepository, 조회) + `delivery/domain/DeliveryService.java`(전이·감사 기록)
- [ ] T065 [US4] `delivery/rest/DeliveryOpsController.java` — ship/deliver 커스텀 REST(소유권 검증)
- [ ] T066 [US4] `order/domain/OrderService.java` 확장 — Delivery 상태 기반 OrderStatus 집계 갱신
- [ ] T067 [P] [US4] `webmvc/page/SellerOrderPageController.java` + `templates/seller/orders.html`

**Checkpoint**: 판매자 배송 이행 동작. 정산 트리거(배송완료 시각) 기록됨.

---

## Phase 7: US5 — 구매자 내 주문 조회 (Priority: P2)

**목표**: 구매자가 자기 주문 목록·상태(판매자별 이행 포함)를 확인한다.
**독립 테스트**: 결제한 주문이 목록에 표시, 판매자별 하위주문·배송 상태 구분 표시.
**의존**: US1(주문 존재).

### 테스트
- [ ] T068 [P] [US5] `test/web/order/OrderQueryTest.java` — 본인 주문만 조회(권한), 상태 정확성
- [ ] T069 [P] [US5] `test/integration/OrderHistoryIT.java` — 다중 판매자 주문 상세 표시

### 구현
- [ ] T070 [US5] `order/repository/OrderRepository.java` 조회 노출/프로젝션(본인 주문·판매자별 이행 상태 포함)
- [ ] T071 [P] [US5] `webmvc/page/OrderPageController.java` + `templates/orders.html`,`order-detail.html`

**Checkpoint**: 구매자 주문 가시성 확보.

---

## Phase 8: Polish & 교차 관심사

- [ ] T072 [P] 감사 기록 완성 — 주문·결제·배송 상태 전이 이벤트 로그(원칙 IV), 로그 민감정보 마스킹
- [ ] T073 [P] Spring Data REST 노출 하드닝 — 프로젝션·쓰기 제한 재점검(민감 필드/무결성 리소스)
- [ ] T074 [P] 검증·오류 메시지 한글화, 오류 페이지(4xx/5xx) 템플릿
- [ ] T075 [P] `dev-ports` 하드코딩 점검 — dev 명령·compose 포트 전부 환경변수 경유
- [ ] T076 [US1] [US4] quickstart S1~S8 전체 검증 스크립트/체크 실행
- [ ] T077 [P] `CLAUDE.md`(에이전트 가이드) + README 아키텍처·실행·컨텍스트 문서화
- [ ] T078 [P] 코드리뷰 서브에이전트로 헌장 준수(테스트/결제·보안/무결성) 최종 점검

---

## 의존성 그래프 (스토리 완료 순서)

```
Setup(P1) → Foundational(P2)
                 │
                 ▼
            US3 가입·로그인 (P1)  ← 다른 스토리의 전제(인증)
                 │
                 ▼
            US2 상품·재고 (P1)    ← 구매 대상 존재
                 │
                 ▼
            US1 구매자 결제 (P1) ⭐ ← 주문·배송·결제 생성
                 ├────────────────┐
                 ▼                ▼
        US4 판매자 배송 (P2)   US5 내 주문 조회 (P2)
                 │                │
                 └───────┬────────┘
                         ▼
                   Polish (교차 관심사)
```

- US1은 US2·US3 완료 후 착수(상품·인증 필요). US4·US5는 US1 이후 병렬 가능.

## 병렬 실행 기회

- **Setup/Foundational**: `[P]` 태스크 다수 동시 진행(값객체·로깅·예외·REST 정책·프론트 클라이언트).
- **각 스토리 테스트**: 도메인·웹·통합 테스트를 `[P]`로 동시 작성.
- **엔티티 생성**: 컨텍스트별 도메인 클래스(`[P]`)는 서로 다른 파일이라 병렬.
- **US4 ∥ US5**: US1 완료 후 두 P2 스토리를 병렬 개발.

## 구현 전략 (MVP 우선, 점진 배포)

1. **MVP = US3 + US2 + US1** (전부 P1) — "가입→상품→구매·결제"의 최소 매출 루프.
2. 이후 **US4(배송), US5(주문 조회)** 로 이행·가시성 보강.
3. **Polish** 로 관측성·REST 하드닝·문서·최종 리뷰.
4. 각 스토리는 테스트 우선(원칙 I)으로, 결제·재고·상태전이는 통합 테스트로 결정적 검증.
