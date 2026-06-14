-- =============================================================
-- ProjectOS — ERP HR Module Extension Schema
-- 추가 테이블: 근태, 연차/휴가, 급여, 인사평가
-- 실행: psql -h localhost -p 5433 -U spring -d springdb -f ERP_HR_EXT_DDL.sql
-- =============================================================

-- =============================================================
-- 1. 근태 관리 (Attendance)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_attendance (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    work_date       DATE NOT NULL,
    check_in        TIMESTAMPTZ,
    check_out       TIMESTAMPTZ,
    work_hours      NUMERIC(4,1) DEFAULT 0,
    overtime_hours  NUMERIC(4,1) DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'present',
    memo            TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, work_date)
);

CREATE INDEX IF NOT EXISTS idx_erp_att_employee ON erp_attendance(employee_id);
CREATE INDEX IF NOT EXISTS idx_erp_att_date ON erp_attendance(work_date);

DROP TRIGGER IF EXISTS trg_erp_attendance_updated ON erp_attendance;
CREATE TRIGGER trg_erp_attendance_updated
    BEFORE UPDATE ON erp_attendance
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 2. 연차/휴가 관리 (Leave Requests)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_leave_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    leave_type      VARCHAR(30) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    total_days      NUMERIC(4,1) NOT NULL,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) DEFAULT 'pending',
    approver_id     UUID REFERENCES erp_employees(id) ON DELETE SET NULL,
    approved_at     TIMESTAMPTZ,
    reject_reason   TEXT,
    linked_item_id  UUID REFERENCES items(id) ON DELETE SET NULL,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_erp_leave_employee ON erp_leave_requests(employee_id);
CREATE INDEX IF NOT EXISTS idx_erp_leave_status ON erp_leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_erp_leave_dates ON erp_leave_requests(start_date, end_date);

DROP TRIGGER IF EXISTS trg_erp_leave_requests_updated ON erp_leave_requests;
CREATE TRIGGER trg_erp_leave_requests_updated
    BEFORE UPDATE ON erp_leave_requests
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 연차 사용 내역 (잔여 일수 계산용)
CREATE TABLE IF NOT EXISTS erp_leave_balances (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    year            INTEGER NOT NULL,
    total_days      NUMERIC(4,1) NOT NULL DEFAULT 15,
    used_days       NUMERIC(4,1) DEFAULT 0,
    remaining_days  NUMERIC(4,1) DEFAULT 15,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, year)
);

DROP TRIGGER IF EXISTS trg_erp_leave_balances_updated ON erp_leave_balances;
CREATE TRIGGER trg_erp_leave_balances_updated
    BEFORE UPDATE ON erp_leave_balances
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 3. 급여 관리 (Payroll)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_payrolls (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    pay_year        INTEGER NOT NULL,
    pay_month       INTEGER NOT NULL,
    pay_date        DATE NOT NULL,
    -- 급여 구성 항목
    base_pay        NUMERIC(12,2) DEFAULT 0,
    position_pay    NUMERIC(12,2) DEFAULT 0,
    overtime_pay    NUMERIC(12,2) DEFAULT 0,
    bonus_pay       NUMERIC(12,2) DEFAULT 0,
    meal_allowance  NUMERIC(12,2) DEFAULT 0,
    transportation  NUMERIC(12,2) DEFAULT 0,
    -- 공제 항목
    income_tax      NUMERIC(12,2) DEFAULT 0,
    local_tax       NUMERIC(12,2) DEFAULT 0,
    national_pension NUMERIC(12,2) DEFAULT 0,
    health_insurance NUMERIC(12,2) DEFAULT 0,
    employment_insurance NUMERIC(12,2) DEFAULT 0,
    longterm_care   NUMERIC(12,2) DEFAULT 0,
    -- 합계
    gross_pay       NUMERIC(12,2) DEFAULT 0,
    total_deduction NUMERIC(12,2) DEFAULT 0,
    net_pay         NUMERIC(12,2) DEFAULT 0,
    -- 상태
    status          VARCHAR(20) DEFAULT 'draft',
    confirmed_by    UUID REFERENCES users(id),
    confirmed_at    TIMESTAMPTZ,
    memo            TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, pay_year, pay_month)
);

CREATE INDEX IF NOT EXISTS idx_erp_payroll_workspace ON erp_payrolls(workspace_id);
CREATE INDEX IF NOT EXISTS idx_erp_payroll_employee ON erp_payrolls(employee_id);
CREATE INDEX IF NOT EXISTS idx_erp_payroll_period ON erp_payrolls(pay_year, pay_month);

DROP TRIGGER IF EXISTS trg_erp_payrolls_updated ON erp_payrolls;
CREATE TRIGGER trg_erp_payrolls_updated
    BEFORE UPDATE ON erp_payrolls
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 4. 인사 평가 (Performance Review)
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_performance_reviews (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES erp_employees(id) ON DELETE CASCADE,
    reviewer_id     UUID NOT NULL REFERENCES erp_employees(id),
    review_year     INTEGER NOT NULL,
    review_period   VARCHAR(20) NOT NULL DEFAULT 'annual',
    ratings         JSONB NOT NULL DEFAULT '{}',
    total_score     NUMERIC(4,2),
    comment         TEXT,
    status          VARCHAR(20) DEFAULT 'draft',
    submitted_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_erp_review_employee ON erp_performance_reviews(employee_id);
CREATE INDEX IF NOT EXISTS idx_erp_review_period ON erp_performance_reviews(review_year, review_period);

DROP TRIGGER IF EXISTS trg_erp_performance_reviews_updated ON erp_performance_reviews;
CREATE TRIGGER trg_erp_performance_reviews_updated
    BEFORE UPDATE ON erp_performance_reviews
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- COMMENTS
-- =============================================================
COMMENT ON TABLE  erp_attendance              IS 'ERP 출퇴근 기록. 직원별 일일 근태';
COMMENT ON TABLE  erp_leave_requests           IS 'ERP 연차/휴가 신청. 승인 워크플로우 포함';
COMMENT ON TABLE  erp_leave_balances           IS 'ERP 연차 사용 내역. 년간 잔여 일수 계산';
COMMENT ON TABLE  erp_payrolls                 IS 'ERP 급여 명세서. 월별 급여 구성/공제 항목';
COMMENT ON TABLE  erp_performance_reviews      IS 'ERP 인사 평가. JSONB 평가 항목/점수';

COMMENT ON COLUMN erp_attendance.id              IS '근태 기록 고유 식별자';
COMMENT ON COLUMN erp_attendance.employee_id     IS '대상 직원 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_attendance.work_date       IS '근무 일자';
COMMENT ON COLUMN erp_attendance.check_in        IS '출근 일시';
COMMENT ON COLUMN erp_attendance.check_out       IS '퇴근 일시';
COMMENT ON COLUMN erp_attendance.work_hours      IS '소정 근무 시간';
COMMENT ON COLUMN erp_attendance.overtime_hours  IS '연장 근무 시간';
COMMENT ON COLUMN erp_attendance.status          IS '근태 상태 (present/absent/late/leave)';
COMMENT ON COLUMN erp_attendance.memo            IS '근태 특이사항 메모';
COMMENT ON COLUMN erp_attendance.created_at      IS '등록 일시';
COMMENT ON COLUMN erp_attendance.updated_at      IS '최근 수정 일시';

COMMENT ON COLUMN erp_leave_requests.id              IS '연차/휴가 신청 고유 식별자';
COMMENT ON COLUMN erp_leave_requests.employee_id     IS '신청 직원 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_leave_requests.leave_type      IS '휴가 종류 (annual/sick/maternity/special)';
COMMENT ON COLUMN erp_leave_requests.start_date      IS '휴가 시작일';
COMMENT ON COLUMN erp_leave_requests.end_date        IS '휴가 종료일';
COMMENT ON COLUMN erp_leave_requests.total_days      IS '사용 일수';
COMMENT ON COLUMN erp_leave_requests.reason          IS '사유';
COMMENT ON COLUMN erp_leave_requests.status          IS '결재 상태 (pending/approved/rejected/cancelled)';
COMMENT ON COLUMN erp_leave_requests.approver_id     IS '결재권자 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_leave_requests.approved_at     IS '결재 승인 일시';
COMMENT ON COLUMN erp_leave_requests.reject_reason   IS '반려 사유';
COMMENT ON COLUMN erp_leave_requests.linked_item_id  IS '연계된 보드 아이템 ID (items.id)';
COMMENT ON COLUMN erp_leave_requests.created_by      IS '신청 작성자 ID (users.id)';
COMMENT ON COLUMN erp_leave_requests.created_at      IS '등록 일시';
COMMENT ON COLUMN erp_leave_requests.updated_at      IS '최근 수정 일시';

COMMENT ON COLUMN erp_leave_balances.id              IS '연차 잔여 일수 고유 식별자';
COMMENT ON COLUMN erp_leave_balances.employee_id     IS '대상 직원 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_leave_balances.year            IS '대상 연도 (예: 2026)';
COMMENT ON COLUMN erp_leave_balances.total_days      IS '발생 총 연차 일수';
COMMENT ON COLUMN erp_leave_balances.used_days       IS '사용 연차 일수';
COMMENT ON COLUMN erp_leave_balances.remaining_days  IS '잔여 연차 일수';
COMMENT ON COLUMN erp_leave_balances.created_at      IS '등록 일시';
COMMENT ON COLUMN erp_leave_balances.updated_at      IS '최근 수정 일시';

COMMENT ON COLUMN erp_payrolls.id              IS '급여대장 고유 식별자';
COMMENT ON COLUMN erp_payrolls.workspace_id    IS '소속 워크스페이스 ID';
COMMENT ON COLUMN erp_payrolls.employee_id     IS '대상 직원 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_payrolls.pay_year        IS '지급 귀속 연도';
COMMENT ON COLUMN erp_payrolls.pay_month       IS '지급 귀속 월';
COMMENT ON COLUMN erp_payrolls.pay_date        IS '급여 지급일';
COMMENT ON COLUMN erp_payrolls.base_pay        IS '기본급';
COMMENT ON COLUMN erp_payrolls.position_pay    IS '직책 수당';
COMMENT ON COLUMN erp_payrolls.overtime_pay    IS '연장 근로 수당';
COMMENT ON COLUMN erp_payrolls.bonus_pay       IS '상여금';
COMMENT ON COLUMN erp_payrolls.meal_allowance  IS '식대';
COMMENT ON COLUMN erp_payrolls.transportation  IS '여비 교통비';
COMMENT ON COLUMN erp_payrolls.income_tax      IS '소득세';
COMMENT ON COLUMN erp_payrolls.local_tax       IS '지방소득세';
COMMENT ON COLUMN erp_payrolls.national_pension IS '국민연금';
COMMENT ON COLUMN erp_payrolls.health_insurance IS '건강보험';
COMMENT ON COLUMN erp_payrolls.employment_insurance IS '고용보험';
COMMENT ON COLUMN erp_payrolls.longterm_care   IS '장기요양보험료';
COMMENT ON COLUMN erp_payrolls.gross_pay       IS '지급 총액 (세전)';
COMMENT ON COLUMN erp_payrolls.total_deduction IS '공제 총액';
COMMENT ON COLUMN erp_payrolls.net_pay         IS '실지급액 (세후)';
COMMENT ON COLUMN erp_payrolls.status          IS '지급 상태 (draft/confirmed/paid)';
COMMENT ON COLUMN erp_payrolls.confirmed_by    IS '확정자 ID (users.id)';
COMMENT ON COLUMN erp_payrolls.confirmed_at    IS '확정 일시';
COMMENT ON COLUMN erp_payrolls.memo            IS '급여 특이사항 메모';
COMMENT ON COLUMN erp_payrolls.created_by      IS '대장 등록자 ID (users.id)';
COMMENT ON COLUMN erp_payrolls.created_at      IS '등록 일시';
COMMENT ON COLUMN erp_payrolls.updated_at      IS '최근 수정 일시';

COMMENT ON COLUMN erp_performance_reviews.id              IS '인사평가 고유 식별자';
COMMENT ON COLUMN erp_performance_reviews.workspace_id    IS '소속 워크스페이스 ID';
COMMENT ON COLUMN erp_performance_reviews.employee_id     IS '피평가 직원 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_performance_reviews.reviewer_id     IS '평가 직원 식별자 (erp_employees.id)';
COMMENT ON COLUMN erp_performance_reviews.review_year     IS '평가 연도';
COMMENT ON COLUMN erp_performance_reviews.review_period   IS '평가 주기 (annual/first_half/second_half)';
COMMENT ON COLUMN erp_performance_reviews.ratings         IS '평가 항목별 상세 점수 (JSONB)';
COMMENT ON COLUMN erp_performance_reviews.total_score     IS '평가 최종 합산 종합 점수';
COMMENT ON COLUMN erp_performance_reviews.comment         IS '평가 종합 서술의견';
COMMENT ON COLUMN erp_performance_reviews.status          IS '평가 상태 (draft/submitted/acknowledged)';
COMMENT ON COLUMN erp_performance_reviews.submitted_at    IS '제출 일시';
COMMENT ON COLUMN erp_performance_reviews.created_at      IS '등록 일시';
COMMENT ON COLUMN erp_performance_reviews.updated_at      IS '최근 수정 일시';
