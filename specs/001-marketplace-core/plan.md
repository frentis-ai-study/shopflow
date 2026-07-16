# 구현 계획: 멀티셀러 핵심 커머스 (구매·판매)

**Branch**: `feature/2-marketplace-core` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)

**입력**: `specs/001-marketplace-core/spec.md` (관련 이슈 #2)

**참고 산출물**: [usecases.md](./usecases.md), [diagrams/](./diagrams/), [ADR](../../docs/adr/README.md)

## 요약 (Summary)

멀티셀러 마켓플레이스의 핵심 거래 루프(가입·상품 등록/재고·탐색·장바구니·결제·판매자별
이행)를 Spring Boot 서버사이드 렌더링 웹 애플리케이션으로 구현한다. 결제는 어댑터 뒤의 모의
게이트웨이로 처리하고, 초과 판매 방지를 위해 결제 전 재고 선점(TTL 10분)과 멱등 결제(정확히
한 번)를 트랜잭션으로 보장한다. 상세 결정은 [ADR-0001~0007](../../docs/adr/README.md) 참조.

## 기술 컨텍스트 (Technical Context)

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.3.x — Spring Data REST(백엔드 REST 리포지토리 +
커스텀 REST 컨트롤러), Spring Data JPA, Spring MVC + Thymeleaf(프론트 제어 레이어), Spring
Security, Bean Validation(Hibernate Validator), Flyway(마이그레이션). 전달 계층 구성은 ADR-0011.

**Storage**: PostgreSQL 16 (관계형, 헌장 원칙 III). 로컬 포트는 `POSTGRES_PORT`(기본 슬롯
0 → 15432) 환경변수 사용.

**Testing**: JUnit 5 + Spring Boot Test(웹/슬라이스), Testcontainers(PostgreSQL 통합 테스트).
순수 도메인 로직은 프레임워크 비의존 단위 테스트(헌장 원칙 I).

**Target Platform**: JVM / Linux 서버. 로컬 개발은 `BACKEND_PORT`(기본 18000) 환경변수.

**Project Type**: 웹 애플리케이션. **백엔드 = 단일 배포 모듈러 모놀리스**(컨텍스트별 인스턴스
분리는 Non-goal, ADR-0010 v2). 단일 DB·단일 트랜잭션으로 코어 무결성 보장. **프론트엔드는 별도
인스턴스**로, 외부 디자인 완료 후 백엔드 REST를 소비(ADR-0011). 개발 순서: 백엔드 서비스 우선 →
프론트 후행.

**Performance Goals**: MVP 기준 일반 웹 응답성. 결제·재고 경합 경로에서 초과 판매 0(정확성
우선). 구체 목표: 상품 목록/상세 P95 < 1s(일반 부하), 결제 처리 정확성 100%.

**Constraints**: 카드 원본 미저장(PCI-DSS 범위 최소화). 금액은 정수 원(KRW)로 저장. 시크릿·
포트는 환경변수 외부화. 단일 통화(원)·한글 UI.

**Scale/Scope**: MVP. 사용자 스토리 5개, 유즈케이스 12개, 화면 약 10여 개. 소규모 동시
사용자 가정(한정 재고 경합 정확성은 규모와 무관하게 보장).

**미해결 → research.md에서 확정**:
- 재고 선점의 동시성 제어 방식(원자적 조건부 UPDATE vs 비관적 락 vs 낙관적 락)
- 재고 선점 만료(TTL) 처리 메커니즘(스케줄 스윕 vs 지연 해제)
- 멱등키 발급·저장·조회 방식
- 주문/하위주문 상태 기계 표현
- 결제 어댑터 인터페이스 계약

## 헌장 준수 점검 (Constitution Check)

*GATE: Phase 0 이전 통과 필수, Phase 1 이후 재점검.*

| 원칙 | 계획 반영 | 상태 |
|---|---|---|
| I. 테스트 우선 | 도메인 순수 로직 단위 테스트 + 통합 테스트(Testcontainers). 커머스 핵심 흐름 TDD | ✅ |
| II. 결제·보안 | 서버측 금액 검증, 카드 미저장·어댑터 위임, Spring Security 인증/인가/CSRF, 시크릿 외부화 | ✅ |
| III. 데이터 무결성 | 결제·재고·주문 트랜잭션 원자성, 정수 원 금액, 동시성 제어(research 확정) | ✅ |
| IV. 관측성 | 구조화 로깅(SLF4J/logback), 주문·결제 상태 전이 감사 기록 + correlation id | ✅ |
| V. 단순성·계층 | 단일 배포 모듈러 모놀리스(백엔드 인스턴스 분리 Non-goal, ADR-0010 v2). 프론트 제어 MVC ↔ 백엔드 REST 분리(ADR-0011)는 단순성 대비 추가 복잡도 → Complexity Tracking에 기재 | ⚠ (deviation 기재) |
| VI. 한글 산출물 | 모든 설계·문서·주석·UI 한글 | ✅ |
| VII. 마켓플레이스 | 판매자별 하위주문 분리, 재고 선점, 멱등 결제 | ✅ |
| VIII. 워크플로 | 이슈 #2 → feature 브랜치 → PR 예정 | ✅ |

**게이트 결과**: 원칙 위반은 없으나, 프론트/백 분리로 인한 추가 복잡도를 Complexity Tracking에
기재한다(거버넌스 준수).

## 프로젝트 구조 (Project Structure)

### 문서 (이 기능)

```text
specs/001-marketplace-core/
├── plan.md              # 이 파일
├── research.md          # Phase 0 산출물
├── data-model.md        # Phase 1 산출물
├── quickstart.md        # Phase 1 산출물
├── contracts/           # Phase 1 산출물(내부 인터페이스 계약)
├── usecases.md          # 유즈케이스 명세
├── diagrams/            # 유즈케이스·비즈니스 프로세스 다이어그램
└── tasks.md             # Phase 2(/speckit-tasks) 산출물 — 이 명령은 만들지 않음
```

### 소스 코드 (저장소 루트)

단일 Spring Boot 프로젝트(모듈러 모놀리스). 전달 계층은 프론트 제어 MVC 모듈과 백엔드 REST
모듈로 분리(ADR-0011). 백엔드 각 컨텍스트는 `rest`(Spring Data REST 리포지토리 + 커스텀 REST
컨트롤러)·`domain`·`repository` 로 구성.

```text
pom.xml                                 # Maven 빌드
src/main/java/com/shopflow/
├── ShopFlowApplication.java
├── webmvc/           # 프론트 제어 MVC (PageControllers, RestApiClient, Thymeleaf) — BFF
│   ├── page/  client/
├── account/          # 계정 (User, Role, Seller) — rest/domain/repository
├── product/          # 상품 (Product, ProductStatus, ProductOpsController)
├── order/            # 주문 — 장바구니 포함 (Cart, Order, SubOrder, OrderLine, CheckoutController)
├── payment/          # 결제 (PaymentService, PaymentGateway, MockPaymentGateway, Idempotency)
├── inventory/        # 재고 (StockReservation, ReservationService, ExpirySweeper)
├── delivery/         # 배송 (Delivery, DeliveryStatus, DeliveryOpsController)
└── common/           # 공통(금액 타입, 감사 로깅, 예외, 보안 설정)
   # 각 컨텍스트 패키지: rest/(RepositoryRestResource + 커스텀 REST) · domain/ · repository/

src/main/resources/
├── templates/        # Thymeleaf 뷰 (한글)
├── static/           # 정적 자원
├── db/migration/     # Flyway 마이그레이션 (V1__init.sql 등)
└── application.yml   # 환경변수 바인딩(포트·DB·시크릿 외부화)

src/test/java/com/shopflow/
├── unit/             # 순수 도메인 단위 테스트(가격·재고·상태전이·멱등)
├── integration/      # 흐름 통합 테스트(체크아웃·이행) — Testcontainers
└── web/              # 컨트롤러/보안 슬라이스 테스트
```

**Structure Decision**: 단일 프로젝트 웹 애플리케이션(ADR-0001). 바운디드 컨텍스트별 패키지
(account, product, order[장바구니 포함], payment, inventory, delivery, common)로 나눠 계층
(도메인/웹/리포지토리)을 각 패키지 안에 둔다. 배송은 정산(SubOrder)과 분리된 별도 컨텍스트
(Delivery)로 둔다(ADR-0008). 프론트/백 분리(Option 2)나 모바일(Option 3)은 채택하지 않는다.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 프론트 제어 MVC ↔ 백엔드 REST 분리(HTTP 홉·인증 전파 추가) | 화면 디자인이 외부에서 별도 진행 → 프론트를 별도 인스턴스로 붙이려면 백엔드가 REST 계약을 노출해야 함(ADR-0011, 0010 v2) | 순수 단일 MVC(직접 렌더)는 프론트 별도 인스턴스·외부 디자인 병렬을 못 받쳐 반려 |
| Spring Data REST 채택 | 애그리거트 조회 CRUD 표준화 | — 단, 무결성 리소스 쓰기는 커스텀 컨트롤러로 제한(자동 노출 최소화) |
