# FastAPI Backend Migration & Implementation Plan

## 1. 개요 (Overview)
기존 Spring Boot 기반의 프로젝트 관리 / ERP 백엔드(Monday.com 클론)를 Python 기반의 **FastAPI**로 마이그레이션하고 개발하기 위한 계획입니다. 기존 시스템은 매우 방대한 모듈(사용자, 워크스페이스, 프로젝트, 보드, 아이템, ERP(HR/Sales), 자동화 등)을 포함하고 있으며, PostgreSQL 데이터베이스와 MyBatis, Spring Security(JWT)를 사용하고 있습니다.

이를 FastAPI 기반의 최신 Python 백엔드 아키텍처로 안전하고 효율적으로 전환하는 것을 목표로 합니다.

## 2. 사용자 검토 필요 (User Review Required)

- **ORM 선택:** 기존에는 SQL 중심의 MyBatis를 사용했습니다. FastAPI에서는 Python 객체 중심의 `SQLAlchemy (Async)` 또는 `SQLModel` 사용을 강력히 권장합니다. 복잡한 동적 쿼리가 많은 ERP/Board 특성상 `SQLAlchemy 2.0 (Async)` 사용을 제안합니다. 동의하시나요?
- **단계별 마이그레이션:** 시스템 규모가 방대하여 한 번에 모든 기능을 전환하는 것(Big Bang)은 위험합니다. 1단계(인증/사용자/워크스페이스), 2단계(보드/아이템/프로젝트), 3단계(ERP 및 기타)로 나누어 점진적으로 개발하는 것을 권장합니다.

## 3. 아키텍처 및 기술 스택 (Architecture & Tech Stack)

### 기존 Spring Boot 스택 -> FastAPI 스택 매핑
*   **Web Framework:** Spring Boot Web -> **FastAPI**
*   **Database ORM/Mapper:** MyBatis -> **SQLAlchemy (Asyncio) + asyncpg**
*   **Security & Auth:** Spring Security + jjwt -> **FastAPI `OAuth2PasswordBearer` + `pyjwt` + `passlib (bcrypt)`**
*   **DTO / Validation:** Lombok + Bean Validation -> **Pydantic V2**
*   **API Documentation:** Springdoc OpenAPI (Swagger) -> **FastAPI Built-in Swagger UI**
*   **Database Migration:** 없음 / 수동 SQL -> **Alembic**

### 디렉토리 구조 (Directory Structure)
FastAPI 프로젝트 (`/fastapibackend`)는 확장에 용이한 **도메인 주도(Domain-Driven)** 구조를 채택합니다.

```text
fastapibackend/
├── app/
│   ├── main.py                 # FastAPI 애플리케이션 엔트리 포인트
│   ├── core/                   # 설정, 보안, JWT 인증 로직 (security.py, config.py)
│   ├── db/                     # DB 연결 및 세션 관리, Alembic 설정 (session.py)
│   ├── api/
│   │   ├── dependencies.py     # FastAPI 의존성 주입 (get_db, get_current_user 등)
│   │   └── v1/                 # API 라우터 (버전 관리)
│   └── domains/                # 각 도메인 모듈 (Spring의 패키지 역할)
│       ├── user/               # 사용자 및 인증
│       │   ├── router.py       # (Spring: Controller)
│       │   ├── schemas.py      # (Spring: DTO) Pydantic 모델
│       │   ├── models.py       # (Spring: Entity) SQLAlchemy 모델
│       │   └── service.py      # (Spring: Service) 비즈니스 로직
│       ├── workspace/          # 워크스페이스 및 팀
│       ├── board/              # 보드 및 컬럼
│       ├── item/               # 아이템 및 댓글
│       └── erp/                # HR 및 Sales
├── alembic/                    # DB 마이그레이션 도구
├── requirements.txt            # Python 의존성 패키지
└── .env                        # 환경 변수 (DB 접속 정보 등)
```

## 4. 제안하는 구현 및 마이그레이션 단계 (Migration Phases)

### [Phase 1] 기초 인프라 및 핵심 도메인 (Foundation & Core)
*   **목표:** 프로젝트 셋업, DB 연동, 인증 및 사용자 관리 기능 구현
*   **작업 내용:**
    *   FastAPI 뼈대 생성, SQLAlchemy + asyncpg 설정, Alembic 초기화
    *   JWT 기반 로그인/회원가입 기능 (`OAuth2`) 구현
    *   `users`, `workspaces`, `workspace_members` 테이블 모델링 및 API 구현

### [Phase 2] 업무 관리 코어 기능 (Project & Board Management)
*   **목표:** 시스템의 핵심인 프로젝트와 보드, 태스크(아이템) 기능 구현
*   **작업 내용:**
    *   `projects`, `boards`, `board_columns`, `board_groups`, `items`, `item_values` 모델 및 관계(Relationship) 설정
    *   아이템 생성, 컬럼 동적 값(EAV 패턴) 처리 로직 이관 (MyBatis의 `JsonbTypeHandler` -> SQLAlchemy `JSONB` 타입 활용)
    *   `comments` (댓글) 및 파일 첨부 메타데이터 관리

### [Phase 3] ERP 모듈 및 부가 기능 (ERP, Automation & Extra)
*   **목표:** 인사/급여(HR), 영업(Sales) 및 자동화 기능 구현
*   **작업 내용:**
    *   ERP 관련 엔티티 (Department, Employee, Payroll, TaxInvoice 등) 구현
    *   `activity_logs`, `notifications`, `automations` 백그라운드 처리 로직 설계

## 5. 검증 계획 (Verification Plan)
1. **Automated Tests:** `pytest`와 `httpx`를 이용한 통합 테스트 셋업 (Spring Boot의 `spring-boot-starter-test` 대체).
2. **API 동등성 검증:** 기존 Spring Boot Swagger 문서와 생성된 FastAPI Swagger 문서를 비교하여 엔드포인트 및 DTO 구조 일치 여부 확인.
3. **DB 호환성:** 기존 `springdb` PostgreSQL 데이터베이스를 그대로 바라보게 하여 스키마 변경 없이 CRUD가 정상 동작하는지 테스트.
