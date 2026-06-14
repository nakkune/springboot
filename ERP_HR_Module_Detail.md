# ProjectOS — 인사/급여(HR) 모듈 상세 설계

> **우선 순위**: Phase 1 (CRM/구매 대신 HR부터 진행)
> **목표**: 기존 PM 시스템 위에 직원 관리, 근태, 연차, 급여, 평가 기능 통합

---

## 1. 전체 구성도

```
workspaces (기존)
  └── erp_departments (부서/조직도)
  └── erp_employees (직원 - users와 1:1 연결)
        ├── erp_attendance (출퇴근)
        ├── erp_leave_requests (연차/휴가)
        ├── erp_payrolls (급여)
        └── erp_performance_reviews (인사평가)
```

기존 PM 시스템과의 관계:
- `users` = 플랫폼 계정 (로그인)
- `erp_employees` = 인사 기록 (users의 확장, JWT와 무관)
- `workspace_members` = 워크스페이스 접근 권한
- `teams` / `team_members` = 프로젝트 팀 (부서와 별도)

---

## 2. DB 스키마 상세

### 2.1 부서 관리

```sql
CREATE TABLE erp_departments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    parent_id       UUID REFERENCES erp_departments(id) ON DELETE SET NULL, -- 트리 구조
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(50),                              -- 부서 코드 (예: DEV-01)
    manager_id      UUID REFERENCES erp_employees(id) ON DELETE SET NULL,
    sort_order      INTEGER DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### 2.2 직원 마스터

```sql
CREATE TABLE erp_employees (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL, -- PM 계정 (NULL=인사만 등록)
    employee_no     VARCHAR(50) NOT NULL,                       -- 사번 (자동 채번)
    department_id   UUID REFERENCES erp_departments(id) ON DELETE SET NULL,
    position        VARCHAR(50),                                -- 직급 (사원/대리/과장/차장/부장)
    job_title       VARCHAR(100),                               -- 직책 (팀장/파트장)
    employment_type VARCHAR(30) DEFAULT 'full_time',            -- 'full_time' | 'contract' | 'intern'
    hire_date       DATE NOT NULL,
    resignation_date DATE,
    status          VARCHAR(20) DEFAULT 'active',               -- 'active' | 'leave' | 'resigned'
    phone           VARCHAR(50),
    email           VARCHAR(255),
    emergency_contact  VARCHAR(100),
    emergency_phone    VARCHAR(50),
    bank_name       VARCHAR(100),
    bank_account    VARCHAR(100),
    annual_leave_days  NUMERIC(4,1) DEFAULT 15,                -- 연간 연차 일수
    memo            TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_erp_emp_workspace ON erp_employees(workspace_id);
CREATE INDEX idx_erp_emp_department ON erp_employees(department_id);
CREATE INDEX idx_erp_emp_user ON erp_employees(user_id);
CREATE INDEX idx_erp_emp_status ON erp_employees(status);
```

### 2.3 근태 관리

```sql
CREATE TABLE erp_attendance (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    work_date       DATE NOT NULL,
    check_in        TIMESTAMPTZ,                                -- 출근 시간
    check_out       TIMESTAMPTZ,                                -- 퇴근 시간
    work_hours      NUMERIC(4,1) DEFAULT 0,                     -- 실제 근무 시간
    overtime_hours  NUMERIC(4,1) DEFAULT 0,                     -- 연장 근무 시간
    status          VARCHAR(20) DEFAULT 'present',              -- 'present' | 'late' | 'early_leave' | 'absent'
    memo            TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, work_date)
);

CREATE INDEX idx_erp_att_employee ON erp_attendance(employee_id);
CREATE INDEX idx_erp_att_date ON erp_attendance(work_date);
```

### 2.4 연차/휴가 관리

```sql
CREATE TABLE erp_leave_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    leave_type      VARCHAR(30) NOT NULL,                       -- 'annual' | 'sick' | 'personal' | 'maternity' | 'etc'
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    total_days      NUMERIC(4,1) NOT NULL,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) DEFAULT 'pending',              -- 'pending' | 'approved' | 'rejected' | 'cancelled'
    approver_id     UUID REFERENCES erp_employees(id) ON DELETE SET NULL,
    approved_at     TIMESTAMPTZ,
    reject_reason   TEXT,
    linked_item_id  UUID REFERENCES items(id) ON DELETE SET NULL, -- PM 보드 아이템 연동
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_erp_leave_employee ON erp_leave_requests(employee_id);
CREATE INDEX idx_erp_leave_status ON erp_leave_requests(status);
CREATE INDEX idx_erp_leave_dates ON erp_leave_requests(start_date, end_date);

-- 연차 사용 내역 (잔여 일수 계산용)
CREATE TABLE erp_leave_balances (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    year            INTEGER NOT NULL,
    total_days      NUMERIC(4,1) NOT NULL DEFAULT 15,           -- 총 부여 일수
    used_days       NUMERIC(4,1) DEFAULT 0,                     -- 사용 일수
    remaining_days  NUMERIC(4,1) DEFAULT 15,                    -- 잔여 일수
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, year)
);
```

### 2.5 급여 관리

```sql
CREATE TABLE erp_payrolls (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    pay_year        INTEGER NOT NULL,
    pay_month       INTEGER NOT NULL,                           -- 1-12
    pay_date        DATE NOT NULL,
    -- 급여 구성 항목
    base_pay        NUMERIC(12,2) DEFAULT 0,                   -- 기본급
    position_pay    NUMERIC(12,2) DEFAULT 0,                   -- 직급 수당
    overtime_pay    NUMERIC(12,2) DEFAULT 0,                   -- 연장 수당
    bonus_pay       NUMERIC(12,2) DEFAULT 0,                   -- 상여
    meal_allowance  NUMERIC(12,2) DEFAULT 0,                   -- 식대
    transportation  NUMERIC(12,2) DEFAULT 0,                   -- 교통비
    -- 공제 항목
    income_tax      NUMERIC(12,2) DEFAULT 0,                   -- 소득세
    local_tax       NUMERIC(12,2) DEFAULT 0,                   -- 지방소득세
    national_pension NUMERIC(12,2) DEFAULT 0,                  -- 국민연금
    health_insurance NUMERIC(12,2) DEFAULT 0,                  -- 건강보험
    employment_insurance NUMERIC(12,2) DEFAULT 0,              -- 고용보험
    longterm_care   NUMERIC(12,2) DEFAULT 0,                   -- 장기요양보험
    -- 합계
    gross_pay       NUMERIC(12,2) DEFAULT 0,                   -- 총지급액
    total_deduction NUMERIC(12,2) DEFAULT 0,                   -- 총공제액
    net_pay         NUMERIC(12,2) DEFAULT 0,                   -- 실지급액
    -- 상태
    status          VARCHAR(20) DEFAULT 'draft',                -- 'draft' | 'confirmed' | 'paid'
    confirmed_by    UUID REFERENCES users(id),
    confirmed_at    TIMESTAMPTZ,
    memo            TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, pay_year, pay_month)
);

CREATE INDEX idx_erp_payroll_workspace ON erp_payrolls(workspace_id);
CREATE INDEX idx_erp_payroll_employee ON erp_payrolls(employee_id);
CREATE INDEX idx_erp_payroll_period ON erp_payrolls(pay_year, pay_month);
```

### 2.6 인사 평가

```sql
CREATE TABLE erp_performance_reviews (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    reviewer_id     UUID NOT NULL REFERENCES erp_employees(id),  -- 평가자
    review_year     INTEGER NOT NULL,
    review_period   VARCHAR(20) NOT NULL DEFAULT 'annual',      -- 'annual' | 'half' | 'quarter'
    -- 평가 항목 (JSON으로 유연하게)
    ratings         JSONB NOT NULL DEFAULT '{}',
    -- 예: { "task_performance": 4.5, "collaboration": 4.0, "leadership": 3.5, "attitude": 4.0 }
    total_score     NUMERIC(4,2),
    comment         TEXT,
    status          VARCHAR(20) DEFAULT 'draft',               -- 'draft' | 'submitted' | 'acknowledged'
    submitted_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_erp_review_employee ON erp_performance_reviews(employee_id);
CREATE INDEX idx_erp_review_period ON erp_performance_reviews(review_year, review_period);
```

---

## 3. Backend 구현

### 3.1 패키지 구조

```
com.nak.backend.erp.hr/
├── controller/
│   ├── DepartmentController.java    (부서 CRUD)
│   ├── EmployeeController.java      (직원 CRUD)
│   ├── AttendanceController.java    (출퇴근)
│   ├── LeaveController.java         (연차/휴가 신청/승인)
│   ├── PayrollController.java       (급여)
│   └── PerformanceController.java   (인사평가)
├── dto/
│   ├── DepartmentDto.java
│   ├── EmployeeDto.java
│   ├── AttendanceDto.java
│   ├── LeaveRequestDto.java
│   ├── LeaveBalanceDto.java
│   ├── PayrollDto.java
│   └── PerformanceReviewDto.java
├── mapper/ (XML은 resources/mapper/erp/hr/ 에 위치)
└── service/
    ├── DepartmentService.java
    ├── EmployeeService.java
    ├── AttendanceService.java
    ├── LeaveService.java
    ├── PayrollService.java
    └── PerformanceService.java
```

### 3.2 API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/erp/hr/departments?workspaceId=` | 부서 목록 (트리) |
| POST | `/api/erp/hr/departments` | 부서 생성 |
| PUT | `/api/erp/hr/departments/:id` | 부서 수정 |
| DELETE | `/api/erp/hr/departments/:id` | 부서 삭제 |
| GET | `/api/erp/hr/employees?workspaceId=&departmentId=&status=` | 직원 목록 (페이지네이션) |
| GET | `/api/erp/hr/employees/:id` | 직원 상세 |
| POST | `/api/erp/hr/employees` | 직원 등록 |
| PUT | `/api/erp/hr/employees/:id` | 직원 정보 수정 |
| PATCH | `/api/erp/hr/employees/:id/status` | 재직 상태 변경 |
| GET | `/api/erp/hr/attendance?employeeId=&from=&to=` | 근태 조회 |
| POST | `/api/erp/hr/attendance/check-in` | 출근 |
| POST | `/api/erp/hr/attendance/check-out` | 퇴근 |
| PUT | `/api/erp/hr/attendance/:id` | 근태 수정 (관리자) |
| GET | `/api/erp/hr/leaves?employeeId=&status=` | 연차 신청 목록 |
| POST | `/api/erp/hr/leaves` | 연차 신청 |
| PUT | `/api/erp/hr/leaves/:id/approve` | 연차 승인/반려 |
| GET | `/api/erp/hr/leaves/balance?employeeId=&year=` | 연차 잔여 조회 |
| GET | `/api/erp/hr/payrolls?workspaceId=&year=&month=` | 급여 명세서 목록 |
| POST | `/api/erp/hr/payrolls/bulk-create` | 급여 일괄 생성 |
| PUT | `/api/erp/hr/payrolls/:id/confirm` | 급여 확정 |
| GET | `/api/erp/hr/reviews?employeeId=&year=` | 평가 목록 |
| POST | `/api/erp/hr/reviews` | 평가 등록 |
| PUT | `/api/erp/hr/reviews/:id` | 평가 수정 |

### 3.3 컨트롤러 예시 (기존 패턴과 동일)

```java
@RestController
@RequestMapping("/erp/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getEmployees(
        @RequestParam String workspaceId,
        @RequestParam(required = false) String departmentId,
        @RequestParam(defaultValue = "active") String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(employeeService.getEmployees(workspaceId, departmentId, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.createEmployee(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable String id, @Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
    }
}
```

---

## 4. Frontend 구현

### 4.1 신규 라우트

```typescript
{
  path: 'hr',   // /hr
  component: () => import('@/views/erp/hr/ErpHrLayout.vue'),
  children: [
    { path: '',               redirect: '/hr/employees' },
    { path: 'dashboard',       component: () => import('@/views/erp/hr/HrDashboard.vue') },
    { path: 'employees',       component: () => import('@/views/erp/hr/EmployeeList.vue') },
    { path: 'employees/:id',   component: () => import('@/views/erp/hr/EmployeeDetail.vue') },
    { path: 'departments',     component: () => import('@/views/erp/hr/DepartmentTree.vue') },
    { path: 'attendance',      component: () => import('@/views/erp/hr/AttendanceView.vue') },
    { path: 'leaves',          component: () => import('@/views/erp/hr/LeaveList.vue') },
    { path: 'payroll',         component: () => import('@/views/erp/hr/PayrollList.vue') },
    { path: 'reviews',         component: () => import('@/views/erp/hr/ReviewList.vue') },
  ]
}
```

### 4.2 Pinia Store

```typescript
// stores/erp/useHrStore.ts
export const useHrStore = defineStore('hr', () => {
  // 상태
  const employees = ref<Employee[]>([])
  const departments = ref<Department[]>([])
  const loading = ref(false)
  const totalCount = ref(0)

  // 액션
  async function fetchEmployees(params: EmployeeSearchParams) { ... }
  async function createEmployee(data: EmployeeCreateDto) { ... }

  // computed
  const activeEmployees = computed(() =>
    employees.value.filter(e => e.status === 'active')
  )
  const departmentTree = computed(() => buildTree(departments.value))

  return { employees, departments, loading, totalCount,
           fetchEmployees, createEmployee, activeEmployees, departmentTree }
})
```

### 4.3 화면 목록

| 화면 | 설명 | 주요 기능 |
|------|------|---------|
| **HrDashboard** | HR 대시보드 | 직원 현황, 부서별 인원, 근태 요약, 승인 대기 건수 |
| **EmployeeList** | 직원 목록 | 검색/필터(부서/직급/상태), 엑셀 내보내기, 신규 등록 |
| **EmployeeDetail** | 직원 상세 | 인적사항, 연차 현황, 근태 이력, 급여 이력, 평가 이력 |
| **DepartmentTree** | 부서/조직도 | 트리 뷰, 부서 추가/수정/이동, 구성원 보기 |
| **AttendanceView** | 근태 관리 | 일별/월별 출퇴근 기록, 지각/조퇴 현황, 수동 수정 |
| **LeaveList** | 연차/휴가 | 신청 목록, 승인/반려, 연차 잔여 일수 |
| **PayrollList** | 급여 명세서 | 월별 급여 조회, 일괄 생성, 확정/지급 처리 |
| **ReviewList** | 인사 평가 | 평가 등록, 평가자 지정, 결과 조회 |

### 4.4 재사용 컴포넌트

| 기존 컴포넌트 | HR 활용 |
|--------------|--------|
| `BoardGrid.vue` (테이블) | 직원 목록, 급여 목록 등 데이터 그리드로 재사용 |
| `ItemDetailPanel.vue` | 직원 상세 프로필 사이드 패널로 재사용 |
| `ConfirmModal.vue` | 연차 승인/반려, 급여 확정 확인 |
| `RichTextEditor.vue` | 평가 코멘트, 메모 작성 |

### 4.5 신규 컴포넌트

| 컴포넌트 | 설명 |
|---------|------|
| `HrDataTable.vue` | 직원/급여 목록 (검색 + 필터 + 페이지 + 엑셀 다운로드) |
| `DepartmentTree.vue` | 조직도 트리 (drag & drop 으로 부서 이동) |
| `AttendanceCalendar.vue` | 월별 출퇴근 달력 뷰 (CalendarView.vue 기반) |
| `PayrollSummaryCard.vue` | 급여 요약 카드 (지급액/공제액/실지급액) |
| `EmployeeSelect.vue` | 직원 검색 선택 컴포넌트 |

---

## 5. 기존 PM 시스템과 연동

### 5.1 사용자 연결

```
users (PM 로그인 계정)
  │
  ├── 연결됨: workspace_members (워크스페이스 접근 권한)
  │
  └── 연결 (선택): erp_employees.user_id
       └── 인사 정보가 있는 사용자만 HR 기능 접근 가능
```

- **인사만 등록된 직원** = `user_id`가 NULL → PM 로그인 불가, 인사 레코드만 존재
- **PM 사용자이면서 직원** = `user_id` 연결 → 프로필에서 급여명세서/연차 조회 가능

### 5.2 보드 연동

`erp_leave_requests.linked_item_id`를 통해 연차 신청을 PM 보드의 아이템으로 생성:

- 연차 신청 시 자동으로 `items` 테이블에 태스크 생성
- 승인/반려 시 아이템 status 자동 변경
- 자동화 규칙과 연계 (예: 연차 승인 시 Slack 알림)

### 5.3 활동 로그 통합

기존 `activity_logs`에 HR 액션 추가:

| Action | 설명 |
|--------|------|
| `hr.employee.create` | 직원 등록 |
| `hr.employee.update` | 직원 정보 변경 |
| `hr.employee.status_change` | 재직 상태 변경 |
| `hr.leave.create` | 연차 신청 |
| `hr.leave.approve` | 연차 승인 |
| `hr.leave.reject` | 연차 반려 |
| `hr.payroll.confirm` | 급여 확정 |
| `hr.payroll.paid` | 급여 지급 완료 |

---

## 6. 개발 일정 (3-4주)

| 주차 | 작업 내용 | 산출물 |
|------|---------|--------|
| **1주차** | DB 스키마 생성, Backend: 부서/직원 CRUD | 6개 API, 4개 테이블 |
| **2주차** | Backend: 근태/연차 CRUD + 승인 워크플로우 | 10개 API, 3개 테이블 |
| **3주차** | Backend: 급여/평가 CRUD + Frontend: 부서/직원/근태 페이지 | 8개 API + 5개 화면 |
| **4주차** | Frontend: 연차/급여/평가 페이지 + 연동 테스트 | 5개 화면 + 통합 QA |

### MVP 우선순위 (최소 2주)

1. 부서/직원 CRUD (1주)
2. 연차 신청/승인 (3일)
3. 근태 출퇴근 (2일)

---

## 7. 질문

계획을 검토하시고, 궁금한 점이나 조정할 부분이 있으면 말씀해주세요.
