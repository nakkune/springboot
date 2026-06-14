-- [NEW] 기존 쓰레기 데이터 연쇄적 안전 삭제 후 완벽 시딩 (10년 차 시니어 복구 및 시드 스크립트 규격)
DELETE FROM item_values WHERE item_id IN (SELECT id FROM items WHERE board_id = '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid);
DELETE FROM items WHERE board_id = '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid;
DELETE FROM board_groups WHERE board_id = '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid;
DELETE FROM board_columns WHERE board_id = '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid;
DELETE FROM boards WHERE id = '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid;
DELETE FROM projects WHERE id = '5c1e3f8b-9e4a-4b72-912a-4f53d26bce2b'::uuid;
DELETE FROM workspaces WHERE id = '8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid;
DELETE FROM users WHERE email = 'admin@example.com';

-- 0. 유저 삽입 (외래키 owner_id 및 created_by 완결성을 위해 선행 삽입)
INSERT INTO users (id, email, password_hash, full_name, member_status, is_active)
VALUES (
    'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 
    'admin@example.com', 
    '$2b$12$iXP6dbYHl/tUZfWbgk0EHuSHwBauhywhmiESeQz3AkIMN3xWqalE.', -- 소셜 로그인 및 세션 매치용 비밀번호
    'Administrator', 
    'approved',
    true
);

UPDATE users 
SET password_hash = '$2b$12$iXP6dbYHl/tUZfWbgk0EHuSHwBauhywhmiESeQz3AkIMN3xWqalE.' 
WHERE email = 'admin@example.com';

-- 1. 워크스페이스 삽입
INSERT INTO workspaces (id, name, slug, owner_id)
VALUES (
    '8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid, 
    '메인 워크스페이스', 
    'main-workspace', 
    'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid
);

-- 2. 프로젝트 삽입
INSERT INTO projects (id, workspace_id, name, created_by)
VALUES (
    '5c1e3f8b-9e4a-4b72-912a-4f53d26bce2b'::uuid,
    '8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid,
    'MVP 제품 개발 프로젝트',
    'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid
);

-- 3. 보드 삽입
INSERT INTO boards (id, project_id, name, description, created_by)
VALUES (
    '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid,
    '5c1e3f8b-9e4a-4b72-912a-4f53d26bce2b'::uuid,
    'MVP 개발 프로젝트 보드',
    '스프레드시트 및 칸반 뷰 기능 테스트용',
    'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid
);

-- 4. 보드 컬럼 삽입
INSERT INTO board_columns (id, board_id, name, column_type, position) VALUES
    ('a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '상태', 'status', 0),
    ('b2c3d4e5-6f7a-8b9c-0d1e-2f3a4b5c6d7e'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '담당자', 'person', 1),
    ('c3d4e5f6-7a8b-9c0d-1e2f-3a4b5c6d7e8f'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '마감일', 'date', 2),
    ('d4e5f6a7-8b9c-0d1e-2f3a-4b5c6d7e8f9a'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '우선순위', 'priority', 3),
    ('e5f6a7b8-9c0d-1e2f-2a3b-4c5d6e7f8g9h'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '시작일', 'date', 4),
    ('f6g7h8i9-0d1e-2f3a-4b5c-6d7e8f9a0b1c'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '종료일', 'date', 5),
    ('g7h8i9j0-1e2f-3a4b-5c6d-7e8f9a0b1c2d'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '태그', 'tags', 6);

-- 5. 보드 그룹 삽입
INSERT INTO board_groups (id, board_id, title, color, position)
VALUES 
    ('4d5e6f7a-8b9c-0d1e-bf2f-4b5c6d7e8f9a'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '이번 주 작업', '#4F46E5', 0),
    ('5e6f7a8b-9c0d-1e2f-ae3a-5c6d7e8f9a0b'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '백로그', '#94A3B8', 1);

-- 6. 보드 아이템 삽입
INSERT INTO items (id, board_id, group_id, name, position, created_by)
VALUES 
    ('6f7a8b9c-0d1e-2f3a-ab4c-6d7e8f9a0b1c'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '4d5e6f7a-8b9c-0d1e-bf2f-4b5c6d7e8f9a'::uuid, '보드 뷰 UI 설계', 0, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid),
    ('7a8b9c0d-1e2f-3a4b-bc5d-7e8f9a0b1c2d'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '4d5e6f7a-8b9c-0d1e-bf2f-4b5c6d7e8f9a'::uuid, '칸반 드래그앤드롭', 1, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid),
    ('8b9c0d1e-2f3a-4b5c-cd6e-8f9a0b1c2d3e'::uuid, '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c'::uuid, '5e6f7a8b-9c0d-1e2f-ae3a-5c6d7e8f9a0b'::uuid, '대시보드 위젯 컴포넌트', 0, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid);

-- 7. 아이템 밸류(상태, 담당자, 마감일 정보) 삽입
INSERT INTO item_values (id, item_id, column_id, value_text)
VALUES
    ('9a2f3a4b-5c6d-7e8f-9a0b-1c2d3e4f5a6b'::uuid, '6f7a8b9c-0d1e-2f3a-ab4c-6d7e8f9a0b1c'::uuid, '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'::uuid, '완료'),
    ('9b3a4b5c-6d7e-8f9a-0b1c-2d3e4f5a6b7c'::uuid, '6f7a8b9c-0d1e-2f3a-ab4c-6d7e8f9a0b1c'::uuid, '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'::uuid, '김개발'),
    ('9c4b5c6d-7e8f-9a0b-1c2d-3e4f5a6b7c8d'::uuid, '6f7a8b9c-0d1e-2f3a-ab4c-6d7e8f9a0b1c'::uuid, '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'::uuid, '2026-05-30'),
    ('9d5c6d7e-8f9a-0b1c-2d3e-4f5a6b7c8d9e'::uuid, '7a8b9c0d-1e2f-3a4b-bc5d-7e8f9a0b1c2d'::uuid, '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'::uuid, '진행 중'),
    ('9e6d7e8f-9a0b-1c2d-3e4f-5a6b7c8d9e0f'::uuid, '7a8b9c0d-1e2f-3a4b-bc5d-7e8f9a0b1c2d'::uuid, '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'::uuid, '이프론트'),
    ('9f7e8f9a-0b1c-2d3e-4f5a-6b7c8d9e0f1a'::uuid, '7a8b9c0d-1e2f-3a4b-bc5d-7e8f9a0b1c2d'::uuid, '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'::uuid, '2026-06-01'),
    ('9a8f9a0b-1c2d-3e4f-5a6b-7c8d9e0f1a2b'::uuid, '8b9c0d1e-2f3a-4b5c-cd6e-8f9a0b1c2d3e'::uuid, '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'::uuid, '시작 전');

-- ==========================================
-- 8. [NEW] 워크스페이스 멤버 테이블 신설 DDL (E2E 팀원 관리용)
-- ==========================================
CREATE TABLE IF NOT EXISTS workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(workspace_id, user_id)
);

-- ==========================================
-- 9. [NEW] 실전 테스트용 팀원 유저 시딩
-- ==========================================
INSERT INTO users (id, email, password_hash, full_name, member_status, is_active)
VALUES 
    ('d1111111-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'dev@example.com', '1234', '김개발', 'approved', true),
    ('d2222222-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'front@example.com', '1234', '이프론트', 'approved', true),
    ('d3333333-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'design@example.com', '1234', '박디자인', 'approved', true)
ON CONFLICT (email) DO NOTHING;

-- ==========================================
-- 10. [NEW] 워크스페이스 멤버 매핑 관계 적재
-- ==========================================
INSERT INTO workspace_members (workspace_id, user_id, role)
VALUES 
    ('8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'owner'),
    ('8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid, 'd1111111-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'member'),
    ('8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid, 'd2222222-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'member'),
    ('8f2e4c6a-4d2a-4a62-8149-1e35d27bcf1a'::uuid, 'd3333333-e9f8-7a6b-5c4d-3e2f1a0b9c8d'::uuid, 'member')
ON CONFLICT (workspace_id, user_id) DO NOTHING;

-- 11. 휴가 진행 상태 공통 코드 시딩
INSERT INTO erp_codes (id, code_group, code_group_name, code, label, sort_order)
VALUES 
    (gen_random_uuid(), 'LEAVE_STATUS', '휴가진행상태', 'pending', '대기', 1),
    (gen_random_uuid(), 'LEAVE_STATUS', '휴가진행상태', 'approved', '승인', 2),
    (gen_random_uuid(), 'LEAVE_STATUS', '휴가진행상태', 'rejected', '반려', 3),
    (gen_random_uuid(), 'LEAVE_STATUS', '휴가진행상태', 'cancelled', '취소', 4)
ON CONFLICT (code_group, code) DO UPDATE SET
    label = EXCLUDED.label,
    sort_order = EXCLUDED.sort_order;

-- 12. 급여 진행 상태 공통 코드 시딩
INSERT INTO erp_codes (id, code_group, code_group_name, code, label, sort_order)
VALUES 
    (gen_random_uuid(), 'PAYROLL_STATUS', '급여진행상태', 'draft', '작성 중', 1),
    (gen_random_uuid(), 'PAYROLL_STATUS', '급여진행상태', 'confirmed', '확정', 2),
    (gen_random_uuid(), 'PAYROLL_STATUS', '급여진행상태', 'paid', '지급 완료', 3)
ON CONFLICT (code_group, code) DO UPDATE SET
    label = EXCLUDED.label,
    sort_order = EXCLUDED.sort_order;

-- 13. 견적서 상태 공통 코드 시딩
INSERT INTO erp_codes (id, code_group, code_group_name, code, label, sort_order)
VALUES 
    (gen_random_uuid(), 'QUOTATION_STATUS', '견적서상태', 'draft', '작성 중 (Draft)', 1),
    (gen_random_uuid(), 'QUOTATION_STATUS', '견적서상태', 'sent', '발송 완료 (Sent)', 2),
    (gen_random_uuid(), 'QUOTATION_STATUS', '견적서상태', 'approved', '계약 승인 (Approved)', 3),
    (gen_random_uuid(), 'QUOTATION_STATUS', '견적서상태', 'rejected', '반려/취소 (Rejected)', 4)
ON CONFLICT (code_group, code) DO UPDATE SET
    label = EXCLUDED.label,
    sort_order = EXCLUDED.sort_order;

-- 14. 세금계산서 상태 공통 코드 시딩
INSERT INTO erp_codes (id, code_group, code_group_name, code, label, sort_order)
VALUES 
    (gen_random_uuid(), 'TAX_INVOICE_STATUS', '세금계산서상태', 'draft', '작성 중 (Draft)', 1),
    (gen_random_uuid(), 'TAX_INVOICE_STATUS', '세금계산서상태', 'issued', '국세청 승인 발행 (Issued)', 2)
ON CONFLICT (code_group, code) DO UPDATE SET
    label = EXCLUDED.label,
    sort_order = EXCLUDED.sort_order;

-- 15. 세금계산서 영수/청구 목적 공통 코드 시딩
INSERT INTO erp_codes (id, code_group, code_group_name, code, label, sort_order)
VALUES 
    (gen_random_uuid(), 'TAX_INVOICE_PURPOSE', '세금계산서목적', 'charge', '청구 (대금 받기 전)', 1),
    (gen_random_uuid(), 'TAX_INVOICE_PURPOSE', '세금계산서목적', 'receipt', '영수 (대금 영수 완료)', 2)
ON CONFLICT (code_group, code) DO UPDATE SET
    label = EXCLUDED.label,
    sort_order = EXCLUDED.sort_order;


