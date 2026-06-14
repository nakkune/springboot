-- =============================================================
-- ProjectOS — ERP HR Module Schema
-- 대상: PostgreSQL
-- 주의: 이 SQL은 별도로 실행해야 합니다. (Schema.sql에 병합 예정)
-- =============================================================

-- =============================================================
-- 1. 부서 (조직도 트리)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_departments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    parent_id       UUID REFERENCES erp_departments(id) ON DELETE SET NULL,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(50),
    manager_id      UUID,  -- erp_employees 생성 후 ALTER로 FK 추가
    sort_order      INTEGER DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 2. 직원 마스터
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_employees (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    employee_no     VARCHAR(50) NOT NULL,
    department_id   UUID REFERENCES erp_departments(id) ON DELETE SET NULL,
    position        VARCHAR(50),
    job_title       VARCHAR(100),
    employment_type VARCHAR(30) DEFAULT 'full_time',
    hire_date       DATE NOT NULL,
    resignation_date DATE,
    status          VARCHAR(20) DEFAULT 'active',
    phone           VARCHAR(50),
    email           VARCHAR(255),
    emergency_contact   VARCHAR(100),
    emergency_phone     VARCHAR(50),
    bank_name       VARCHAR(100),
    bank_account    VARCHAR(100),
    annual_leave_days  NUMERIC(4,1) DEFAULT 15,
    memo            TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- erp_departments.manager_id FK (erp_employees 생성 후)
ALTER TABLE erp_departments
    ADD CONSTRAINT fk_dept_manager
    FOREIGN KEY (manager_id) REFERENCES erp_employees(id) ON DELETE SET NULL;

-- =============================================================
-- 인덱스
-- =============================================================
CREATE INDEX IF NOT EXISTS idx_erp_dept_workspace ON erp_departments(workspace_id);
CREATE INDEX IF NOT EXISTS idx_erp_dept_parent   ON erp_departments(parent_id);

CREATE INDEX IF NOT EXISTS idx_erp_emp_workspace   ON erp_employees(workspace_id);
CREATE INDEX IF NOT EXISTS idx_erp_emp_department  ON erp_employees(department_id);
CREATE INDEX IF NOT EXISTS idx_erp_emp_user        ON erp_employees(user_id);
CREATE INDEX IF NOT EXISTS idx_erp_emp_status      ON erp_employees(status);
CREATE INDEX IF NOT EXISTS idx_erp_emp_no          ON erp_employees(employee_no);

-- =============================================================
-- 사번 자동 채번 시퀀스 (workspace별)
-- =============================================================
CREATE SEQUENCE IF NOT EXISTS seq_employee_no START 1 INCREMENT 1;

-- =============================================================
-- updated_at 트리거
-- =============================================================
CREATE TRIGGER trg_erp_departments_updated
    BEFORE UPDATE ON erp_departments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_erp_employees_updated
    BEFORE UPDATE ON erp_employees
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- COMMENTS
-- =============================================================
COMMENT ON TABLE  erp_departments      IS 'ERP 부서/조직도. 트리 구조로 계층형 조직 표현';
COMMENT ON COLUMN erp_departments.id         IS '부서 고유 식별자';
COMMENT ON COLUMN erp_departments.workspace_id IS '소속 워크스페이스 ID';
COMMENT ON COLUMN erp_departments.parent_id  IS '상위 부서 ID (NULL=최상위)';
COMMENT ON COLUMN erp_departments.name       IS '부서명 (예: 개발팀, 마케팅팀)';
COMMENT ON COLUMN erp_departments.code       IS '부서 코드 (예: DEV-01)';
COMMENT ON COLUMN erp_departments.manager_id IS '부서장 직원 ID';
COMMENT ON COLUMN erp_departments.sort_order IS '같은 레벨 내 정렬 순서';
COMMENT ON COLUMN erp_departments.is_active  IS '활성 여부. FALSE=해체/통합된 부서';

COMMENT ON TABLE  erp_employees        IS 'ERP 직원 마스터. users 테이블과 1:1 연결 가능';
COMMENT ON COLUMN erp_employees.id            IS '직원 고유 식별자';
COMMENT ON COLUMN erp_employees.workspace_id  IS '소속 워크스페이스 ID';
COMMENT ON COLUMN erp_employees.user_id       IS '연결된 PM 사용자 ID (NULL=인사만 등록)';
COMMENT ON COLUMN erp_employees.employee_no   IS '사번 (워크스페이스 내 유니크)';
COMMENT ON COLUMN erp_employees.department_id IS '소속 부서 ID';
COMMENT ON COLUMN erp_employees.position      IS '직급 (사원/대리/과장/차장/부장)';
COMMENT ON COLUMN erp_employees.job_title     IS '직책 (팀장/파트장)';
COMMENT ON COLUMN erp_employees.employment_type IS '고용 형태 (full_time/contract/intern)';
COMMENT ON COLUMN erp_employees.hire_date      IS '입사일';
COMMENT ON COLUMN erp_employees.resignation_date IS '퇴사일';
COMMENT ON COLUMN erp_employees.status        IS '재직 상태 (active/leave/resigned)';
COMMENT ON COLUMN erp_employees.annual_leave_days IS '연간 연차 일수';
COMMENT ON COLUMN erp_employees.leave_start_date IS '휴직일';
COMMENT ON COLUMN erp_employees.leave_end_date   IS '휴직복직일 (복직예정일)';
COMMENT ON COLUMN erp_employees.phone            IS '전화번호';
COMMENT ON COLUMN erp_employees.email            IS '이메일 주소';
COMMENT ON COLUMN erp_employees.emergency_contact IS '비상 연락망 이름';
COMMENT ON COLUMN erp_employees.emergency_phone   IS '비상 연락망 전화번호';
COMMENT ON COLUMN erp_employees.bank_name         IS '급여 계좌 은행명';
COMMENT ON COLUMN erp_employees.bank_account      IS '급여 계좌 번호';
COMMENT ON COLUMN erp_employees.memo              IS '기타 인사 메모';
COMMENT ON COLUMN erp_employees.created_by        IS '등록자 ID (users.id)';
COMMENT ON COLUMN erp_employees.created_at        IS '생성 일시';
COMMENT ON COLUMN erp_employees.updated_at        IS '최근 수정 일시';
