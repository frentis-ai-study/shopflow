# 추적 매트릭스 (Traceability Matrix) — 멀티셀러 핵심 커머스

**기능**: [spec.md](./spec.md) · **관련 이슈**: #2 · **작성일**: 2026-07-16

요구사항(FR)·성공기준(SC)부터 사용자 스토리·유즈케이스·설계(엔티티/계약/ADR)·태스크·검증까지
양방향으로 추적하는 단일 뷰. 커버리지 점검과 변경 영향 분석의 기준(SSOT)으로 쓴다.

**참조 문서**: [spec.md](./spec.md) · [usecases.md](./usecases.md) · [data-model.md](./data-model.md)
· [contracts/](./contracts/) · [plan.md](./plan.md) · [research.md](./research.md)
· [tasks.md](./tasks.md) · [ADR](../../docs/adr/README.md)

범례: US=사용자스토리, UC=유즈케이스, FR=기능요구사항, SC=성공기준, T###=태스크.

---

## 1. 기능 요구사항(FR) → 태스크 추적

| FR | 요약 | US | UC | 핵심 엔티티/계약 | ADR | 구현 태스크 | 검증(테스트·SC) |
|---|---|---|---|---|---|---|---|
| FR-001 | 계정 생성 | US3 | UC-01 | User | — | T020, T022, T023, T025 | T017 |
| FR-002 | 이메일 형식·중복 검증 | US3 | UC-01 | User | — | T023 | T017 |
| FR-003 | 로그인/로그아웃 | US3 | UC-02 | User | — | T024, T026 | T019 |
| FR-004 | 구매자·판매자 복수 역할 | US3 | UC-01/02 | User, Role, Seller | 0009 | T020, T021, T023 | T018 |
| FR-005 | 보호 작업 인증·권한 강제 | US3(전역) | UC-02/05/09 | SecurityConfig | 0011 | T012, T024 | T019 |
| FR-006 | 상품 등록 | US2 | UC-05 | Product | — | T030, T031, T032, T034 | T028 |
| FR-007 | 상품 정보 수정 | US2 | UC-06 | Product | — | T032 | T028 |
| FR-008 | 판매중/판매중지 전환 | US2 | UC-07 | Product, ProductStatus | — | T033, T036 | T028 |
| FR-009 | 판매자 소유 자원만 접근 | US2/US4 | UC-06/07/11/12 | Product, SubOrder | — | T032, T065 | T029, T062 (SC-007) |
| FR-010 | 판매자 주문(하위주문) 조회 | US4 | UC-11 | SubOrder | 0005 | T064, T067 | T062 |
| FR-011 | 배송 상태 전이 | US4 | UC-12 | Delivery, DeliveryStatus | 0008 | T064, T065 | T061 |
| FR-012 | 판매중 상품 탐색·검색 | US2 | UC-03 | Product | — | T035, T036 | T029 |
| FR-013 | 상품 상세 조회 | US2 | UC-04 | Product | — | T035 | T029 |
| FR-014 | 장바구니 담기·수정 | US1 | UC-08 | Cart, CartItem | — | T046, T047, T048 | — |
| FR-015 | 배송지 입력·결제 진행 | US1 | UC-09 | Order, Address | — | T058, T059 | T041 |
| FR-016 | 내 주문 목록·상태 조회 | US5 | UC-10 | Order | — | T070, T071 | T068 |
| FR-017 | 재고 선점 + TTL 10분 자동 해제 | US1 | UC-09 | StockReservation | 0004 | T049, T050, T051 | T037, T045 (SC-005) |
| FR-018 | 멱등 결제(정확히 1회) | US1 | UC-09 | Payment, PaymentIdempotency | 0003 | T052, T054 | T038, T043 (SC-004) |
| FR-019 | 서버측 금액 검증 | US1 | UC-09 | CheckoutService | 0002 | T058 | T040 |
| FR-020 | 초과 판매 방지 | US1 | UC-09 | StockReservation | 0004 | T050 | T037, T042 (SC-003) |
| FR-021 | 판매자별 하위주문 분리 | US1 | UC-09 | SubOrder, OrderLine | 0005 | T055, T056 | T039 |
| FR-022 | 주문 상태·전이 감사 기록 | US1/US4 | UC-09/12 | OrderStatus, Delivery | 0008 | T056, T057, T066, T072 | T063 |
| FR-023 | 결제 시점 가격·상품 스냅샷 불변 | US1 | UC-06/09 | OrderLine | — | T056 | T039 |
| FR-024 | 카드 미저장·외부 위임(모의) | US1 | UC-09 | PaymentGateway | 0002 | T053 | T040 |
| FR-025 | 배송 전 주문 취소 이번 범위 제외 | — | — | — | — | (범위 밖) | 명시적 제외 |

## 2. 성공 기준(SC) → 검증 추적

| SC | 측정 목표 | US | 관련 FR | 검증 태스크 |
|---|---|---|---|---|
| SC-001 | 상품→결제 3분 이내 | US1 | FR-015 | T041, T059, T076 |
| SC-002 | 상품 등록 5분 이내 노출 | US2 | FR-006 | T034, T076 |
| SC-003 | 동시 결제 초과판매 0 | US1 | FR-020 | T037, T042 |
| SC-004 | 중복 요청 이중결제 0 | US1 | FR-018 | T038, T043 |
| SC-005 | 미결제 선점 TTL 후 100% 복원 | US1 | FR-017 | T045 |
| SC-006 | 조회 상태와 실제 상태 일치 | US4/US5 | FR-022 | T063, T068 |
| SC-007 | 권한 없는 접근 100% 차단 | US3/US2/US4 | FR-005/009 | T019, T029, T062 |

## 3. 사용자 스토리 → 페이즈·태스크

| US | 제목 | 우선순위 | 페이즈 | 태스크 범위 | 관련 FR |
|---|---|---|---|---|---|
| US3 | 가입·로그인 | P1 | Phase 3 | T017–T027 | FR-001~005 |
| US2 | 상품 등록·재고 | P1 | Phase 4 | T028–T036 | FR-006~009, 012, 013 |
| US1 | 구매자 결제 ⭐ | P1 | Phase 5 | T037–T060 | FR-014~024 |
| US4 | 판매자 배송 | P2 | Phase 6 | T061–T067 | FR-009~011, 022 |
| US5 | 내 주문 조회 | P2 | Phase 7 | T068–T071 | FR-016, 022 |

> Setup(T001–T006)·Foundational(T007–T016)·Polish(T072–T078)는 전 스토리 공통.

## 4. ADR → 반영 위치

| ADR | 결정 | 반영 태스크/산출물 |
|---|---|---|
| 0001 | 서버사이드 렌더링(Spring Boot+Thymeleaf) | plan, T015, webmvc/* |
| 0002 | 결제 외부 위임 + 모의 게이트웨이 | FR-019/024, T053 |
| 0003 | 멱등키 정확히 1회 | FR-018, T052, T054 |
| 0004 | 재고 선점 + TTL | FR-017/020, T049–T051 |
| 0005 | 판매자별 하위주문 분리 | FR-021, T055, T056 |
| 0006 | 금액 고정소수점 | T008(Money) |
| 0007 | 이슈→브랜치→PR 워크플로 | 이슈 #2, PR 절차 |
| 0008 | 배송 별도 컨텍스트(Delivery) | FR-011/022, T057, T064 |
| 0009 | 판매자 프로필·사업자 유형 | FR-004, T021, T023 |
| 0010 | 추출 가능 모듈러 모놀리스 | plan 구조, ERD 경계 |
| 0011 | REST 리포지토리 + 프론트 제어 MVC | plan, C4, T013–T015, *Repository/*OpsController |

## 5. 유즈케이스 → 계약(HTTP) → 태스크

| UC | HTTP 계약(대표) | 태스크 |
|---|---|---|
| UC-01 회원가입 | POST /signup | T025 |
| UC-02 로그인 | POST /login, /logout | T024, T026 |
| UC-03 탐색·검색 | GET /products?q= | T035 |
| UC-04 상세 | GET /products/{id} | T035 |
| UC-05 상품 등록 | POST /seller/products | T034 |
| UC-06 상품 수정 | POST /seller/products/{id} | T034 |
| UC-07 판매 상태 | POST /seller/products/{id}/status | T033 |
| UC-08 장바구니 | POST /cart/items | T048 |
| UC-09 결제 | GET/POST /checkout | T058, T059 |
| UC-10 내 주문 | GET /orders, /orders/{id} | T071 |
| UC-11 판매자 주문 | GET /seller/orders | T067 |
| UC-12 배송 전이 | POST /seller/orders/{subOrderId}/ship,deliver | T065 |

## 6. 커버리지 요약

- **FR 커버리지**: FR-001~024 전부 태스크로 매핑됨. FR-025는 명시적 범위 밖(취소·환불 후속).
- **SC 커버리지**: SC-001~007 전부 검증 태스크 존재.
- **UC 커버리지**: UC-01~12 전부 계약·태스크로 매핑됨.
- **미커버(의도된 범위 밖)**: 정산·수수료 공제, 환불·회수, 주문 취소 → 후속 이슈.
- **갭 점검**: 없음(현 범위 기준). 신규 요구사항 추가 시 이 표에 행을 추가하고 태스크를 연결한다.

## 7. 유지 규칙

- 요구사항·태스크·ADR이 바뀌면 이 매트릭스를 함께 갱신한다(변경 영향 분석의 기준).
- 새 FR/SC → US·UC·엔티티·ADR·태스크·검증을 채운 뒤에만 구현에 착수한다.
- `/speckit-analyze` 로 spec·plan·tasks 정합성을 교차 점검할 때 이 표를 대조 기준으로 쓴다.
