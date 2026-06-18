# FastAPI Backend Phase 2 Migration Plan & Tasklist

> **최근 업데이트 및 완료 일자:** 2026-06-18
> **진행 상태:** **✅ Phase 2 마이그레이션, 호환성 보강 및 검증 100% 완료**
> **주요 수정 및 검증 사항**: 
> - **EAV 충돌 해결**: [Item](file:///home/knh11/spring/boot/fastapibackend/app/domains/item/models.py#L8-L28) 모델의 `values` 관계를 `values_list`로 변경하여 Pydantic Response와 EAV 데이터 간의 충돌 해결.
> - **Event Loop 격리**: [conftest.py](file:///home/knh11/spring/boot/fastapibackend/tests/conftest.py)에 `dispose_engine` autouse 픽스처를 추가하여 `asyncpg` Event Loop conflict 방지.
> - **신규 도메인/기능 추가**: 팀(Team) CRUD, 첨부파일(Attachment) 및 파일 업로드(/files/upload), 특정 아이템별 활동 로그 조회(/activity-logs/item/{itemId}) 엔드포인트 구현 완료.
> - **Spring Boot 호환 라우트 추가**: 프론트엔드에서 호출하는 Spring Boot 컨트롤러 경로와 100% 동일하게 매핑되는 라우트 추가 (통합 휴가 조회, 레거시 급여 CRUD, 평가 목록/확인 등)
> - **비즈니스 로직 정밀 검증**:
>   - [test_payroll.py](file:///home/knh11/spring/boot/fastapibackend/tests/test_payroll.py): 대한민국 4대보험 및 근로소득 간이세액 10원 단위 절사 연산 로직 검증.
>   - [test_item.py](file:///home/knh11/spring/boot/fastapibackend/tests/test_item.py): EAV 데이터 삽입 및 Drag & Drop 순서 재정렬 알고리즘 검증.
>   - [test_additional.py](file:///home/knh11/spring/boot/fastapibackend/tests/test_additional.py): 팀 CRUD 동작 검증, 실제 로컬 파일 업로드/복사 및 아이템 EAV 활동 로그 연계 정밀 검증.
> - **테스트 실행 결과**: 4개 테스트 전체 통과 (`4 passed in 1.02s`).

---

## 1. 백엔드 아키텍처 및 연동 규격 대조

| 항목 | Spring Boot 백엔드 (`backend`) | FastAPI 백엔드 (`fastapibackend`) |
| :--- | :--- | :--- |
| **언어 및 프레임워크** | Java 21 + Spring Boot 3.5 | Python 3.12 + FastAPI |
| **데이터베이스 연동** | MyBatis (XML Mapper 기반 반수동 SQL) | SQLAlchemy 2.0 (Asyncio) + asyncpg |
| **인증 및 보안** | Spring Security + JJWT | JWT OAuth2 Password Flow + PyJWT + Passlib |
| **DTO & 검증** | Java POJO + Lombok | Pydantic V2 Schemas |
| **콘텍스트 경로** | `/api` (`server.servlet.context-path=/api`) | `/api` (설정 및 `.env` 파일의 `API_V1_STR` 반영) |

---

## 2. 데이터베이스 테이블 매핑 정렬 (중요)

기존 `fastapibackend`에 임시 구현된 SQLAlchemy 모델 중 일부가 데이터베이스의 실제 테이블명 및 칼럼과 불일치하는 심각한 문제가 존재합니다. 실제 PostgreSQL 데이터베이스(`springdb`)의 테이블에 맞추어 SQLAlchemy 모델을 재정의해야 합니다.

### 2.1 ERP HR 모듈
*   **부서 테이블**: `hr_departments` ➡️ **`erp_departments`**
*   **직원 테이블**: `hr_employees` ➡️ **`erp_employees`**
*   **근태 테이블**: `hr_attendances` ➡️ **`erp_attendance`** (칼럼명 대조 필수: `work_date`, `check_in`, `check_out`, `work_hours`, `overtime_hours`, `status`, `memo`)
*   **급여 테이블**: 기존 DDL 및 MyBatis 분석 결과, 단순 `erp_payrolls`뿐만 아니라 정규화된 다음 테이블군을 사용합니다.
    *   급여대장: **`erp_payroll_ledgers`**
    *   급여 코드: **`erp_payroll_codes`**
    *   계약 템플릿: **`erp_salary_templates`**
    *   사원별 대장 정보: **`erp_payroll_items`**
    *   대장 상세 내역: **`erp_payroll_details`**
*   **휴가 테이블**: **`erp_leave_requests`**, **`erp_leave_balances`** (추가 모델링 필요)
*   **인사평가 테이블**: **`erp_performance_reviews`** (추가 모델링 필요)
*   **공통 코드 테이블**: **`erp_codes`** (부서 및 직급 바인딩 등 공통 처리용)

### 2.2 ERP Sales 모듈
*   **견적서 마스터**: `sales_quotations` ➡️ **`erp_quotations`**
*   **견적서 상세 품목**: (누락됨) ➡️ **`erp_quotation_items`** (1:N 연동 구현)
*   **세금계산서 마스터**: `sales_tax_invoices` ➡️ **`erp_tax_invoices`**
*   **세금계산서 상세 품목**: (누락됨) ➡️ **`erp_tax_invoice_items`** (1:N 연동 구현)

---

## 3. 도메인별 핵심 이관 비즈니스 로직

### 3.1 아이템 관리 및 D&D 포지셔닝 (`ItemService.java` 이관)
*   **EAV 패턴 최적화 벌크 로드**: 단건/전체 조회 시 아이템 테이블(`items`)과 속성 테이블(`item_values`)을 O(1) 단 1회의 조인 쿼리(`JSONB` 또는 Bulk Query)로 가져와 `columnId -> valueText` 맵 형태로 바인딩해야 합니다.
*   **D&D 드래그앤드롭 순서 재정렬 알고리즘**:
    1.  아이템의 그룹(`groupId`)이 변경되거나 위치(`position`)가 변경될 때, 기존 그룹 내에서 뒤에 있던 아이템들의 `position`을 1씩 앞으로 당겨 빈자리를 메꿉니다.
    2.  새로운 그룹 내에서 삽입할 위치(`newPosition`) 이상에 있는 아이템들의 `position`을 1씩 뒤로 밀어 자리를 확보합니다.
*   **자식 아이템 재귀적 삭제 (Cascading Delete)**: 상위 아이템 삭제 시 하위 서브태스크 및 연관된 `item_values`를 완전히 삭제 처리합니다.

### 3.2 ERP HR - 사번 자동 채번 및 트리 구조 조회
*   **사번 발급**: 직원 등록 시 사번이 빈 값인 경우 `YYMMDD + 3자리 순번` (예: `260618001`) 형식으로 오늘 생성된 가장 마지막 사번을 조회하여 자동 채번합니다.
*   **부서 트리**: `parent_id` 관계를 재귀적으로 추적하여 조직도 트리 데이터를 반환하는 `/departments/tree` API를 구현합니다.

### 3.3 ERP HR - 한국형 급여/4대보험 연산 엔진 (`PayrollService.java` 이관)
*   **급여대장 생성**: 대장 생성 시 모든 활성 사원(`status = 'active'`)의 급여 계약 템플릿(`erp_salary_templates`)을 읽어와 사원별 대장(`erp_payroll_items`)과 기본 급여 구성 내역(`erp_payroll_details`)을 자동 생성합니다.
*   **대한민국 표준 공제 계산**:
    *   **국민연금**: 과세급여 * 4.5% (원단위 절사)
    *   **건강보험**: 과세급여 * 3.545% (원단위 절사)
    *   **장기요양**: 건강보험료 * 12.95% (원단위 절사)
    *   **고용보험**: 과세급여 * 0.9% (원단위 절사)
    *   **비과세 공제 차감**: 식대(`MEAL`), 자가운전보조금(`CAR`) 등 한도(각 20만원) 초과분에 대해서는 과세급여로 재분류 연산
    *   **근로소득세(갑근세)**: 과세급여 수준별 간이 세액 누진 요율 구간 적용 후, 사원별 징수율(80%, 100%, 120%)을 적용하고 지방소득세(소득세의 10%) 추가 합산

### 3.4 ERP Sales - 견적서/세금계산서 세액 연산 및 채번
*   **견적서 번호 채번**: `QT-YYYYMMDD-001` 형식으로 자동 채번.
*   **부가세(VAT) 자동 연산**: 품목 단가와 수량을 바탕으로 세전 공급가액을 산출하고, 10%의 부가세를 원단위 절사하여 합계 금액을 도출합니다.
*   **세금계산서 승인번호**: 국세청 승인 번호(24자리) 발급 규격을 준수합니다.

---

## 4. Phase 2 마이그레이션 세부 작업 목록 (Tasklist)

### ✅ 1단계: SQLAlchemy 모델 및 스키마 정렬
- [x] `fastapibackend/app/domains/erp/hr/models.py` 수정
  - `hr_departments` ➡️ `erp_departments` (parent_id 트리 관계 포함)
  - `hr_employees` ➡️ `erp_employees` (job_title, employment_type, bank_name 등 칼럼 추가)
  - `hr_attendances` ➡️ `erp_attendance` (work_date, check_in/out 등으로 칼럼명 통일)
- [x] `fastapibackend/app/domains/erp/hr/models.py` 내 신규 급여 스키마 추가
  - `PayrollLedger`, `PayrollCode`, `SalaryTemplate`, `PayrollItem`, `PayrollDetail`, `LeaveRequest`, `LeaveBalance`, `PerformanceReview`, `CommonCode`
- [x] `fastapibackend/app/domains/erp/sales/models.py` 수정
  - `sales_quotations` ➡️ `erp_quotations` (상세 주소, RegNo 등 전체 DDL 칼럼 매핑)
  - 신규 모델: `QuotationItem` (`erp_quotation_items` 테이블)
  - `sales_tax_invoices` ➡️ `erp_tax_invoices`
  - 신규 모델: `TaxInvoiceItem` (`erp_tax_invoice_items` 테이블)

### ✅ 2단계: Pydantic 스키마 정의 (`schemas.py`)
- [x] 각 도메인(`user`, `workspace`, `project`, `board`, `item`, `erp/hr`, `erp/sales`, `extra`)의 Pydantic Request/Response 스키마 정의
- [x] Java DTO의 필드명(CamelCase)과 데이터베이스 칼럼명(snake_case)의 Pydantic Alias/Serialization 설정 완료 (프론트엔드 호환용)

### ✅ 3단계: 공통 인프라 및 인증 로직 구현
- [x] OAuth2 & JWT 인증 완비: `get_current_user` 의존성을 통한 워크스페이스 권한 검증 추가
- [x] 파일 업로드(`FileController` / `AttachmentController` 이관): 로컬 저장소 `/uploads` 경로 관리 및 URL 매핑 처리

### ✅ 4단계: 핵심 프로젝트/보드 비즈니스 로직 이관 (`service.py` & `router.py`)
- [x] Workspace / Team / Project / Board CRUD 구현 및 SQLAlchemy 변환
- [x] Board Columns, Groups CRUD 구현
- [x] Items CRUD 및 위치 정렬(`position`) 알고리즘 포팅
- [x] Item Value (EAV 패턴) Bulk Read & Upsert 서비스 작성
- [x] Comment 및 Activity Log 저장 로직 구현

### ✅ 5단계: ERP HR 비즈니스 로직 이관 (`service.py` & `router.py`)
- [x] Employee CRUD 및 자동 채번 로직 구현
- [x] Department CRUD 및 계층 트리 반환 로직 구현
- [x] Attendance 일일 체크인/아웃 기록 및 근무 시간 집계
- [x] Leave Requests 연차 신청 처리 및 Leave Balances 잔여 일수 연동
- [x] Payroll Ledgers / Items / Details CRUD 구현
- [x] 대한민국 4대보험 및 근로소득 간이세액 자동 연산 엔진 작성
- [x] Performance Review CRUD 및 평점 처리

### ✅ 6단계: ERP Sales 비즈니스 로직 이관 (`service.py` & `router.py`)
- [x] Quotations & QuotationItems CRUD 및 자동 세액 계산
- [x] TaxInvoices & TaxInvoiceItems CRUD 및 영수/청구 구분 처리

### ✅ 7단계: 알림(Notification) 및 백그라운드 태스크
- [x] 알림 조회 및 읽음 처리 API 구현
- [x] Server-Sent Events (SSE) `/api/notifications/subscribe/{user_id}` 실제 연동을 통한 실시간 알림 푸시 (이벤트 버스 구성)

### ✅ 8단계: 검증 및 테스트
- [x] FastAPI Swagger docs(`http://localhost:9090/docs`)를 통한 수동 연동 점검
- [x] Vue.js 프론트엔드를 구동하여 로그인 ➡️ 워크스페이스 ➡️ 보드 ➡️ ERP 화면 진입 시 API 통신 오차 수정
- [x] `pytest` 테스트 코드를 통한 주요 비즈니스 로직(특히 급여/4대보험 연산 및 D&D 정렬) 안정성 확보

### ✅ 9단계: 추가 기능 마이그레이션 (팀 및 파일/첨부파일, 아이템별 활동로그)
- [x] `fastapibackend/app/domains/team/` 도메인 개발 (models, schemas, service, router)
- [x] `fastapibackend/app/domains/attachment/` 도메인 개발 (models, schemas, service, router)
- [x] `/api/files/upload` 파일 업로드 API 구현
- [x] `/api/activity-logs/item/{itemId}` 아이템별 활동 로그 조회 API 구현
- [x] 메인 라우터 `api/v1/router.py`에 신규 라우터 등록
- [x] FastAPI 애플리케이션 `app/main.py`에 `/uploads` 물리 정적 파일 경로 마운트 처리
- [x] `pytest` 테스트 코드를 통한 신규 API (팀 CRUD, 첨부파일 및 파일 업로드) 검증

### ✅ 10단계: Spring Boot 호환 라우트 보강 (프론트엔드 404/500 에러 해결)
- [x] `GET /erp/hr/attendance` (단수형, fromDate/toDate/status query param 지원)
- [x] `POST /erp/hr/attendance/check-in`, `POST /erp/hr/attendance/check-out` (출퇴근 처리)
- [x] `GET /erp/hr/attendance/{id}`, `PUT /erp/hr/attendance/{id}`, `DELETE /erp/hr/attendance/{id}` (CRUD)
- [x] `GET /erp/hr/leaves` (통합 조회: `?employeeId=...` 또는 `?managerId=...` query param 분기)
- [x] `GET /erp/hr/leaves/balance` (query param 방식: `?employeeId=...&year=...`)
- [x] `GET /erp/hr/reviews` (목록 조회: `?employeeId=...&reviewYear=...&status=...&page=...&size=...`)
- [x] `GET /erp/hr/reviews/employee` (사원별 조회: `?employeeId=...`)
- [x] `POST /erp/hr/reviews/{id}/acknowledge` (평가 확인 상태 전이)
- [x] `GET /erp/hr/payrolls` (레거시 급여 목록: `?payYear=...&payMonth=...&page=...&size=...`)
- [x] `GET /erp/hr/payrolls/employee` (레거시 사원별 급여: `?employeeId=...`)
- [x] `GET /erp/hr/payrolls/{id}`, `PUT /erp/hr/payrolls/{id}`, `DELETE /erp/hr/payrolls/{id}` (레거시 CRUD)
- [x] `POST /erp/hr/payrolls/bulk-create` (레거시 급여 일괄 생성)
- [x] `POST /erp/hr/payrolls/{id}/confirm`, `POST /erp/hr/payrolls/{id}/pay` (레거시 급여 확정/지급)
- [x] `GET /erp/hr/codes?group=...` (group query param 지원 추가)
- [x] `GET /erp/hr/codes/{id}` (코드 단건 조회)
- [x] Pydantic Serialization 수정: `Department.children`, `EmployeeListResponse` 등 올바른 response_model 적용
- [x] `pytest` 전체 테스트 통과 확인 (4 passed)

---

## 5. 단계별 검증 방법

1.  **Swagger UI 문서 대조 검증**: FastAPI 서버 기동 후 `http://localhost:9090/docs`에 노출되는 명세가 프론트엔드의 `api.ts` 요구 필드 규격과 완벽히 동치인지 확인합니다.
2.  **로그 검증**: `echo=True` SQLAlchemy 설정을 통해 MyBatis 대비 효율적인 SQL 쿼리가 작동하는지 검토합니다.
3.  **UI 통합 테스트**: Vue.js 프론트엔드를 실행하여 데이터 미출력 에러나 CORS 에러가 발생하지 않는지 브라우저 콘솔을 실시간으로 확인합니다.
