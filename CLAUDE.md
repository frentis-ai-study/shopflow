# ShopFlow — 에이전트/개발 가이드 (CLAUDE.md)

멀티셀러 이커머스 마켓플레이스. 이 문서는 코드베이스 작업 시 지켜야 할 핵심 규약을 요약한다.
상세는 `.specify/memory/constitution.md`(헌장)과 `docs/adr/`(결정 기록)을 따른다.

## 아키텍처

- **백엔드 = 단일 배포 모듈러 모놀리스**(컨텍스트별 인스턴스 분리는 Non-goal, ADR-0010 v2).
- **프론트엔드 = 별도 인스턴스**(외부 디자인 완료 후 구축). 백엔드 REST를 소비(ADR-0011).
- 스택: Java 21 · Spring Boot 3.3(Data JPA, Security, Data REST) · PostgreSQL 16 · Flyway · Maven.
- 바운디드 컨텍스트 = 패키지: `account · product · order(장바구니 포함) · payment · inventory · delivery · common`.

## 필수 규약 (헌장)

- **테스트 우선(원칙 I)**: 비즈니스 로직·커머스 흐름은 테스트 먼저. 동시성·TTL·멱등은 통합
  테스트(실 DB)로 결정적 검증. 시간 의존 로직은 `Clock` 주입.
- **결제·보안(원칙 II)**: 금액은 서버 계산·검증. 카드 미저장(PSP 위임). 시크릿·포트는 환경변수.
- **데이터 무결성(원칙 III)**: 재고/주문/결제는 트랜잭션. 금액은 정수 원(`Money`). 초과판매 0.
- **한글 산출물(원칙 VI)**: 문서·주석·오류 메시지·UI 한글.
- **워크플로(원칙 VIII)**: 이슈 → `feature/<이슈>-*` 브랜치 → PR. main 직접 커밋 금지.

## 핵심 도메인 규칙 (구현 반영)

- **재고 선점**(ADR-0004): `products.reserved`는 네이티브 조건부 UPDATE로만 변경(JPA `@Version`
  혼용 금지). 가용재고 = `stock - reserved`. DB CHECK `0<=reserved<=stock`가 최후 방어선.
- **멱등 결제**(ADR-0003): 서버 발급 멱등키 + `payment_idempotency` 유니크. 재요청은 기존 주문
  반환(이중결제 0). 실패 결제도 기록(`payments.order_id` nullable).
- **판매자별 분리**(ADR-0005): 주문 → SubOrder(판매자별 정산 단위) → OrderLine(스냅샷).
- **배송 분리**(ADR-0008): Delivery(SubOrder와 1:1) `PENDING→SHIPPING→DELIVERED`. 주문 상태는
  Delivery 집계.

## 개발·실행

```bash
dev-init                                    # 포트 슬롯 셋업(.envrc + direnv)
direnv exec . docker compose up -d postgres # PostgreSQL(POSTGRES_PORT)
direnv exec . ./mvnw spring-boot:run        # 백엔드(BACKEND_PORT)
direnv exec . ./mvnw test                   # 테스트(Testcontainers 또는 test 프로파일=실 DB)
```

- **JDK는 21 고정**. Homebrew maven이 JDK 25를 끌어올 수 있으니 `JAVA_HOME`을 21로 설정해 실행.
- 통합 테스트는 `application-test.yml`(compose PostgreSQL)에 연결(`@ActiveProfiles("test")`).
  CI에서 Docker 소켓이 잡히면 `PostgresContainerBase`(Testcontainers)로 전환 가능.

## 알려진 후속(범위 밖)

취소·환불·정산·수수료(계층 3), 영속 감사 원장·백엔드 REST 인증전파 상세(계층 2), 알림·배송추적·
리뷰·검색 고도화. 상세는 `specs/001-marketplace-core/reviews/2026-07-16-negative-review.md`.
