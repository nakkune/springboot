-- =============================================================
-- ProjectOS ? PostgreSQL Schema
-- 참조: Monday.com Work OS 기반 프로젝트 관리 플랫폼
-- 생성일: 2025-05
-- 대상 경로: /home/knh11/spring/boot/schema.sql
-- =============================================================

-- -------------------------------------------------------------
-- 확장 기능
-- -------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================
-- 1. 사용자 & 인증
-- =============================================================

CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),                          -- OAuth 사용 시 NULL 가능
    full_name       VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    timezone        VARCHAR(50)  DEFAULT 'Asia/Seoul',
    role            VARCHAR(20)  DEFAULT 'member',         -- 사용자 권한 ('member' | 'moderator' | 'admin')
    member_status   VARCHAR(20)  DEFAULT 'pending',       -- 가입 승인 상태 ('pending' | 'approved' | 'rejected')
    theme           VARCHAR(20)  DEFAULT 'dark',          -- [NEW] 시스템 테마 스킨 ('dark' | 'light')
    is_active       BOOLEAN      DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW()
);

CREATE TABLE oauth_accounts (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        VARCHAR(30) NOT NULL,                  -- 'google' | 'github' | 'saml'
    provider_uid    VARCHAR(255) NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (provider, provider_uid)
);

-- =============================================================
-- 2. 워크스페이스 & 팀
-- =============================================================

CREATE TABLE workspaces (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,          -- URL 식별자
    logo_url        VARCHAR(500),
    plan            VARCHAR(20)  DEFAULT 'free',           -- 'free' | 'pro' | 'enterprise'
    owner_id        UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE workspace_members (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'member', -- 'owner' | 'admin' | 'member' | 'guest'
    invited_by      UUID        REFERENCES users(id),
    joined_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (workspace_id, user_id)
);

CREATE TABLE teams (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    color           VARCHAR(7),                            -- HEX 색상
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE team_members (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id         UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) DEFAULT 'member',
    joined_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (team_id, user_id)
);

-- =============================================================
-- 3. 프로젝트 (Board 컨테이너)
-- =============================================================

CREATE TABLE projects (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    team_id         UUID        REFERENCES teams(id) ON DELETE SET NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    color           VARCHAR(7),
    icon            VARCHAR(50),
    is_archived     BOOLEAN     DEFAULT FALSE,
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE project_members (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) DEFAULT 'member',          -- 'admin' | 'member' | 'viewer'
    joined_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (project_id, user_id)
);

-- =============================================================
-- 4. 보드 (Board)
-- =============================================================

CREATE TABLE boards (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    board_type      VARCHAR(20) DEFAULT 'main',            -- 'main' | 'private' | 'shareable'
    position        INTEGER     DEFAULT 0,
    is_archived     BOOLEAN     DEFAULT FALSE,
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 5. 컬럼 (Column / Field 정의)
-- =============================================================

CREATE TABLE board_columns (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id        UUID        NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    column_type     VARCHAR(30) NOT NULL,
    -- 'text' | 'status' | 'person' | 'date' | 'number' | 'checkbox'
    -- 'dropdown' | 'file' | 'link' | 'formula' | 'tags' | 'rating'
    settings        JSONB       DEFAULT '{}',              -- 타입별 옵션 (status 색상 등)
    position        INTEGER     DEFAULT 0,
    is_required     BOOLEAN     DEFAULT FALSE,
    is_hidden       BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- status 컬럼 설정 예시:
-- { "options": [{"id":"1","label":"시작 전","color":"#C4C4C4"},
--               {"id":"2","label":"진행 중","color":"#0073EA"},
--               {"id":"3","label":"완료","color":"#00CA72"}] }

-- =============================================================
-- 6. 그룹 & 아이템 (태스크)
-- =============================================================

CREATE TABLE board_groups (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id        UUID        NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    color           VARCHAR(7),
    position        INTEGER     DEFAULT 0,
    is_collapsed    BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE items (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id        UUID        NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    group_id        UUID        NOT NULL REFERENCES board_groups(id) ON DELETE CASCADE,
    parent_item_id  UUID        REFERENCES items(id) ON DELETE CASCADE, -- 서브태스크
    name            VARCHAR(500) NOT NULL,
    position        INTEGER     DEFAULT 0,
    is_archived     BOOLEAN     DEFAULT FALSE,
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 아이템별 컬럼 값 (EAV 패턴)
CREATE TABLE item_values (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id         UUID        NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    column_id       UUID        NOT NULL REFERENCES board_columns(id) ON DELETE CASCADE,
    value_text      TEXT,
    value_number    NUMERIC,
    value_date      DATE,
    value_json      JSONB,                                 -- person(배열), dropdown(다중) 등
    updated_by      UUID        REFERENCES users(id),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (item_id, column_id)
);

-- =============================================================
-- 7. 뷰 (View) ? Board / 칸반 / 타임라인 / 달력 등
-- =============================================================

CREATE TABLE board_views (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id        UUID        NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    view_type       VARCHAR(20) NOT NULL DEFAULT 'board',  -- 'board' | 'kanban' | 'timeline' | 'calendar' | 'chart'
    settings        JSONB       DEFAULT '{}',
    -- 칸반: { "group_by_column_id": "..." }
    -- 타임라인: { "start_column_id": "...", "end_column_id": "..." }
    -- 필터/정렬 등 포함
    position        INTEGER     DEFAULT 0,
    is_default      BOOLEAN     DEFAULT FALSE,
    created_by      UUID        REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 8. 댓글 & 활동 로그
-- =============================================================

CREATE TABLE comments (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id         UUID        NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    parent_id       UUID        REFERENCES comments(id) ON DELETE CASCADE, -- 스레드
    author_id       UUID        NOT NULL REFERENCES users(id),
    body            TEXT        NOT NULL,
    is_edited       BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE comment_mentions (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    comment_id      UUID        NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    mentioned_user  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (comment_id, mentioned_user)
);

CREATE TABLE activity_logs (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID        REFERENCES workspaces(id) ON DELETE SET NULL,
    project_id      UUID        REFERENCES projects(id) ON DELETE SET NULL,
    board_id        UUID        REFERENCES boards(id) ON DELETE SET NULL,
    item_id         UUID        REFERENCES items(id) ON DELETE SET NULL,
    actor_id        UUID        NOT NULL REFERENCES users(id),
    action          VARCHAR(50) NOT NULL,
    -- 'item.create' | 'item.update' | 'item.delete'
    -- 'status.change' | 'assignee.change' | 'comment.add' 등
    meta            JSONB       DEFAULT '{}',              -- before/after 값 등
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 9. 파일 첨부
-- =============================================================

CREATE TABLE attachments (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id         UUID        REFERENCES items(id) ON DELETE CASCADE,
    comment_id      UUID        REFERENCES comments(id) ON DELETE CASCADE,
    uploader_id     UUID        NOT NULL REFERENCES users(id),
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT      NOT NULL,                  -- bytes
    mime_type       VARCHAR(100),
    storage_url     VARCHAR(1000) NOT NULL,                -- S3 / GCS 경로
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    CHECK (item_id IS NOT NULL OR comment_id IS NOT NULL)
);

-- =============================================================
-- 10. 자동화 (Automation)
-- =============================================================

CREATE TABLE automations (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id        UUID        NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    is_active       BOOLEAN     DEFAULT TRUE,
    trigger_config  JSONB       NOT NULL,
    -- { "type": "status_change", "column_id": "...", "to_value": "완료" }
    condition_config JSONB      DEFAULT '{}',
    -- { "type": "assignee_is", "user_id": "..." }
    action_config   JSONB       NOT NULL,
    -- { "type": "notify", "channel": "slack", "target": "#general" }
    run_count       INTEGER     DEFAULT 0,
    last_run_at     TIMESTAMPTZ,
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE automation_logs (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    automation_id   UUID        NOT NULL REFERENCES automations(id) ON DELETE CASCADE,
    item_id         UUID        REFERENCES items(id) ON DELETE SET NULL,
    status          VARCHAR(20) NOT NULL,                  -- 'success' | 'failed' | 'skipped'
    error_message   TEXT,
    executed_at     TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 11. 알림 (Notification)
-- =============================================================

CREATE TABLE notifications (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id       UUID        REFERENCES users(id) ON DELETE SET NULL,
    type            VARCHAR(50) NOT NULL,
    -- 'mention' | 'assigned' | 'due_soon' | 'status_change' | 'automation'
    title           VARCHAR(300) NOT NULL,
    body            TEXT,
    ref_type        VARCHAR(30),                           -- 'item' | 'comment' | 'board'
    ref_id          UUID,
    is_read         BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 12. 대시보드 (Dashboard)
-- =============================================================

CREATE TABLE dashboards (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    is_shared       BOOLEAN     DEFAULT FALSE,
    share_token     VARCHAR(64) UNIQUE,                    -- 공개 공유용 토큰
    created_by      UUID        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE dashboard_widgets (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    dashboard_id    UUID        NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    widget_type     VARCHAR(30) NOT NULL,
    -- 'number' | 'bar_chart' | 'pie_chart' | 'burndown' | 'table' | 'battery'
    title           VARCHAR(200),
    config          JSONB       NOT NULL DEFAULT '{}',     -- 데이터 소스, 컬럼 매핑 등
    pos_x           INTEGER     DEFAULT 0,
    pos_y           INTEGER     DEFAULT 0,
    width           INTEGER     DEFAULT 4,
    height          INTEGER     DEFAULT 3,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 13. 초대 링크
-- =============================================================

CREATE TABLE invitations (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    project_id      UUID        REFERENCES projects(id) ON DELETE CASCADE,
    email           VARCHAR(255),                          -- NULL이면 링크 초대
    token           VARCHAR(64) NOT NULL UNIQUE,
    role            VARCHAR(20) DEFAULT 'member',
    invited_by      UUID        NOT NULL REFERENCES users(id),
    accepted_by     UUID        REFERENCES users(id),
    expires_at      TIMESTAMPTZ NOT NULL,
    accepted_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================
-- 인덱스
-- =============================================================

-- 사용자
CREATE INDEX idx_users_email               ON users(email);

-- 워크스페이스 멤버
CREATE INDEX idx_ws_members_workspace      ON workspace_members(workspace_id);
CREATE INDEX idx_ws_members_user           ON workspace_members(user_id);

-- 프로젝트
CREATE INDEX idx_projects_workspace        ON projects(workspace_id);
CREATE INDEX idx_project_members_project   ON project_members(project_id);
CREATE INDEX idx_project_members_user      ON project_members(user_id);

-- 보드
CREATE INDEX idx_boards_project            ON boards(project_id);

-- 그룹 & 아이템
CREATE INDEX idx_groups_board              ON board_groups(board_id);
CREATE INDEX idx_items_board               ON items(board_id);
CREATE INDEX idx_items_group               ON items(group_id);
CREATE INDEX idx_items_parent              ON items(parent_item_id);
CREATE INDEX idx_item_values_item          ON item_values(item_id);
CREATE INDEX idx_item_values_column        ON item_values(column_id);

-- 댓글
CREATE INDEX idx_comments_item             ON comments(item_id);
CREATE INDEX idx_comments_parent           ON comments(parent_id);

-- 활동 로그
CREATE INDEX idx_activity_workspace        ON activity_logs(workspace_id);
CREATE INDEX idx_activity_item             ON activity_logs(item_id);
CREATE INDEX idx_activity_created          ON activity_logs(created_at DESC);

-- 알림
CREATE INDEX idx_notifications_recipient   ON notifications(recipient_id, is_read, created_at DESC);

-- 자동화
CREATE INDEX idx_automations_board         ON automations(board_id);

-- 대시보드
CREATE INDEX idx_dashboard_workspace       ON dashboards(workspace_id);
CREATE INDEX idx_dashboard_share_token     ON dashboards(share_token);

-- =============================================================
-- updated_at 자동 갱신 트리거
-- =============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated           BEFORE UPDATE ON users           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_workspaces_updated      BEFORE UPDATE ON workspaces      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_teams_updated           BEFORE UPDATE ON teams           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_projects_updated        BEFORE UPDATE ON projects        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_boards_updated          BEFORE UPDATE ON boards          FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_board_columns_updated   BEFORE UPDATE ON board_columns   FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_board_groups_updated    BEFORE UPDATE ON board_groups    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_items_updated           BEFORE UPDATE ON items           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_comments_updated        BEFORE UPDATE ON comments        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_automations_updated     BEFORE UPDATE ON automations     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_dashboards_updated      BEFORE UPDATE ON dashboards      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_dashboard_widgets_updated BEFORE UPDATE ON dashboard_widgets FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- COMMENT ON ? 테이블 & 컬럼 설명
-- =============================================================

-- -------------------------------------------------------------
-- 1. users
-- -------------------------------------------------------------
COMMENT ON TABLE  users                     IS '플랫폼 사용자 계정. OAuth 로그인 시 password_hash는 NULL';
COMMENT ON COLUMN users.id                  IS '사용자 고유 식별자 (UUID v4)';
COMMENT ON COLUMN users.email               IS '로그인 이메일, 전체 유니크';
COMMENT ON COLUMN users.password_hash       IS '비밀번호 해시(bcrypt). OAuth 전용 계정은 NULL';
COMMENT ON COLUMN users.full_name           IS '사용자 표시 이름';
COMMENT ON COLUMN users.avatar_url          IS '프로필 이미지 URL (S3 또는 외부 CDN)';
COMMENT ON COLUMN users.timezone            IS '사용자 로컬 타임존 (기본값: Asia/Seoul)';
COMMENT ON COLUMN users.role                IS '사용자 권한 (member/moderator/admin)';
COMMENT ON COLUMN users.member_status       IS '가입 승인 상태 (pending/approved/rejected)';
COMMENT ON COLUMN users.is_active           IS '계정 활성 여부. FALSE이면 로그인 불가';
COMMENT ON COLUMN users.last_login_at       IS '마지막 로그인 일시';
COMMENT ON COLUMN users.created_at          IS '계정 생성 일시';
COMMENT ON COLUMN users.updated_at          IS '계정 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 2. oauth_accounts
-- -------------------------------------------------------------
COMMENT ON TABLE  oauth_accounts            IS 'OAuth 소셜 로그인 연결 계정 (Google / GitHub / SAML 등)';
COMMENT ON COLUMN oauth_accounts.id         IS 'OAuth 계정 고유 식별자';
COMMENT ON COLUMN oauth_accounts.user_id    IS '연결된 내부 사용자 ID';
COMMENT ON COLUMN oauth_accounts.provider   IS 'OAuth 제공자 식별자 (google | github | saml)';
COMMENT ON COLUMN oauth_accounts.provider_uid IS '제공자 측 사용자 고유 ID';
COMMENT ON COLUMN oauth_accounts.access_token  IS 'OAuth 액세스 토큰 (암호화 저장 권장)';
COMMENT ON COLUMN oauth_accounts.refresh_token IS 'OAuth 리프레시 토큰 (암호화 저장 권장)';
COMMENT ON COLUMN oauth_accounts.expires_at IS '액세스 토큰 만료 일시';
COMMENT ON COLUMN oauth_accounts.created_at IS '소셜 계정 연결 일시';

-- -------------------------------------------------------------
-- 3. workspaces
-- -------------------------------------------------------------
COMMENT ON TABLE  workspaces                IS '최상위 조직 단위. 모든 프로젝트/팀/보드의 루트 컨텍스트';
COMMENT ON COLUMN workspaces.id             IS '워크스페이스 고유 식별자';
COMMENT ON COLUMN workspaces.name           IS '워크스페이스 표시 이름';
COMMENT ON COLUMN workspaces.slug           IS 'URL에 사용되는 고유 슬러그 (예: my-company)';
COMMENT ON COLUMN workspaces.logo_url       IS '워크스페이스 로고 이미지 URL';
COMMENT ON COLUMN workspaces.plan           IS '구독 플랜 (free | pro | enterprise)';
COMMENT ON COLUMN workspaces.owner_id       IS '워크스페이스 최초 생성자 (Owner 권한)';
COMMENT ON COLUMN workspaces.created_at     IS '워크스페이스 생성 일시';
COMMENT ON COLUMN workspaces.updated_at     IS '워크스페이스 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 4. workspace_members
-- -------------------------------------------------------------
COMMENT ON TABLE  workspace_members         IS '워크스페이스 소속 멤버 및 역할 매핑';
COMMENT ON COLUMN workspace_members.id      IS '멤버십 고유 식별자';
COMMENT ON COLUMN workspace_members.workspace_id IS '소속 워크스페이스 ID';
COMMENT ON COLUMN workspace_members.user_id IS '멤버 사용자 ID';
COMMENT ON COLUMN workspace_members.role    IS '워크스페이스 내 역할 (owner | admin | member | guest)';
COMMENT ON COLUMN workspace_members.invited_by IS '초대한 사용자 ID';
COMMENT ON COLUMN workspace_members.joined_at  IS '워크스페이스 참여 일시';

-- -------------------------------------------------------------
-- 5. teams
-- -------------------------------------------------------------
COMMENT ON TABLE  teams                     IS '워크스페이스 내 부서/팀 단위. 프로젝트 접근 권한을 팀 단위로 관리';
COMMENT ON COLUMN teams.id                  IS '팀 고유 식별자';
COMMENT ON COLUMN teams.workspace_id        IS '소속 워크스페이스 ID';
COMMENT ON COLUMN teams.name               IS '팀 표시 이름';
COMMENT ON COLUMN teams.description        IS '팀 설명';
COMMENT ON COLUMN teams.color              IS '팀 대표 색상 (HEX, 예: #FF5733)';
COMMENT ON COLUMN teams.created_by         IS '팀 생성자 사용자 ID';
COMMENT ON COLUMN teams.created_at         IS '팀 생성 일시';
COMMENT ON COLUMN teams.updated_at         IS '팀 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 6. team_members
-- -------------------------------------------------------------
COMMENT ON TABLE  team_members             IS '팀 소속 멤버 매핑';
COMMENT ON COLUMN team_members.id          IS '팀 멤버십 고유 식별자';
COMMENT ON COLUMN team_members.team_id     IS '소속 팀 ID';
COMMENT ON COLUMN team_members.user_id     IS '멤버 사용자 ID';
COMMENT ON COLUMN team_members.role        IS '팀 내 역할 (admin | member)';
COMMENT ON COLUMN team_members.joined_at   IS '팀 참여 일시';

-- -------------------------------------------------------------
-- 7. projects
-- -------------------------------------------------------------
COMMENT ON TABLE  projects                 IS '워크스페이스 내 프로젝트. 보드(Board)들의 컨테이너';
COMMENT ON COLUMN projects.id              IS '프로젝트 고유 식별자';
COMMENT ON COLUMN projects.workspace_id    IS '소속 워크스페이스 ID';
COMMENT ON COLUMN projects.team_id         IS '담당 팀 ID (팀 미지정 시 NULL)';
COMMENT ON COLUMN projects.name            IS '프로젝트 표시 이름';
COMMENT ON COLUMN projects.description     IS '프로젝트 설명';
COMMENT ON COLUMN projects.color           IS '프로젝트 대표 색상 (HEX)';
COMMENT ON COLUMN projects.icon            IS '프로젝트 아이콘 식별자 (이모지 또는 아이콘 코드)';
COMMENT ON COLUMN projects.is_archived     IS '보관 여부. TRUE이면 목록에서 숨김 처리';
COMMENT ON COLUMN projects.created_by      IS '프로젝트 생성자 사용자 ID';
COMMENT ON COLUMN projects.created_at      IS '프로젝트 생성 일시';
COMMENT ON COLUMN projects.updated_at      IS '프로젝트 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 8. project_members
-- -------------------------------------------------------------
COMMENT ON TABLE  project_members          IS '프로젝트 접근 권한이 부여된 멤버 목록';
COMMENT ON COLUMN project_members.id       IS '프로젝트 멤버십 고유 식별자';
COMMENT ON COLUMN project_members.project_id IS '대상 프로젝트 ID';
COMMENT ON COLUMN project_members.user_id  IS '멤버 사용자 ID';
COMMENT ON COLUMN project_members.role     IS '프로젝트 내 역할 (admin | member | viewer)';
COMMENT ON COLUMN project_members.joined_at IS '프로젝트 참여 일시';

-- -------------------------------------------------------------
-- 9. boards
-- -------------------------------------------------------------
COMMENT ON TABLE  boards                   IS '프로젝트 내 업무 보드. 아이템(태스크)들의 직접 컨테이너';
COMMENT ON COLUMN boards.id                IS '보드 고유 식별자';
COMMENT ON COLUMN boards.project_id        IS '소속 프로젝트 ID';
COMMENT ON COLUMN boards.name              IS '보드 표시 이름';
COMMENT ON COLUMN boards.description       IS '보드 설명';
COMMENT ON COLUMN boards.board_type        IS '보드 공개 범위 (main | private | shareable)';
COMMENT ON COLUMN boards.position          IS '프로젝트 내 보드 정렬 순서';
COMMENT ON COLUMN boards.is_archived       IS '보관 여부. TRUE이면 목록에서 숨김';
COMMENT ON COLUMN boards.created_by        IS '보드 생성자 사용자 ID';
COMMENT ON COLUMN boards.created_at        IS '보드 생성 일시';
COMMENT ON COLUMN boards.updated_at        IS '보드 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 10. board_columns
-- -------------------------------------------------------------
COMMENT ON TABLE  board_columns            IS '보드의 열(필드) 정의. 아이템 속성 스키마를 구성';
COMMENT ON COLUMN board_columns.id         IS '컬럼 고유 식별자';
COMMENT ON COLUMN board_columns.board_id   IS '소속 보드 ID';
COMMENT ON COLUMN board_columns.name       IS '컬럼 표시 이름 (예: 상태, 담당자, 마감일)';
COMMENT ON COLUMN board_columns.column_type IS '컬럼 데이터 타입 (text | status | person | date | number | checkbox | dropdown | file | link | formula | tags | rating)';
COMMENT ON COLUMN board_columns.settings   IS '타입별 세부 설정 JSONB. status 예시: {"options":[{"id":"1","label":"진행 중","color":"#0073EA"}]}';
COMMENT ON COLUMN board_columns.position   IS '보드 내 컬럼 좌우 정렬 순서';
COMMENT ON COLUMN board_columns.is_required IS '필수 입력 여부. TRUE이면 아이템 저장 시 값 필요';
COMMENT ON COLUMN board_columns.is_hidden  IS '컬럼 숨김 여부. TRUE이면 뷰에서 표시 안 됨';
COMMENT ON COLUMN board_columns.created_at IS '컬럼 생성 일시';
COMMENT ON COLUMN board_columns.updated_at IS '컬럼 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 11. board_groups
-- -------------------------------------------------------------
COMMENT ON TABLE  board_groups             IS '보드 내 아이템 묶음(그룹). 스프린트·카테고리 등으로 활용';
COMMENT ON COLUMN board_groups.id          IS '그룹 고유 식별자';
COMMENT ON COLUMN board_groups.board_id    IS '소속 보드 ID';
COMMENT ON COLUMN board_groups.title       IS '그룹 표시 이름 (예: 이번 주 작업, 백로그)';
COMMENT ON COLUMN board_groups.color       IS '그룹 헤더 색상 (HEX)';
COMMENT ON COLUMN board_groups.position    IS '보드 내 그룹 수직 정렬 순서';
COMMENT ON COLUMN board_groups.is_collapsed IS '그룹 접힘 여부. TRUE이면 UI에서 아이템 목록 숨김';
COMMENT ON COLUMN board_groups.created_at  IS '그룹 생성 일시';
COMMENT ON COLUMN board_groups.updated_at  IS '그룹 정보 최종 수정 일시';

-- -------------------------------------------------------------
-- 12. items
-- -------------------------------------------------------------
COMMENT ON TABLE  items                    IS '보드의 행(태스크/업무 아이템). parent_item_id로 서브태스크 계층 지원';
COMMENT ON COLUMN items.id                 IS '아이템 고유 식별자';
COMMENT ON COLUMN items.board_id           IS '소속 보드 ID';
COMMENT ON COLUMN items.group_id           IS '소속 그룹 ID';
COMMENT ON COLUMN items.parent_item_id     IS '부모 아이템 ID. NULL이면 최상위 태스크, 값이 있으면 서브태스크';
COMMENT ON COLUMN items.name              IS '아이템 제목(태스크명)';
COMMENT ON COLUMN items.position          IS '그룹 내 아이템 수직 정렬 순서';
COMMENT ON COLUMN items.is_archived       IS '보관 여부. TRUE이면 목록에서 숨김';
COMMENT ON COLUMN items.created_by        IS '아이템 생성자 사용자 ID';
COMMENT ON COLUMN items.created_at        IS '아이템 생성 일시';
COMMENT ON COLUMN items.updated_at        IS '아이템 최종 수정 일시';

-- -------------------------------------------------------------
-- 13. item_values
-- -------------------------------------------------------------
COMMENT ON TABLE  item_values              IS '아이템별 컬럼 값 저장 (EAV 패턴). 컬럼 타입에 따라 해당 value_* 컬럼 사용';
COMMENT ON COLUMN item_values.id           IS '값 레코드 고유 식별자';
COMMENT ON COLUMN item_values.item_id      IS '대상 아이템 ID';
COMMENT ON COLUMN item_values.column_id    IS '대상 컬럼 ID';
COMMENT ON COLUMN item_values.value_text   IS '텍스트·링크·status 라벨 등 문자열 값';
COMMENT ON COLUMN item_values.value_number IS '숫자·금액·진행률 등 숫자 값';
COMMENT ON COLUMN item_values.value_date   IS '날짜(마감일·시작일 등) 값';
COMMENT ON COLUMN item_values.value_json   IS '복합 값 JSONB. person(담당자 배열), dropdown(다중 선택), tags, 파일 메타 등';
COMMENT ON COLUMN item_values.updated_by   IS '값을 마지막으로 수정한 사용자 ID';
COMMENT ON COLUMN item_values.updated_at   IS '값 최종 수정 일시';

-- -------------------------------------------------------------
-- 14. board_views
-- -------------------------------------------------------------
COMMENT ON TABLE  board_views              IS '보드 데이터를 표현하는 뷰 설정 (Board | 칸반 | 타임라인 | 달력 | 차트)';
COMMENT ON COLUMN board_views.id           IS '뷰 고유 식별자';
COMMENT ON COLUMN board_views.board_id     IS '소속 보드 ID';
COMMENT ON COLUMN board_views.name         IS '뷰 표시 이름 (예: 스프린트 칸반, 분기 타임라인)';
COMMENT ON COLUMN board_views.view_type    IS '뷰 유형 (board | kanban | timeline | calendar | chart)';
COMMENT ON COLUMN board_views.settings     IS '뷰별 세부 설정 JSONB. 칸반: {"group_by_column_id":"..."}, 타임라인: {"start_column_id":"...","end_column_id":"..."}, 필터/정렬 포함';
COMMENT ON COLUMN board_views.position     IS '보드 내 뷰 탭 정렬 순서';
COMMENT ON COLUMN board_views.is_default   IS '기본 뷰 여부. TRUE이면 보드 진입 시 이 뷰가 자동 선택';
COMMENT ON COLUMN board_views.created_by   IS '뷰 생성자 사용자 ID';
COMMENT ON COLUMN board_views.created_at   IS '뷰 생성 일시';
COMMENT ON COLUMN board_views.updated_at   IS '뷰 설정 최종 수정 일시';

-- -------------------------------------------------------------
-- 15. comments
-- -------------------------------------------------------------
COMMENT ON TABLE  comments                 IS '아이템에 달린 댓글. parent_id로 스레드(답글) 구조 지원';
COMMENT ON COLUMN comments.id              IS '댓글 고유 식별자';
COMMENT ON COLUMN comments.item_id         IS '댓글이 달린 아이템 ID';
COMMENT ON COLUMN comments.parent_id       IS '답글 대상 댓글 ID. NULL이면 최상위 댓글';
COMMENT ON COLUMN comments.author_id       IS '작성자 사용자 ID';
COMMENT ON COLUMN comments.body            IS '댓글 본문 (마크다운 또는 HTML 허용)';
COMMENT ON COLUMN comments.is_edited       IS '수정 여부. TRUE이면 UI에 "(수정됨)" 표시';
COMMENT ON COLUMN comments.created_at      IS '댓글 작성 일시';
COMMENT ON COLUMN comments.updated_at      IS '댓글 최종 수정 일시';

-- -------------------------------------------------------------
-- 16. comment_mentions
-- -------------------------------------------------------------
COMMENT ON TABLE  comment_mentions         IS '댓글 내 @멘션된 사용자 목록. 알림 발송에 활용';
COMMENT ON COLUMN comment_mentions.id      IS '멘션 레코드 고유 식별자';
COMMENT ON COLUMN comment_mentions.comment_id       IS '멘션이 포함된 댓글 ID';
COMMENT ON COLUMN comment_mentions.mentioned_user   IS '멘션된 사용자 ID';

-- -------------------------------------------------------------
-- 17. activity_logs
-- -------------------------------------------------------------
COMMENT ON TABLE  activity_logs            IS '아이템·보드·프로젝트 변경 이력 감사 로그';
COMMENT ON COLUMN activity_logs.id         IS '로그 고유 식별자';
COMMENT ON COLUMN activity_logs.workspace_id IS '대상 워크스페이스 ID';
COMMENT ON COLUMN activity_logs.project_id IS '대상 프로젝트 ID';
COMMENT ON COLUMN activity_logs.board_id   IS '대상 보드 ID';
COMMENT ON COLUMN activity_logs.item_id    IS '대상 아이템 ID';
COMMENT ON COLUMN activity_logs.actor_id   IS '작업을 수행한 사용자 ID';
COMMENT ON COLUMN activity_logs.action     IS '수행된 작업 유형 (item.create | item.update | status.change | assignee.change | comment.add 등)';
COMMENT ON COLUMN activity_logs.meta       IS '변경 상세 JSONB. before/after 값, 변경된 컬럼명 등 포함';
COMMENT ON COLUMN activity_logs.created_at IS '작업 발생 일시';

-- -------------------------------------------------------------
-- 18. attachments
-- -------------------------------------------------------------
COMMENT ON TABLE  attachments              IS '아이템 또는 댓글에 첨부된 파일 메타데이터. 실제 파일은 오브젝트 스토리지(S3 등)에 저장';
COMMENT ON COLUMN attachments.id           IS '첨부파일 고유 식별자';
COMMENT ON COLUMN attachments.item_id      IS '첨부된 아이템 ID (댓글 첨부 시 NULL)';
COMMENT ON COLUMN attachments.comment_id   IS '첨부된 댓글 ID (아이템 첨부 시 NULL)';
COMMENT ON COLUMN attachments.uploader_id  IS '파일 업로드한 사용자 ID';
COMMENT ON COLUMN attachments.file_name    IS '원본 파일명';
COMMENT ON COLUMN attachments.file_size    IS '파일 크기 (bytes). 최대 100MB 제한 권장';
COMMENT ON COLUMN attachments.mime_type    IS 'MIME 타입 (예: image/png, application/pdf)';
COMMENT ON COLUMN attachments.storage_url  IS '오브젝트 스토리지 경로 또는 서명된 URL';
COMMENT ON COLUMN attachments.created_at   IS '파일 업로드 일시';

-- -------------------------------------------------------------
-- 19. automations
-- -------------------------------------------------------------
COMMENT ON TABLE  automations              IS '보드 단위 자동화 규칙. Trigger → Condition → Action 구조';
COMMENT ON COLUMN automations.id           IS '자동화 규칙 고유 식별자';
COMMENT ON COLUMN automations.board_id     IS '자동화가 적용되는 보드 ID';
COMMENT ON COLUMN automations.name         IS '자동화 규칙 이름 (예: 완료 시 Slack 알림)';
COMMENT ON COLUMN automations.is_active    IS '활성화 여부. FALSE이면 규칙이 실행되지 않음';
COMMENT ON COLUMN automations.trigger_config IS '트리거 조건 JSONB. 예: {"type":"status_change","column_id":"...","to_value":"완료"}';
COMMENT ON COLUMN automations.condition_config IS '추가 조건 JSONB (선택). 예: {"type":"assignee_is","user_id":"..."}';
COMMENT ON COLUMN automations.action_config IS '실행 액션 JSONB. 예: {"type":"notify","channel":"slack","target":"#general"}';
COMMENT ON COLUMN automations.run_count    IS '자동화 누적 실행 횟수';
COMMENT ON COLUMN automations.last_run_at  IS '마지막 실행 일시';
COMMENT ON COLUMN automations.created_by   IS '규칙 생성자 사용자 ID';
COMMENT ON COLUMN automations.created_at   IS '규칙 생성 일시';
COMMENT ON COLUMN automations.updated_at   IS '규칙 최종 수정 일시';

-- -------------------------------------------------------------
-- 20. automation_logs
-- -------------------------------------------------------------
COMMENT ON TABLE  automation_logs          IS '자동화 규칙 실행 이력. 성공/실패 추적 및 디버깅에 활용';
COMMENT ON COLUMN automation_logs.id       IS '실행 로그 고유 식별자';
COMMENT ON COLUMN automation_logs.automation_id IS '실행된 자동화 규칙 ID';
COMMENT ON COLUMN automation_logs.item_id  IS '트리거를 발생시킨 아이템 ID';
COMMENT ON COLUMN automation_logs.status   IS '실행 결과 (success | failed | skipped)';
COMMENT ON COLUMN automation_logs.error_message IS '실패 시 오류 메시지';
COMMENT ON COLUMN automation_logs.executed_at IS '실행 일시';

-- -------------------------------------------------------------
-- 21. notifications
-- -------------------------------------------------------------
COMMENT ON TABLE  notifications            IS '사용자별 인앱 알림. 멘션·할당·마감·자동화 등 이벤트 발생 시 생성';
COMMENT ON COLUMN notifications.id         IS '알림 고유 식별자';
COMMENT ON COLUMN notifications.recipient_id IS '알림 수신자 사용자 ID';
COMMENT ON COLUMN notifications.sender_id  IS '알림을 유발한 사용자 ID (시스템 발송 시 NULL)';
COMMENT ON COLUMN notifications.type       IS '알림 유형 (mention | assigned | due_soon | status_change | automation)';
COMMENT ON COLUMN notifications.title      IS '알림 제목 (UI에 표시)';
COMMENT ON COLUMN notifications.body       IS '알림 본문 상세 내용';
COMMENT ON COLUMN notifications.ref_type   IS '연결된 객체 타입 (item | comment | board)';
COMMENT ON COLUMN notifications.ref_id     IS '연결된 객체 UUID (ref_type에 따라 해당 테이블 조회)';
COMMENT ON COLUMN notifications.is_read    IS '읽음 여부. FALSE이면 미읽음 배지에 포함';
COMMENT ON COLUMN notifications.created_at IS '알림 생성 일시';

-- -------------------------------------------------------------
-- 22. dashboards
-- -------------------------------------------------------------
COMMENT ON TABLE  dashboards               IS '워크스페이스 단위 대시보드. 복수 보드 데이터를 집계하여 시각화';
COMMENT ON COLUMN dashboards.id            IS '대시보드 고유 식별자';
COMMENT ON COLUMN dashboards.workspace_id  IS '소속 워크스페이스 ID';
COMMENT ON COLUMN dashboards.name          IS '대시보드 표시 이름';
COMMENT ON COLUMN dashboards.is_shared     IS '외부 공유 활성화 여부';
COMMENT ON COLUMN dashboards.share_token   IS '비회원 읽기 전용 공유용 랜덤 토큰 (is_shared=TRUE일 때 사용)';
COMMENT ON COLUMN dashboards.created_by    IS '대시보드 생성자 사용자 ID';
COMMENT ON COLUMN dashboards.created_at    IS '대시보드 생성 일시';
COMMENT ON COLUMN dashboards.updated_at    IS '대시보드 최종 수정 일시';

-- -------------------------------------------------------------
-- 23. dashboard_widgets
-- -------------------------------------------------------------
COMMENT ON TABLE  dashboard_widgets        IS '대시보드에 배치된 위젯. 그리드 레이아웃으로 자유 배치 지원';
COMMENT ON COLUMN dashboard_widgets.id     IS '위젯 고유 식별자';
COMMENT ON COLUMN dashboard_widgets.dashboard_id IS '소속 대시보드 ID';
COMMENT ON COLUMN dashboard_widgets.widget_type IS '위젯 유형 (number | bar_chart | pie_chart | burndown | table | battery)';
COMMENT ON COLUMN dashboard_widgets.title  IS '위젯 표시 제목';
COMMENT ON COLUMN dashboard_widgets.config IS '데이터 소스 및 표시 설정 JSONB. 집계 보드 ID·컬럼 매핑·필터 포함';
COMMENT ON COLUMN dashboard_widgets.pos_x  IS '그리드 X축 위치 (열 번호, 0-based)';
COMMENT ON COLUMN dashboard_widgets.pos_y  IS '그리드 Y축 위치 (행 번호, 0-based)';
COMMENT ON COLUMN dashboard_widgets.width  IS '위젯 가로 크기 (그리드 열 단위)';
COMMENT ON COLUMN dashboard_widgets.height IS '위젯 세로 크기 (그리드 행 단위)';
COMMENT ON COLUMN dashboard_widgets.created_at IS '위젯 생성 일시';
COMMENT ON COLUMN dashboard_widgets.updated_at IS '위젯 설정 최종 수정 일시';

-- -------------------------------------------------------------
-- 24. invitations
-- -------------------------------------------------------------
COMMENT ON TABLE  invitations              IS '워크스페이스·프로젝트 초대 링크 및 이메일 초대 관리';
COMMENT ON COLUMN invitations.id           IS '초대 고유 식별자';
COMMENT ON COLUMN invitations.workspace_id IS '초대 대상 워크스페이스 ID';
COMMENT ON COLUMN invitations.project_id   IS '초대 대상 프로젝트 ID (프로젝트 초대 시, 워크스페이스 초대 시 NULL)';
COMMENT ON COLUMN invitations.email        IS '초대받는 이메일. NULL이면 링크 기반 초대';
COMMENT ON COLUMN invitations.token        IS '초대 URL에 포함되는 고유 토큰 (64자 랜덤)';
COMMENT ON COLUMN invitations.role         IS '수락 시 부여될 역할 (owner | admin | member | guest)';
COMMENT ON COLUMN invitations.invited_by   IS '초대를 보낸 사용자 ID';
COMMENT ON COLUMN invitations.accepted_by  IS '초대를 수락한 사용자 ID (미수락 시 NULL)';
COMMENT ON COLUMN invitations.expires_at   IS '초대 링크 만료 일시';
COMMENT ON COLUMN invitations.accepted_at  IS '초대 수락 일시 (미수락 시 NULL)';
COMMENT ON COLUMN invitations.created_at   IS '초대 생성 일시';

-- =============================================================
-- END OF SCHEMA
-- =============================================================