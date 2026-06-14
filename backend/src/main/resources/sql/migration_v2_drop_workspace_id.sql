-- ============================================================
-- Migration v2: Remove workspace_id from erp_% tables
-- ERP 데이터를 워크스페이스에서 분리 (멀티테넌시 제거)
-- ============================================================

-- 1. erp_codes: UNIQUE 제약 조건 변경 (workspace_id 제거)
ALTER TABLE erp_codes DROP CONSTRAINT IF EXISTS erp_codes_workspace_id_code_group_code_key;
ALTER TABLE erp_codes DROP COLUMN workspace_id;
ALTER TABLE erp_codes ADD CONSTRAINT uq_erp_codes_group_code UNIQUE (code_group, code);

-- 2. erp_departments
ALTER TABLE erp_departments DROP COLUMN workspace_id;

-- 3. erp_employees
ALTER TABLE erp_employees DROP COLUMN workspace_id;

-- 4. erp_payrolls
ALTER TABLE erp_payrolls DROP COLUMN workspace_id;

-- 5. erp_performance_reviews
ALTER TABLE erp_performance_reviews DROP COLUMN workspace_id;

-- 참고: erp_attendance, erp_leave_requests, erp_leave_balances는
-- 이미 workspace_id 컬럼이 없으므로 변경 불필요
