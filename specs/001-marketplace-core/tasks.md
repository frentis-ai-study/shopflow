# 태스크: 멀티셀러 핵심 커머스 (구매·판매)

**기능 디렉터리**: `specs/001-marketplace-core` · **관련 이슈**: #2
**입력**: [plan.md](./plan.md), [spec.md](./spec.md), [data-model.md](./data-model.md),
[contracts/](./contracts/), [research.md](./research.md), [tasks.md 재편 근거: ADR-0010 v2](../../docs/adr/0010-extraction-ready-modular-monolith.md)

**스택**: Java 21 · Spring Boot 3.3 (Data REST + 커스텀 REST, Data JPA, Security; 프론트 제어
MVC는 후행) · PostgreSQL 16 · Flyway · Maven · JUnit 5 + Testcontainers

**개발 순서(ADR-0010 v2)**: 화면 디자인은 외부에서 별도 진행되므로 **백엔드 서비스(Part A)를
먼저 완성**하고, **디자인 완료 시점에 프론트엔드 인스턴스(Part B)** 를 붙인다. 백엔드는 단일
배포 모듈러 모놀리스(백엔드 인스턴스 분리 = Non-goal), 프론트만 별도 인스턴스로 REST 소비.

**테스트 정책**: 헌장 원칙 I(테스트 우선, 타협 불가). 동시성·TTL·멱등은 **통합 테스트
(Testcontainers)** 로 결정적 검증하며, 시간 의존 로직은 `java.time.Clock` 주입으로 조작한다.

**경로 규칙**: 백엔드 `src/main/java/com/shopflow/<context>/{rest,domain,repository}`,
공통 `.../common/...`, 프론트 `.../webmvc/{page,client}`(Part B),
리소스 `src/main/resources/...`, 테스트 `src/test/java/com/shopflow/{unit,integration,web}`.

**표기**: `[P]` = 병렬 가능, `[US#]` = 사용자 스토리, `[BE]`=백엔드, `[FE]`=프론트(디자인 후).

---

# Part A — 백엔드 서비스 (먼저 완성)

## Phase 1: Setup

- [X] T001 Maven `pom.xml` — Spring Boot 3.3(web, data-rest, data-jpa, security, validation), Flyway, PostgreSQL, Testcontainers, JUnit 5
- [X] T002 [P] `src/main/resources/application.yml` — 포트(`BACKEND_PORT`)·DB(`POSTGRES_PORT`)·시크릿 환경변수 바인딩, `local`/`test` 프로파일, `spring.jpa.hibernate.ddl-auto=validate`, `spring.data.rest.base-path=/api`
- [X] T003 [P] `.envrc`(use_dev_ports) + `direnv allow` (dev-init), 포트 슬롯 확인
- [X] T004 [P] `docker-compose.yml` — PostgreSQL `"${POSTGRES_PORT:-15432}:5432"`
- [X] T005 [P] `src/main/java/com/shopflow/ShopFlowApplication.java`
- [X] T006 [P] README dev 실행 절(`./mvnw spring-boot:run`)

## Phase 2: Foundational (모든 스토리 선행)

- [X] T007 `src/main/resources/db/migration/V1__init.sql` — 전체 스키마(enum 8종, 13 테이블, 인덱스, 컨텍스트 내부 FK). enum은 varchar + `@Enumerated(STRING)` 매핑 전제
- [X] T008 [P] `common/domain/Money.java` — 정수 원 값객체(plus/times, 음수·오버플로 방어) + 단위테스트
- [X] T009 [P] `common/domain/Address.java` — 임베더블
- [X] T010 [P] `common/error/` — 도메인 예외 + 전역 예외 핸들러(한글, 민감정보 비노출)
- [X] T011 [P] `common/logging/CorrelationIdFilter.java` — 구조화 로깅 + correlation id (원칙 IV)
- [X] T012 [P] `common/time/ClockConfig.java` — `java.time.Clock` 빈 주입(TTL·시각 결정적 테스트용)
- [X] T013 `common/config/SecurityConfig.java` — 인증·인가·CSRF 골격
- [X] T014 [P] `common/config/RestExposureConfig.java` — Spring Data REST 노출 정책(기본 비노출, 조회 화이트리스트, **쓰기는 커스텀 컨트롤러로만**)
- [X] T015 [P] `src/test/java/com/shopflow/support/PostgresContainerBase.java` — Testcontainers 베이스

**Checkpoint**: 스키마·공통·보안·REST 노출 정책·Clock·테스트 인프라 준비.

---

## Phase 3: US3(BE) — 가입·로그인 서비스 (P1)

**목표**: 가입·로그인·판매자 프로필과 인증·권한(REST/보안)이 동작한다(화면 제외).
**독립 테스트**: API로 가입→로그인(세션)→보호 엔드포인트 접근/차단.

- [X] T016 [P] [US3] `test/unit/account/EmailPasswordPolicyTest.java` — 이메일 형식·중복·비밀번호 정책
- [X] T017 [P] [US3] `test/unit/account/SellerValidationTest.java` — 유형별 사업자등록번호·대표자 필수(개인 제외)
- [X] T018 [P] [US3] `test/web/account/AuthSecurityTest.java` — 로그인·CSRF·비인증 401/403
- [X] T019 [P] [US3] `account/domain/User.java`, `Role.java`
- [X] T020 [P] [US3] `account/domain/Seller.java`, `SellerType.java`, `SellerStatus.java`
- [X] T021 [US3] `account/repository/UserRepository.java`, `SellerRepository.java` (RestRepository, 조회 제한 노출·프로젝션)
- [X] T022 [US3] `account/domain/UserService.java`(가입·BCrypt), `SellerService.java`(유형별 검증)
- [X] T023 [US3] `common/config/SecurityConfig.java` 완성 — 로그인 처리·BCrypt·세션·권한
- [X] T024 [US3] `test/integration/AuthFlowIT.java` — 가입→로그인→보호 엔드포인트(Testcontainers)

---

## Phase 4: US2(BE) — 상품 등록·재고 관리 서비스 (P1)

**목표**: 상품 등록·수정·판매상태 전환, 판매중 상품 조회 API. **의존**: US3.

- [X] T025 [P] [US2] `test/unit/product/ProductServiceTest.java` — 등록·수정 검증, 가격·재고 음수 불가
- [X] T026 [P] [US2] `test/web/product/ProductRestTest.java` — REST 조회 노출·소유권 403(쓰기는 커스텀 경유)
- [X] T027 [P] [US2] `product/domain/Product.java`, `ProductStatus.java`
- [X] T028 [US2] `product/repository/ProductRepository.java` (RestRepository 조회, 판매중 검색 쿼리)
- [X] T029 [US2] `product/domain/ProductService.java` — 등록·수정 규칙, 소유권, `stock>=reserved` 하향 가드
- [X] T030 [US2] `product/rest/ProductController.java` — 상품 생성/수정/판매상태 전환(커스텀 REST, 소유권 인가)
- [X] T031 [US2] `test/integration/ProductCatalogIT.java` — 등록→조회 노출, 판매중지 미노출, 타 판매자 차단

---

## Phase 5: US1(BE) — 구매자 결제 서비스 (P1) ⭐

**목표**: 장바구니→체크아웃으로 주문 확정. 재고 선점·멱등 결제·판매자별 분리·배송 생성.
**의존**: US3, US2.

### 테스트 (먼저 작성·실패 확인)
- [X] T032 [P] [US1] `test/unit/order/OrderSplitSnapshotTest.java` — 판매자별 분리·상품/판매자명 스냅샷·총액
- [X] T033 [P] [US1] `test/unit/payment/MockGatewayTest.java` — 승인/거절/멱등 규칙
- [X] T034 [P] [US1] `test/unit/cart/CartServiceTest.java` — 담기·수량·삭제·upsert 누적 (FR-014)
- [X] T035 [P] [US1] `test/integration/CheckoutHappyPathIT.java` — 주문 PAID·재고 확정 차감 + **서버측 금액 검증(FR-019, 클라이언트 금액 변조 거부)**
- [X] T036 [P] [US1] `test/integration/OversellConcurrencyIT.java` — 동시 결제 초과판매 0 (SC-003, CountDownLatch)
- [X] T037 [P] [US1] `test/integration/IdempotentRetryIT.java` — 동시·재시도 이중결제 0 (SC-004)
- [X] T038 [P] [US1] `test/integration/PaymentRejectRestoreIT.java` — 거절 시 재고 복원·주문 미생성
- [X] T039 [P] [US1] `test/integration/ReservationTtlSweepIT.java` — TTL 만료 복원 (SC-005, Clock 조작)

### 구현
- [X] T040 [P] [US1] `order/domain/Cart.java`, `CartItem.java`
- [X] T041 [US1] `order/domain/CartService.java` + `order/repository/CartRepository.java` (upsert 누적)
- [X] T042 [P] [US1] `inventory/domain/StockReservation.java`, `ReservationStatus.java`
- [X] T043 [US1] `inventory/domain/ReservationService.java` — 원자적 조건부 UPDATE 선점/확정/해제(상태 전이 `WHERE status=...` 원자화). **Product 재고 갱신은 네이티브 `@Modifying` 전용**(엔티티 @Version과 혼용 금지)
- [X] T044 [US1] `inventory/scheduler/ExpirySweeper.java` — `@Scheduled` TTL 해제(Clock 기반, 조건부 UPDATE)
- [X] T045 [P] [US1] `payment/domain/Payment.java`, `PaymentIdempotency.java`, `PaymentStatus.java`
- [X] T046 [US1] `payment/domain/PaymentGateway.java`(포트) + `payment/adapter/MockPaymentGateway.java`(지연·블로킹 테스트 훅 포함)
- [X] T047 [US1] `payment/domain/IdempotencyStore.java` + `PaymentService.java` — 멱등 상태기계(STARTED→DONE/FAILED, 경합 처리)
- [X] T048 [P] [US1] `order/domain/Order.java`, `SubOrder.java`, `OrderLine.java`, `OrderStatus.java`(판매자명 스냅샷 포함)
- [X] T049 [US1] `order/domain/OrderService.java` — 판매자별 분리·스냅샷·총액·상태 집계 + `OrderRepository`
- [X] T050 [US1] `delivery/domain/Delivery.java`, `DeliveryStatus.java` + 결제 완료 시 SubOrder마다 PENDING 생성
- [X] T051 [US1] `order/rest/CheckoutController.java` + `order/domain/CheckoutService.java` — 단일 트랜잭션 오케스트레이션(금액검증→멱등→선점→결제→주문/SubOrder→배송 생성). 판매자 ACTIVE 검사 포함
- [X] T052 [US1] `test/integration/` — [quickstart](./quickstart.md) S2 해피패스 e2e(API)

**Checkpoint**: 백엔드 매출 루프 완성(화면 없이 API로 검증).

---

## Phase 6: US4(BE) — 판매자 배송 처리 서비스 (P2)

**목표**: 판매자가 하위주문 배송을 배송중→배송완료로 전이, 주문 집계 반영. **의존**: US1.

- [X] T053 [P] [US4] `test/unit/delivery/DeliveryTransitionTest.java` — 허용/금지 전이
- [X] T054 [P] [US4] `test/web/delivery/DeliveryOpsWebTest.java` — 소유 판매자만 전이(403)
- [X] T055 [P] [US4] `test/integration/DeliveryFulfillmentIT.java` — 배송중→배송완료→집계 (SC-006)
- [X] T056 [US4] `delivery/repository/DeliveryRepository.java`(조회) + `delivery/domain/DeliveryService.java`(전이·감사)
- [X] T057 [US4] `delivery/rest/DeliveryOpsController.java` — ship/deliver 커스텀 REST(소유권)
- [X] T058 [US4] `order/domain/OrderService.java` 확장 — Delivery 상태 기반 OrderStatus 집계(상호배타 가드)

---

## Phase 7: US5(BE) — 구매자 내 주문 조회 서비스 (P2)

**목표**: 본인 주문 목록·상세(판매자별 이행 상태) API. **의존**: US1.

- [X] T059 [P] [US5] `test/web/order/OrderQueryTest.java` — 본인 주문만(권한)·상태 정확성
- [X] T060 [P] [US5] `test/integration/OrderHistoryIT.java` — 다중 판매자 주문 상세
- [X] T061 [US5] `order/repository/OrderRepository.java` 조회 노출/프로젝션(본인·판매자별 이행 상태)

---

## Phase 8: 백엔드 Polish & 교차 관심사

- [X] T062 [P] 상태 전이 감사 로깅(주문·결제·배송) + 민감정보 마스킹 (원칙 IV) — 영속 감사 원장은 후속 보강 대상으로 명시
- [X] T063 [P] Spring Data REST 노출 하드닝 재점검(프로젝션·쓰기 제한)
- [X] T064 [P] 검증·오류 메시지 한글화(API 응답)
- [X] T065 [P] `dev-ports` 하드코딩 점검(포트 전부 환경변수)
- [X] T066 [P] `contracts/rest-api.md` — 백엔드 REST 계약(엔드포인트·페이로드·인증 전파·`/api` base-path) 문서화(프론트 착수 전제)
- [X] T067 quickstart S1~S8 백엔드(API) 검증
- [X] T068 [P] `CLAUDE.md` + README 아키텍처·실행 문서화
- [ ] T069 코드리뷰 서브에이전트로 헌장 준수(테스트/결제·보안/무결성) 점검

**Checkpoint(Part A 완료)**: 백엔드 서비스 전체 + REST 계약 확정. 프론트 착수 가능 상태.

---

# Part B — 프론트엔드 인스턴스 (외부 디자인 완료 후) 🎨

> **선행 조건**: (1) Part A 백엔드 REST 계약(T066) 확정, (2) 외부 디자인 산출물(화면 시안·
> 디자인 토큰) 수령. 프론트는 백엔드와 **별도 인스턴스**로 REST를 소비한다(ADR-0010 v2, 0011).

- [X] T070 [FE] `webmvc/client/RestApiClient.java` — 백엔드 REST 호출(RestClient), 인증 전파
- [X] T071 [FE] 디자인 산출물 기반 공통 레이아웃·디자인 토큰 적용(`templates/`, static)
- [X] T072 [FE] [US3] 인증 화면 — Signup/Login PageController + 템플릿
- [X] T073 [FE] [US2] 상품 탐색·검색·상세 화면(구매자)
- [X] T074 [FE] [US2] 판매자 상품 관리 화면(목록·등록·수정)
- [X] T075 [FE] [US1] 장바구니·체크아웃·완료 화면(멱등키 처리·선점 타이머 노출)
- [X] T076 [FE] [US4] 판매자 주문·배송 처리 화면
- [X] T077 [FE] [US5] 구매자 주문 목록·상세 화면
- [ ] T078 [FE] 프론트 인스턴스 분리 배포 구성(별도 앱/포트, `FRONTEND_PORT`) + Playwright E2E(백엔드 연동)

---

## 의존성 그래프

```
Part A (백엔드, 먼저)
  Setup → Foundational
            └ US3(BE) → US2(BE) → US1(BE) ⭐ → { US4(BE) ∥ US5(BE) } → 백엔드 Polish
                                                                         │  (+ REST 계약 T066)
                                                                         ▼
Part B (프론트, 외부 디자인 완료 후) ──────────────────────────── 별도 인스턴스로 REST 소비
```

- Part A는 화면 없이 API·테스트로 완결. Part B는 디자인·REST 계약 준비 후 착수.

## 병렬 실행 기회
- Setup/Foundational의 `[P]` 다수 동시.
- 각 스토리: 테스트·엔티티(`[P]`) 동시 작성.
- US4(BE) ∥ US5(BE): US1(BE) 완료 후 병렬.
- **Part A ∥ 외부 디자인**: 백엔드 개발과 화면 디자인이 병렬 진행되고, T066(REST 계약)에서 접점.

## 구현 전략 (MVP 우선, 백엔드 선행)
1. **백엔드 MVP = US3+US2+US1(BE)** — API로 "가입→상품→구매·결제" 검증.
2. US4·US5(BE)로 이행·조회 보강 → 백엔드 Polish + REST 계약 확정.
3. 외부 디자인 완료 시 **Part B(프론트 인스턴스)** 착수, 화면을 REST에 연결.
4. 결제·재고·상태전이는 통합 테스트(Testcontainers)로 결정적 검증(원칙 I).
