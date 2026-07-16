# ShopFlow

멀티 판매자(마켓플레이스) 이커머스 쇼핑몰. 여러 판매자가 입점해 상품을 팔고, 구매자는 한 번의
주문으로 여러 판매자 상품을 담아 결제한다. 배송·정산·환불은 판매자별로 관리한다.

## 핵심 가치

- **결제·보안 신뢰성** — 금액은 서버에서 검증, 카드 정보는 저장하지 않고 PSP(결제대행사)에 위임
- **정확히 한 번의 결제** — 멱등키로 재시도가 겹쳐도 결제는 한 번만 발생
- **재고 선점** — 결제 전 재고를 선점(TTL 만료 시 자동 해제)해 초과 판매 방지
- **판매자별 정산** — 배송 완료 시 수수료를 공제해 정산, 환불 시 정산 회수(claw-back)
- **테스트 우선 품질** — 커머스 핵심 흐름은 TDD로 검증

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 백엔드 | Java + Spring Boot (Spring MVC, Spring Data, Spring Security) |
| 화면 | 서버 사이드 템플릿 엔진 (Thymeleaf) |
| 데이터 | 관계형 데이터베이스 + 버전 관리 마이그레이션 |
| 결제 | 외부 PSP 연동 (자체 카드 저장 없음) |

## 주요 기능 (계획)

- 상품 카탈로그 · 검색
- 장바구니 · 주문 · 결제(checkout)
- 재고 선점 및 확정 차감
- 판매자별 배송 처리
- 배송 완료 기반 정산 · 수수료 공제
- 부분/전체 환불 및 정산 회수

## 개발 원칙

이 프로젝트의 모든 설계·구현은 [프로젝트 헌장](.specify/memory/constitution.md)을 따른다.
헌장은 테스트 우선, 결제·보안, 데이터 무결성, 관측 가능성, 단순성, 한글 표현, 마켓플레이스
정산 등 7개 핵심 원칙을 정의한다.

개발은 [Spec Kit](https://github.com/github/spec-kit) 워크플로로 진행한다:
`헌장(constitution) → 사양(specify) → 계획(plan) → 태스크(tasks) → 구현(implement)`

## 로컬 실행 (백엔드)

포트·DB·시크릿 등 환경별 값은 환경변수로 외부화하며 소스에 하드코딩하지 않는다(dev-ports).

```bash
# 0) 포트 슬롯/환경변수 셋업 (최초 1회)
dev-init                                   # .envrc(use_dev_ports) + direnv allow
direnv exec . env | grep -E 'BACKEND_PORT|POSTGRES_PORT'

# 1) PostgreSQL 기동
direnv exec . docker compose up -d postgres

# 2) 백엔드 실행 (Maven Wrapper). mvnw 없으면: mvn -N wrapper:wrapper
direnv exec . ./mvnw spring-boot:run       # http://localhost:${BACKEND_PORT:-18000}

# 3) 테스트 (Testcontainers가 PostgreSQL 자동 기동 — Docker 필요)
direnv exec . ./mvnw test
```

- 백엔드 REST 리소스는 `/api` 하위(ADR-0011). 프론트엔드는 외부 디자인 완료 후 별도 인스턴스로
  이 REST를 소비한다(ADR-0010 v2).
- 스키마는 Flyway(`src/main/resources/db/migration`)로 관리하며, JPA는 `ddl-auto=validate`.

## 라이선스

미정 (TBD)
