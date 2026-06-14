-- =============================================================
-- ProjectOS — ERP Sales (견적서 & 세금계산서) Module Schema
-- 대상: PostgreSQL
-- 생성일: 2026-06-04
-- =============================================================

-- =============================================================
-- 1. 견적서 마스터
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_quotations (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quotation_no        VARCHAR(50) NOT NULL UNIQUE,          -- 견적 번호 (예: QT-20260604-001)
    title               VARCHAR(200) NOT NULL,                -- 견적 건명
    quote_date          DATE NOT NULL,                        -- 견적 일자
    valid_date          DATE,                                 -- 유효 기간 (만료일)
    supplier_reg_no     VARCHAR(50) NOT NULL,                 -- 공급자 사업자등록번호
    supplier_name       VARCHAR(100) NOT NULL,                -- 공급자 상호/법인명
    supplier_ceo        VARCHAR(50) NOT NULL,                 -- 공급자 대표자명
    supplier_address    VARCHAR(255),                         -- 공급자 주소
    supplier_biz_type   VARCHAR(100),                         -- 공급자 업태
    supplier_biz_item   VARCHAR(100),                         -- 공급자 업종
    customer_name       VARCHAR(100) NOT NULL,                -- 공급받는자 상호/성명
    customer_ceo        VARCHAR(50),                          -- 공급받는자 대표자명
    total_supply_value  NUMERIC(15,2) DEFAULT 0.00,           -- 총 공급가액
    total_tax_value     NUMERIC(15,2) DEFAULT 0.00,           -- 총 세액 (부가세)
    total_amount        NUMERIC(15,2) DEFAULT 0.00,           -- 합계금액 (공급가액 + 세액)
    remarks             TEXT,                                 -- 특이사항 및 메모
    status              VARCHAR(20) DEFAULT 'draft',          -- 상태 ('draft' | 'sent' | 'approved' | 'rejected')
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL, -- 작성자
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 2. 견적서 품목 상세
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_quotation_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quotation_id        UUID NOT NULL REFERENCES erp_quotations(id) ON DELETE CASCADE,
    item_name           VARCHAR(200) NOT NULL,                -- 품명
    spec                VARCHAR(100),                         -- 규격
    qty                 INTEGER DEFAULT 1,                    -- 수량
    unit_price          NUMERIC(15,2) DEFAULT 0.00,           -- 단가
    supply_value        NUMERIC(15,2) DEFAULT 0.00,           -- 공급가액
    tax_value           NUMERIC(15,2) DEFAULT 0.00,           -- 세액
    remarks             VARCHAR(255),                         -- 비고
    sort_order          INTEGER DEFAULT 0
);

-- =============================================================
-- 3. 전자 세금계산서 마스터
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_tax_invoices (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id            VARCHAR(50) NOT NULL UNIQUE,          -- 국세청 승인번호 (24자리 규격)
    write_date          DATE NOT NULL,                        -- 작성일자
    supplier_reg_no     VARCHAR(50) NOT NULL,                 -- 공급자 등록번호
    supplier_sub_no     VARCHAR(4),                           -- 공급자 종사업장번호
    supplier_name       VARCHAR(100) NOT NULL,                -- 공급자 상호
    supplier_ceo        VARCHAR(50) NOT NULL,                 -- 공급자 대표자
    supplier_address    VARCHAR(255),                         -- 공급자 주소
    supplier_biz_type   VARCHAR(100),                         -- 공급자 업태
    supplier_biz_item   VARCHAR(100),                         -- 공급자 업종
    supplier_email      VARCHAR(100),                         -- 공급자 담당자 이메일
    customer_reg_no     VARCHAR(50) NOT NULL,                 -- 공급받는자 등록번호 (사업자번호/주민번호)
    customer_sub_no     VARCHAR(4),                           -- 공급받는자 종사업장번호
    customer_name       VARCHAR(100) NOT NULL,                -- 공급받는자 상호
    customer_ceo        VARCHAR(50),                          -- 공급받는자 대표자
    customer_address    VARCHAR(255),                         -- 공급받는자 주소
    customer_biz_type   VARCHAR(100),                         -- 공급받는자 업태
    customer_biz_item   VARCHAR(100),                         -- 공급받는자 업종
    customer_email1     VARCHAR(100),                         -- 공급받는자 담당자 이메일 1
    total_supply_value  NUMERIC(15,2) DEFAULT 0.00,           -- 합계 공급가액
    total_tax_value     NUMERIC(15,2) DEFAULT 0.00,           -- 합계 세액
    total_amount        NUMERIC(15,2) DEFAULT 0.00,           -- 합계금액
    purpose             VARCHAR(10) DEFAULT 'charge',         -- 영수/청구 구분 ('charge': 청구, 'receipt': 영수)
    status              VARCHAR(20) DEFAULT 'draft',          -- 상태 ('draft': 작성중, 'issued': 발행완료)
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 4. 전자 세금계산서 품목 상세
-- =============================================================
CREATE TABLE IF NOT EXISTS erp_tax_invoice_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tax_invoice_id      UUID NOT NULL REFERENCES erp_tax_invoices(id) ON DELETE CASCADE,
    item_date           VARCHAR(5),                           -- 작성 월일 (MM-DD)
    item_name           VARCHAR(200) NOT NULL,                -- 품목
    spec                VARCHAR(100),                         -- 규격
    qty                 INTEGER DEFAULT 1,                    -- 수량
    unit_price          NUMERIC(15,2) DEFAULT 0.00,           -- 단가
    supply_value        NUMERIC(15,2) DEFAULT 0.00,           -- 공급가액
    tax_value           NUMERIC(15,2) DEFAULT 0.00,           -- 세액
    remarks             VARCHAR(255),                         -- 비고
    sort_order          INTEGER DEFAULT 0
);

-- =============================================================
-- 5. 인덱스 생성
-- =============================================================
CREATE INDEX IF NOT EXISTS idx_erp_quotations_no ON erp_quotations(quotation_no);
CREATE INDEX IF NOT EXISTS idx_erp_quotations_date ON erp_quotations(quote_date);
CREATE INDEX IF NOT EXISTS idx_erp_quotation_items_ref ON erp_quotation_items(quotation_id);

CREATE INDEX IF NOT EXISTS idx_erp_tax_invoices_issue ON erp_tax_invoices(issue_id);
CREATE INDEX IF NOT EXISTS idx_erp_tax_invoices_date ON erp_tax_invoices(write_date);
CREATE INDEX IF NOT EXISTS idx_erp_tax_invoice_items_ref ON erp_tax_invoice_items(tax_invoice_id);

-- =============================================================
-- 6. updated_at 자동 갱신 트리거 설정
-- =============================================================
CREATE TRIGGER trg_erp_quotations_updated
    BEFORE UPDATE ON erp_quotations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_erp_tax_invoices_updated
    BEFORE UPDATE ON erp_tax_invoices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 7. 테이블 및 컬럼 코멘트(설명) 추가
-- =============================================================
COMMENT ON TABLE erp_quotations IS 'ERP 견적서 마스터 테이블';
COMMENT ON COLUMN erp_quotations.quotation_no IS '견적 번호 (자동 채번)';
COMMENT ON COLUMN erp_quotations.total_supply_value IS '총 공급가액 (합계)';
COMMENT ON COLUMN erp_quotations.total_tax_value IS '총 세액 (부가세 합계)';
COMMENT ON COLUMN erp_quotations.total_amount IS '합계금액 (공급가액 + 세액)';
COMMENT ON COLUMN erp_quotations.status IS '견적서 상태 (draft/sent/approved/rejected)';

COMMENT ON TABLE erp_tax_invoices IS 'ERP 전자세금계산서 마스터 테이블';
COMMENT ON COLUMN erp_tax_invoices.issue_id IS '국세청 표준 승인번호 (24자리)';
COMMENT ON COLUMN erp_tax_invoices.purpose IS '영수/청구 구분 (charge/receipt)';
COMMENT ON COLUMN erp_tax_invoices.status IS '세금계산서 상태 (draft/issued)';
