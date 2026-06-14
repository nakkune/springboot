-- Seed Data Script with Cleanup

DO $$
DECLARE
    v_board_id UUID := '7b3d2e1a-8c4b-4f92-913a-5d63c27bde3c';
    v_col_status UUID := uuid_generate_v4();
    v_col_person UUID := uuid_generate_v4();
    v_col_start UUID := uuid_generate_v4();
    v_col_end UUID := uuid_generate_v4();
    v_col_due UUID := uuid_generate_v4();
    v_col_tags UUID := uuid_generate_v4();
    
    v_group1 UUID := uuid_generate_v4();
    v_group2 UUID := uuid_generate_v4();
    
    v_item1 UUID := uuid_generate_v4();
    v_item2 UUID := uuid_generate_v4();
    v_item3 UUID := uuid_generate_v4();
BEGIN
    -- 기존 데이터 정리 (해당 보드의 모든 아이템, 그룹, 컬럼 삭제)
    DELETE FROM items WHERE board_id = v_board_id;
    DELETE FROM board_groups WHERE board_id = v_board_id;
    DELETE FROM board_columns WHERE board_id = v_board_id;

    -- 컬럼 추가
    INSERT INTO board_columns (id, board_id, name, column_type, settings, position) VALUES
        (v_col_status, v_board_id, '상태', 'status', '{"options":[{"id":"1","label":"완료","color":"#00C875"},{"id":"2","label":"진행 중","color":"#FDAB3D"},{"id":"3","label":"시작 전","color":"#C4C4C4"}]}', 1),
        (v_col_person, v_board_id, '담당자', 'person', '{}', 2),
        (v_col_start, v_board_id, '시작일', 'date', '{}', 3),
        (v_col_end, v_board_id, '종료일', 'date', '{}', 4),
        (v_col_due, v_board_id, '마감일', 'date', '{}', 5),
        (v_col_tags, v_board_id, '태그', 'tags', '{}', 6);

    -- 그룹 추가
    INSERT INTO board_groups (id, board_id, title, color, position) VALUES
        (v_group1, v_board_id, '이번 주 작업', '#579BFC', 1),
        (v_group2, v_board_id, '다음 주 작업', '#A25DDC', 2);

    -- 아이템 추가
    INSERT INTO items (id, board_id, group_id, name, position, created_by) VALUES
        (v_item1, v_board_id, v_group1, 'UI 디자인 완료', 1, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'),
        (v_item2, v_board_id, v_group1, 'API 서버 연동', 2, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d'),
        (v_item3, v_board_id, v_group2, 'QA 테스트 진행', 1, 'a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d');

    -- 아이템 밸류 (UI 디자인 완료)
    INSERT INTO item_values (item_id, column_id, value_text, value_json) VALUES
        (v_item1, v_col_status, '완료', NULL),
        (v_item1, v_col_person, NULL, '["d3333333-e9f8-7a6b-5c4d-3e2f1a0b9c8d"]'),
        (v_item1, v_col_start, '2026-05-01', NULL),
        (v_item1, v_col_end, '2026-05-15', NULL),
        (v_item1, v_col_due, '2026-05-18', NULL),
        (v_item1, v_col_tags, NULL, '["Design", "UI"]');

    -- 아이템 밸류 (API 서버 연동)
    INSERT INTO item_values (item_id, column_id, value_text, value_json) VALUES
        (v_item2, v_col_status, '진행 중', NULL),
        (v_item2, v_col_person, NULL, '["d1111111-e9f8-7a6b-5c4d-3e2f1a0b9c8d"]'),
        (v_item2, v_col_start, '2026-05-10', NULL),
        (v_item2, v_col_end, '2026-05-25', NULL),
        (v_item2, v_col_due, '2026-05-28', NULL),
        (v_item2, v_col_tags, NULL, '["Backend", "API"]');

    -- 아이템 밸류 (QA 테스트 진행)
    INSERT INTO item_values (item_id, column_id, value_text, value_json) VALUES
        (v_item3, v_col_status, '시작 전', NULL),
        (v_item3, v_col_person, NULL, '["d2222222-e9f8-7a6b-5c4d-3e2f1a0b9c8d"]'),
        (v_item3, v_col_start, '2026-05-20', NULL),
        (v_item3, v_col_end, '2026-05-30', NULL),
        (v_item3, v_col_due, '2026-06-02', NULL),
        (v_item3, v_col_tags, NULL, '["QA", "Testing"]');

END $$;
