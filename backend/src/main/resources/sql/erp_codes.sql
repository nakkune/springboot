-- ============================================================
-- ERP 공통 코드 테이블 (기준정보)
-- 직위, 직책, 고용형태, 휴가유형, 평가기간, 사원상태 등 관리
-- ============================================================

CREATE TABLE IF NOT EXISTS erp_codes (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code_group       VARCHAR(50)  NOT NULL,
    code_group_name  VARCHAR(100) NOT NULL DEFAULT '',
    code             VARCHAR(50)  NOT NULL,
    label            VARCHAR(100) NOT NULL,
    sort_order       INT          NOT NULL DEFAULT 0,
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(code_group, code)
);

COMMENT ON TABLE  erp_codes                IS '공통 코드 (기준정보)';
COMMENT ON COLUMN erp_codes.code_group     IS '코드 그룹 식별자 (POSITION, JOB_TITLE, EMPLOYMENT_TYPE, LEAVE_TYPE, REVIEW_PERIOD, EMPLOYEE_STATUS)';
COMMENT ON COLUMN erp_codes.code_group_name IS '코드 그룹 표시명 (직위, 직책, 고용형태 등)';
COMMENT ON COLUMN erp_codes.code           IS '시스템 식별 코드';
COMMENT ON COLUMN erp_codes.label          IS 'UI 표시명';

-- 샘플 데이터
-- INSERT INTO erp_codes (code_group, code, label, sort_order) VALUES
--     ('POSITION', 'intern',      '인턴',     1),
--     ('POSITION', 'assistant',   '주임',     2),
--     ('POSITION', 'manager',     '대리',     3),
--     ('POSITION', 'deputy_head', '과장',     4),
--     ('POSITION', 'deputy_gm',   '차장',     5),
--     ('POSITION', 'gm',          '부장',     6),
--     ('EMPLOYMENT_TYPE', 'full_time',  '정규직',   1),
--     ('EMPLOYMENT_TYPE', 'part_time',  '파트타임', 2),
--     ('EMPLOYMENT_TYPE', 'contract',   '계약직',   3),
--     ('EMPLOYMENT_TYPE', 'intern',     '인턴',     4),
--     ('LEAVE_TYPE', 'annual',    '연차',       1),
--     ('LEAVE_TYPE', 'sick',      '병가',       2),
--     ('LEAVE_TYPE', 'personal',  '개인 휴가',  3),
--     ('LEAVE_TYPE', 'maternity', '출산 휴가',  4),
--     ('LEAVE_TYPE', 'etc',       '기타',       5),
--     ('REVIEW_PERIOD', 'annual',  '연간', 1),
--     ('REVIEW_PERIOD', 'half',    '반기', 2),
--     ('REVIEW_PERIOD', 'quarter', '분기', 3),
--     ('EMPLOYEE_STATUS', 'active',   '재직', 1),
--     ('EMPLOYEE_STATUS', 'leave',    '휴직', 2),
--     ('EMPLOYEE_STATUS', 'resigned', '퇴사', 3);
