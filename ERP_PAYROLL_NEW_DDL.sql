-- =============================================================
-- ProjectOS — New ERP Payroll Normalization Schema
-- 대상: PostgreSQL
-- 설명: 한국형 급여대장 중심의 동적 다차원 급여 ERP 시스템 스키마 DDL
-- =============================================================

-- =============================================================
-- 1. 급여대장 마스터 테이블 (Payroll Ledgers)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_payroll_ledgers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    pay_year        INTEGER NOT NULL,
    pay_month       INTEGER NOT NULL,
    pay_date        DATE NOT NULL,
    pay_type        VARCHAR(50) NOT NULL DEFAULT '정기급여', -- 정기급여, 상여, 성과급, 연차수당
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',   -- draft (작성중), confirmed (최종확정), paid (지급완료)
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_erp_ledger_workspace ON erp_payroll_ledgers(workspace_id);
CREATE INDEX IF NOT EXISTS idx_erp_ledger_period    ON erp_payroll_ledgers(pay_year, pay_month);
CREATE INDEX IF NOT EXISTS idx_erp_ledger_status    ON erp_payroll_ledgers(status);

-- =============================================================
-- 2. 수당 및 공제 코드 설정 테이블 (Payroll Codes)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_payroll_codes (
    code            VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL,                    -- allowance (지급/수당), deduction (공제)
    is_taxable      BOOLEAN DEFAULT TRUE,                    -- 과세 여부
    tax_free_limit  NUMERIC(12,2) DEFAULT 0,                 -- 비과세 한도액 (0 이면 제한 없음)
    is_system       BOOLEAN DEFAULT FALSE,                   -- 시스템 예약 코드 여부 (4대보험 및 갑근세 등)
    is_active       BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 3. 사원별 기본 급여 계약 및 보험 기본설정 테이블 (Salary Templates)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_salary_templates (
    employee_id                 UUID PRIMARY KEY REFERENCES erp_employees(id) ON DELETE CASCADE,
    base_pay                    NUMERIC(12,2) DEFAULT 0,     -- 기본급
    position_pay                NUMERIC(12,2) DEFAULT 0,     -- 직책수당
    meal_allowance              NUMERIC(12,2) DEFAULT 200000, -- 식대 비과세 (기본 20만)
    car_allowance               NUMERIC(12,2) DEFAULT 0,      -- 자가운전 비과세
    use_national_pension        BOOLEAN DEFAULT TRUE,
    use_health_insurance        BOOLEAN DEFAULT TRUE,
    use_employment_insurance    BOOLEAN DEFAULT TRUE,
    income_tax_rate             INTEGER DEFAULT 100,         -- 소득세 징수 비율 (80, 100, 120%)
    created_at                  TIMESTAMPTZ DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 4. 급여대장 사원별 마스터 테이블 (Payroll Items)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_payroll_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ledger_id       UUID NOT NULL REFERENCES erp_payroll_ledgers(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    gross_pay       NUMERIC(12,2) DEFAULT 0,                 -- 지급 총액 (세전)
    total_deduction NUMERIC(12,2) DEFAULT 0,                 -- 공제 총액
    net_pay         NUMERIC(12,2) DEFAULT 0,                 -- 실수령액 (세후)
    bank_name       VARCHAR(100),                            -- 수령 은행
    bank_account    VARCHAR(100),                            -- 수령 계좌
    bank_owner      VARCHAR(100),                            -- 예금주
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',    -- draft, confirmed, paid
    memo            TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (ledger_id, employee_id)
);

CREATE INDEX IF NOT EXISTS idx_erp_item_ledger   ON erp_payroll_items(ledger_id);
CREATE INDEX IF NOT EXISTS idx_erp_item_employee ON erp_payroll_items(employee_id);

-- =============================================================
-- 5. 사원별 급여 대장 지급/공제 상세 내역 테이블 (Payroll Details - 1:N)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_payroll_details (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payroll_item_id UUID NOT NULL REFERENCES erp_payroll_items(id) ON DELETE CASCADE,
    code            VARCHAR(50) NOT NULL REFERENCES erp_payroll_codes(code),
    amount          NUMERIC(12,2) DEFAULT 0,
    is_taxable      BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (payroll_item_id, code)
);

CREATE INDEX IF NOT EXISTS idx_erp_detail_item ON erp_payroll_details(payroll_item_id);

-- =============================================================
-- updated_at 트리거 연결
-- =============================================================
DROP TRIGGER IF EXISTS trg_erp_payroll_ledgers_updated ON erp_payroll_ledgers;
CREATE TRIGGER trg_erp_payroll_ledgers_updated
    BEFORE UPDATE ON erp_payroll_ledgers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_erp_salary_templates_updated ON erp_salary_templates;
CREATE TRIGGER trg_erp_salary_templates_updated
    BEFORE UPDATE ON erp_salary_templates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_erp_payroll_items_updated ON erp_payroll_items;
CREATE TRIGGER trg_erp_payroll_items_updated
    BEFORE UPDATE ON erp_payroll_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 대한민국 표준 급여수당 및 공제 코드 기초데이터 주입 (Seeding)
-- =============================================================

-- 1) 지급 항목 (Allowances)
INSERT INTO erp_payroll_codes (code, name, type, is_taxable, tax_free_limit, is_system, sort_order)
VALUES
  ('BASE', '기본급', 'allowance', TRUE, 0, TRUE, 10),
  ('POSITION', '직책수당', 'allowance', TRUE, 0, FALSE, 20),
  ('MEAL', '식대', 'allowance', FALSE, 200000, TRUE, 30),
  ('CAR', '자가운전보조금', 'allowance', FALSE, 200000, TRUE, 40),
  ('OVERTIME', '연장근로수당', 'allowance', TRUE, 0, FALSE, 50),
  ('BONUS', '성과상여금', 'allowance', TRUE, 0, FALSE, 60)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_taxable = EXCLUDED.is_taxable,
    tax_free_limit = EXCLUDED.tax_free_limit,
    sort_order = EXCLUDED.sort_order;

-- 2) 공제 항목 (Deductions)
INSERT INTO erp_payroll_codes (code, name, type, is_taxable, tax_free_limit, is_system, sort_order)
VALUES
  ('PENSION', '국민연금', 'deduction', FALSE, 0, TRUE, 110),
  ('HEALTH', '건강보험', 'deduction', FALSE, 0, TRUE, 120),
  ('LTC', '장기요양보험', 'deduction', FALSE, 0, TRUE, 130),
  ('EMP', '고용보험', 'deduction', FALSE, 0, TRUE, 140),
  ('INCOME_TAX', '소득세', 'deduction', FALSE, 0, TRUE, 150),
  ('LOCAL_TAX', '지방소득세', 'deduction', FALSE, 0, TRUE, 160)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_system = EXCLUDED.is_system,
    sort_order = EXCLUDED.sort_order;

-- =============================================================
-- 임직원 급여 계약 템플릿 초기 정보 시딩 (임직원 존재할 시 매칭 주입)
-- =============================================================
INSERT INTO erp_salary_templates (employee_id, base_pay, position_pay, meal_allowance, car_allowance, use_national_pension, use_health_insurance, use_employment_insurance, income_tax_rate)
SELECT id, 3000000.00, 200000.00, 200000.00, 100000.00, TRUE, TRUE, TRUE, 100
FROM erp_employees
ON CONFLICT (employee_id) DO NOTHING;

COMMENT ON TABLE erp_payroll_ledgers IS 'ERP 급여대장 마스터. 지급 회차 단위 정보';
COMMENT ON TABLE erp_payroll_codes   IS 'ERP 급여 수당/공제 코드 및 과세 설정';
COMMENT ON TABLE erp_salary_templates IS '사원별 기본 급여 연봉 계약 및 기본 4대보험 마스터';
COMMENT ON TABLE erp_payroll_items   IS '급여대장 참여 사원별 최종 급여 총액 내역';
COMMENT ON TABLE erp_payroll_details IS '사원별 개별 지급/공제 상세 금액 정보';
