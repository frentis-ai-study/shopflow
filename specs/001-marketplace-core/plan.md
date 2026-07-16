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

**Primary Dependencies**: Spring Boot 3.3.x — Spring MVC, Spring Data JPA, Spring Security,
Thymeleaf, Bean Validation(Hibernate Validator), Flyway(마이그레이션)

**Storage**: PostgreSQL 16 (관계형, 헌장 원칙 III). 로컬 포트는 `POSTGRES_PORT`(기본 슬롯
0 → 15432) 환경변수 사용.

**Testing**: JUnit 5 + Spring Boot Test(웹/슬라이스), Testcontainers(PostgreSQL 통합 테스트).
순수 도메인 로직은 프레임워크 비의존 단위 테스트(헌장 원칙 I).

**Target Platform**: JVM / Linux 서버. 로컬 개발은 `BACKEND_PORT`(기본 18000) 환경변수.

**Project Type**: 웹 애플리케이션(서버사이드 렌더링 모놀리스, 단일 프로젝트)

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
- 주문/이행 단위 상태 기계 표현
- 결제 어댑터 인터페이스 계약

## 헌장 준수 점검 (Constitution Check)

*GATE: Phase 0 이전 통과 필수, Phase 1 이후 재점검.*

| 원칙 | 계획 반영 | 상태 |
|---|---|---|
| I. 테스트 우선 | 도메인 순수 로직 단위 테스트 + 통합 테스트(Testcontainers). 커머스 핵심 흐름 TDD | ✅ |
| II. 결제·보안 | 서버측 금액 검증, 카드 미저장·어댑터 위임, Spring Security 인증/인가/CSRF, 시크릿 외부화 | ✅ |
| III. 데이터 무결성 | 결제·재고·주문 트랜잭션 원자성, 정수 원 금액, 동시성 제어(research 확정) | ✅ |
| IV. 관측성 | 구조화 로깅(SLF4J/logback), 주문·결제 상태 전이 감사 기록 + correlation id | ✅ |
| V. 단순성·계층 | 단일 모놀리스, controller→service→repository, 뷰 로직 최소화 | ✅ |
| VI. 한글 산출물 | 모든 설계·문서·주석·UI 한글 | ✅ |
| VII. 마켓플레이스 | 판매자별 이행 단위 분리, 재고 선점, 멱등 결제 | ✅ |
| VIII. 워크플로 | 이슈 #2 → feature 브랜치 → PR 예정 | ✅ |

**게이트 결과**: 위반 없음 → Complexity Tracking 비움.

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

단일 Spring Boot 프로젝트(모놀리스). 도메인 중심 패키지 구조.

```text
build.gradle / settings.gradle          # Gradle 빌드
src/main/java/com/shopflow/
├── ShopFlowApplication.java
├── account/          # 계정·인증 (User, 역할)
│   ├── domain/  web/  repository/
├── catalog/          # 상품·판매자 (Product, Seller)
│   ├── domain/  web/  repository/
├── cart/             # 장바구니 (Cart, CartItem)
│   ├── domain/  web/  repository/
├── order/            # 주문·이행 단위·상태 기계 (Order, Fulfillment)
│   ├── domain/  web/  repository/
├── payment/          # 결제 어댑터·멱등 (PaymentGateway, MockPaymentGateway, Idempotency)
│   ├── domain/  adapter/  repository/
├── inventory/        # 재고 선점·확정 차감·TTL 스윕 (StockReservation)
│   ├── domain/  repository/  scheduler/
└── common/           # 공통(금액 타입, 감사 로깅, 예외, 보안 설정)

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

**Structure Decision**: 단일 프로젝트 웹 애플리케이션(ADR-0001). 도메인별 패키지(account,
catalog, cart, order, payment, inventory, common)로 나눠 계층(도메인/웹/리포지토리)을 각
패키지 안에 둔다. 프론트/백 분리(Option 2)나 모바일(Option 3)은 채택하지 않는다.

## Complexity Tracking

> 헌장 위반이 없으므로 비움.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (없음) | — | — |
