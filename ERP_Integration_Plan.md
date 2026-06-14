# ProjectOS — ERP 시스템 통합 계획서

> **목표**: 기존 Project Management System(ProjectOS)에 전사적 자원 관리(ERP) 모듈을 추가하여, 프로젝트 관리 + 업무 운영(재무/인사/구매/CRM)을 하나의 플랫폼에서 통합 제공

---

## 1. 현재 시스템 분석 (As-Is)

### 1.1 기술 스택

| 계층 | 기술 | 버전 |
|------|------|------|
| Backend | Spring Boot / Java | 3.5 / 21 |
| ORM | MyBatis | 3.0.5 |
| DB | PostgreSQL | via JDBC |
| Auth | JWT (jjwt 0.11.5) + Spring Security | - |
| Frontend | Vue 3 + TypeScript | 3.5 / 6.0 |
| State | Pinia | 3.0 |
| Router | Vue Router | 5.0 |
| UI | Tailwind CSS 4 + Lucide Icons | 4.3 / 1.0 |
| Build | Vite | 8.0 |

### 1.2 현재 모듈 구조

```
com.nak.backend/
├── config/          # SecurityConfig, CorsConfig, MyBatisConfig
├── security/        # JwtAuthenticationFilter, JwtTokenProvider
├── user/            # User CRUD, login/signup, profile
├── workspace/       # Workspace + members (owner/admin/member/guest)
├── project/         # Project container + members
├── board/           # Board + columns + groups (EAV item_values)
├── item/            # Items (tasks) + values
├── team/            # Teams + team members
├── activity/        # Activity/audit logs
├── attachment/      # File uploads (local/S3)
├── automation/      # Trigger → Condition → Action rules
└── notification/    # In-app notifications + SSE
```

### 1.3 현재 데이터 모델 (핵심)

```
users ── workspace_members ── workspaces
  │                            │
  │                       projects ── project_members
  │                            │
  │                         boards
  │                       ───┬───
  │                    │          │
  │              board_columns  board_groups
  │                    │          │
  │               item_values ── items ── comments
  │                                   │
  │                              attachments
  │
  └── notifications, activity_logs, automations
```

### 1.4 Frontend 라우트

| Path | View | 설명 |
|------|------|------|
| `/login`, `/signup` | LoginView, SignupView | 인증 |
| `/main` | MainView | 워크스페이스 메인 |
| `/board/:id` | BoardView | 보드 (Table/Kanban/Timeline/Calendar/Dashboard) |
| `/dashboard` | DashboardView | 워크스페이스 대시보드 |
| `/profile` | ProfileView | 사용자 프로필 |
| `/admin` | AdminView | 관리자 콘솔 |
| `-` | SettingsPopup | 설정 팝업 (오버레이) |

---

## 2. ERP 통합 아키텍처 (To-Be)

### 2.1 설계 원칙

1. **기존 PM 기능은 100% 유지** — ERP는 별도 모듈로 추가, 기존 코드 수정 최소화
2. **모듈별 독립성** — 각 ERP 도메인(재무/인사/구매/CRM)은 독립된 패키지 + DB 테이블 세트
3. **PM ↔ ERP 양방향 연동** — 프로젝트 아이템과 ERP 레코드 간 링크 (예: 태스크 → 구매발주서)
4. **동일한 인증/권한 체계** — 기존 JWT + workspace 멤버십을 ERP까지 확장
5. **일관된 UI 패턴** — 기존 Sidebar + Router + Pinia 패턴 유지
6. **단계적 롤아웃** — Phase 1: CRM + 구매 → Phase 2: 회계 → Phase 3: 인사

### 2.2 전체 시스템 구성도

```
┌─────────────────────────────────────────────────────┐
│                   Frontend (Vue 3)                    │
│  ┌─────────┬──────────┬──────┬──────┬─────────────┐  │
│  │  Main   │  Board   │ ERP  │ ERP  │  Admin      │  │
│  │  View   │  View    │Menu  │ Detail│  Console    │  │
│  ├─────────┴──────────┴──────┴──────┴─────────────┤  │
│  │           Pinia Stores (PM + ERP modules)        │  │
│  └───────────────────┬─────────────────────────────┘  │
│                      │ REST API (JWT)                  │
├──────────────────────┼──────────────────────────────┤
│  Backend (Spring)    │                               │
│  ┌───────────────────┴────────────────────────────┐  │
│  │         Security (JWT Filter)                   │  │
│  ├────────────────────────────────────────────────┤  │
│  │  PM Modules  │       ERP Modules                │  │
│  │  ──────────  │       ───────────                │  │
│  │  user/       │  ┌─ erp/crm/        (Phase 1)   │  │
│  │  workspace/  │  ├─ erp/procurement/ (Phase 1)   │  │
│  │  project/    │  ├─ erp/finance/     (Phase 2)   │  │
│  │  board/      │  ├─ erp/hr/          (Phase 3)   │  │
│  │  item/       │  └─ erp/common/                  │  │
│  │  team/       │                                   │  │
│  │  ...         │                                   │  │
│  ├────────────────────────────────────────────────┤  │
│  │         PostgreSQL (PM schema + ERP schema)     │  │
│  └────────────────────────────────────────────────┘  │
```

---

## 3. ERP 모듈별 상세 설계

### Phase 1: CRM + 구매/자재 (우선 순위 최상)

#### 3.1 CRM 모듈

**목적**: 고객/거래처 관리, 영업 파이프라인, 견적/계약 관리

**DB Tables**:

```sql
-- 3.1.1 고객/거래처
CREATE TABLE erp_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    account_type    VARCHAR(20) NOT NULL DEFAULT 'customer', -- 'customer' | 'vendor' | 'both'
    company_name    VARCHAR(300) NOT NULL,
    business_no     VARCHAR(50),                              -- 사업자등록번호
    ceo_name        VARCHAR(100),
    industry        VARCHAR(200),
    phone           VARCHAR(50),
    email           VARCHAR(255),
    address         TEXT,
    status          VARCHAR(20) DEFAULT 'active',             -- 'active' | 'inactive' | 'lead'
    tags            JSONB DEFAULT '[]',
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.1.2 담당자 (거래처 내 연락처)
CREATE TABLE erp_contacts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id      UUID NOT NULL REFERENCES erp_accounts(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    department      VARCHAR(100),
    position        VARCHAR(100),
    phone           VARCHAR(50),
    email           VARCHAR(255),
    is_primary      BOOLEAN DEFAULT FALSE,
    memo            TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.1.3 영업 기회 (파이프라인)
CREATE TABLE erp_opportunities (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    account_id      UUID NOT NULL REFERENCES erp_accounts(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES erp_contacts(id) ON DELETE SET NULL,
    name            VARCHAR(300) NOT NULL,
    stage           VARCHAR(30) NOT NULL DEFAULT 'prospecting',
    -- 'prospecting' | 'qualification' | 'proposal' | 'negotiation' | 'closed_won' | 'closed_lost'
    amount          NUMERIC(15,2) DEFAULT 0,
    probability     INTEGER DEFAULT 0,                        -- 0-100
    expected_close_date DATE,
    pipeline_id     UUID REFERENCES erp_pipelines(id),
    assigned_to     UUID REFERENCES users(id),
    description     TEXT,
    linked_item_id  UUID REFERENCES items(id) ON DELETE SET NULL, -- PM 연동
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.1.4 영업 파이프라인 설정
CREATE TABLE erp_pipelines (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    stages          JSONB NOT NULL, -- [{ "key": "prospecting", "name": "발굴", "color": "#..." }]
    is_default      BOOLEAN DEFAULT FALSE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.1.5 견적서
CREATE TABLE erp_quotes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quote_no        VARCHAR(50) NOT NULL UNIQUE,               -- 자동 채번 (QT-2026-0001)
    account_id      UUID NOT NULL REFERENCES erp_accounts(id),
    contact_id      UUID REFERENCES erp_contacts(id),
    opportunity_id  UUID REFERENCES erp_opportunities(id) ON DELETE SET NULL,
    status          VARCHAR(20) DEFAULT 'draft',               -- 'draft' | 'sent' | 'accepted' | 'rejected'
    subtotal        NUMERIC(15,2) DEFAULT 0,
    discount_rate   NUMERIC(5,2) DEFAULT 0,
    tax_amount      NUMERIC(15,2) DEFAULT 0,
    total_amount    NUMERIC(15,2) DEFAULT 0,
    valid_until     DATE,
    description     TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.1.6 견적 항목
CREATE TABLE erp_quote_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quote_id        UUID NOT NULL REFERENCES erp_quotes(id) ON DELETE CASCADE,
    product_name    VARCHAR(300) NOT NULL,
    description     TEXT,
    quantity        NUMERIC(10,2) DEFAULT 1,
    unit_price      NUMERIC(15,2) DEFAULT 0,
    tax_rate        NUMERIC(5,2) DEFAULT 10,
    amount          NUMERIC(15,2) DEFAULT 0,
    sort_order      INTEGER DEFAULT 0
);

-- 3.1.7 계약
CREATE TABLE erp_contracts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    contract_no     VARCHAR(50) NOT NULL UNIQUE,
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    account_id      UUID NOT NULL REFERENCES erp_accounts(id),
    quote_id        UUID REFERENCES erp_quotes(id) ON DELETE SET NULL,
    opportunity_id  UUID REFERENCES erp_opportunities(id) ON DELETE SET NULL,
    status          VARCHAR(20) DEFAULT 'active',              -- 'active' | 'completed' | 'terminated'
    start_date      DATE NOT NULL,
    end_date        DATE,
    contract_amount NUMERIC(15,2) DEFAULT 0,
    description     TEXT,
    linked_item_id  UUID REFERENCES items(id) ON DELETE SET NULL,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

#### 3.2 구매/자재 모듈

**목적**: 발주/구매 요청, 재고 관리, 협력사 관리

**DB Tables**:

```sql
-- 3.2.1 상품/자재 마스터
CREATE TABLE erp_products (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_code    VARCHAR(100) NOT NULL,
    product_name    VARCHAR(300) NOT NULL,
    category        VARCHAR(100),
    unit            VARCHAR(20) DEFAULT 'EA',                  -- 'EA' | 'KG' | 'BOX' | 'M'
    purchase_price  NUMERIC(15,2) DEFAULT 0,
    sale_price      NUMERIC(15,2) DEFAULT 0,
    stock_quantity  NUMERIC(12,2) DEFAULT 0,
    min_stock       NUMERIC(12,2) DEFAULT 0,                   -- 안전재고
    stock_location  VARCHAR(200),
    description     TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.2.2 구매 요청
CREATE TABLE erp_purchase_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pr_no           VARCHAR(50) NOT NULL UNIQUE,
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    requester_id    UUID NOT NULL REFERENCES users(id),
    status          VARCHAR(20) DEFAULT 'draft',               -- 'draft' | 'pending' | 'approved' | 'ordered' | 'received'
    description     TEXT,
    linked_item_id  UUID REFERENCES items(id) ON DELETE SET NULL, -- PM 연동
    approved_by     UUID REFERENCES users(id),
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.2.3 구매 요청 항목
CREATE TABLE erp_pr_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_request_id UUID NOT NULL REFERENCES erp_purchase_requests(id) ON DELETE CASCADE,
    product_id      UUID REFERENCES erp_products(id),
    product_name    VARCHAR(300) NOT NULL,
    quantity        NUMERIC(12,2) NOT NULL,
    unit_price      NUMERIC(15,2),
    received_qty    NUMERIC(12,2) DEFAULT 0,
    memo            TEXT,
    sort_order      INTEGER DEFAULT 0
);

-- 3.2.4 발주서
CREATE TABLE erp_purchase_orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    po_no           VARCHAR(50) NOT NULL UNIQUE,
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    vendor_id       UUID NOT NULL REFERENCES erp_accounts(id),   -- 거래처 (vendor)
    purchase_request_id UUID REFERENCES erp_purchase_requests(id),
    status          VARCHAR(20) DEFAULT 'draft',
    -- 'draft' | 'sent' | 'confirmed' | 'shipped' | 'partially_received' | 'received' | 'cancelled'
    expected_date   DATE,
    total_amount    NUMERIC(15,2) DEFAULT 0,
    description     TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3.2.5 발주 항목
CREATE TABLE erp_po_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_order_id UUID NOT NULL REFERENCES erp_purchase_orders(id) ON DELETE CASCADE,
    product_id      UUID REFERENCES erp_products(id),
    product_name    VARCHAR(300) NOT NULL,
    quantity        NUMERIC(12,2) NOT NULL,
    unit_price      NUMERIC(15,2) DEFAULT 0,
    received_qty    NUMERIC(12,2) DEFAULT 0,
    amount          NUMERIC(15,2) DEFAULT 0,
    sort_order      INTEGER DEFAULT 0
);

-- 3.2.6 입고
CREATE TABLE erp_receivings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    po_id           UUID NOT NULL REFERENCES erp_purchase_orders(id) ON DELETE CASCADE,
    received_by     UUID NOT NULL REFERENCES users(id),
    received_at     TIMESTAMPTZ DEFAULT NOW(),
    memo            TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE erp_receiving_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    receiving_id    UUID NOT NULL REFERENCES erp_receivings(id) ON DELETE CASCADE,
    po_item_id      UUID NOT NULL REFERENCES erp_po_items(id),
    product_id      UUID REFERENCES erp_products(id),
    quantity        NUMERIC(12,2) NOT NULL,
    unit_price      NUMERIC(15,2) DEFAULT 0
);
```

---

### Phase 2: 재무/회계

#### 3.3 회계 모듈

**목적**: 전표 관리, 매출/매입 관리, 계정과목 관리

**핵심 Tables**:

| 테이블 | 설명 |
|--------|------|
| `erp_accounting_periods` | 회계 기간 (년/월, 마감 여부) |
| `erp_account_categories` | 계정과목 코드표 |
| `erp_journal_entries` | 분개장 (전표 번호, 날짜, 금액, 차변/대변) |
| `erp_invoices` | 매출/매입 세금계산서 |
| `erp_payments` | 지급/수금 내역 |
| `erp_budgets` | 예산 편성 및 집행 관리 |

---

### Phase 3: 인사/급여

#### 3.4 HR 모듈

**목적**: 직원 정보, 근태, 연차, 급여, 평가

**핵심 Tables**:

| 테이블 | 설명 |
|--------|------|
| `erp_employees` | 직원 마스터 (users와 1:1 확장) |
| `erp_departments` | 부서/조직도 |
| `erp_attendance` | 출퇴근 기록 |
| `erp_leave_requests` | 연차/휴가 신청 |
| `erp_payrolls` | 급여 명세서 |
| `erp_performance_reviews` | 인사 평가 |

---

## 4. PM ↔ ERP 연동 설계

### 4.1 아이템-ERP 레코드 링크

기존 PM 아이템과 ERP 레코드를 연결하여 태스크 기반으로 업무 처리:

```sql
-- 공통 링크 테이블
CREATE TABLE erp_item_links (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id         UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    entity_type     VARCHAR(50) NOT NULL,   -- 'opportunity' | 'quote' | 'contract' | 'purchase_request' | 'purchase_order' | 'invoice'
    entity_id       UUID NOT NULL,          -- 해당 ERP 테이블의 PK
    link_type       VARCHAR(20) DEFAULT 'related',  -- 'related' | 'parent' | 'child'
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (entity_type, entity_id)
);
```

### 4.2 보드 컬럼 확장

기존 `board_columns.column_type`에 ERP 타입 추가:

| Column Type | 설명 |
|-------------|------|
| `erp_account` | 거래처 선택 (erp_accounts에서 조회) |
| `erp_product` | 상품 선택 (erp_products에서 조회) |
| `erp_link` | PM 아이템 ↔ ERP 레코드 연결 |

### 4.3 활동 로그 통합

기존 `activity_logs` 테이블의 `action` 값 확장:

```
erp.opportunity.create
erp.opportunity.stage_change
erp.quote.sent
erp.purchase_order.confirm
erp.purchase_order.receive
erp.contract.sign
```

---

## 5. Frontend ERP UI 설계

### 5.1 네비게이션 구조

```
Sidebar (기존 + ERP 메뉴 그룹)
├── Main
├── Projects (기존)
├── ─────────
├── [ERP] Sales          ← 신규
│   ├── Dashboard
│   ├── Accounts
│   ├── Opportunities
│   ├── Quotes
│   └── Contracts
├── [ERP] Purchasing     ← 신규
│   ├── Dashboard
│   ├── Products
│   ├── Purchase Requests
│   └── Purchase Orders
├── [ERP] Finance        ← Phase 2
└── ─────────
├── Dashboard
├── Admin
└── Settings
```

### 5.2 신규 라우트

```typescript
// ERP Routes (Phase 1)
{ path: 'erp/sales',          component: ErpSalesLayout.vue, children: [
  { path: '',                  component: ErpSalesDashboard.vue },
  { path: 'accounts',          component: ErpAccountList.vue },
  { path: 'accounts/:id',      component: ErpAccountDetail.vue },
  { path: 'opportunities',     component: ErpOpportunityList.vue },
  { path: 'opportunities/:id', component: ErpOpportunityDetail.vue },
  { path: 'quotes',            component: ErpQuoteList.vue },
  { path: 'quotes/:id',        component: ErpQuoteDetail.vue },
  { path: 'contracts',         component: ErpContractList.vue },
]}
{ path: 'erp/purchasing',     component: ErpPurchasingLayout.vue, children: [
  { path: '',                  component: ErpPurchasingDashboard.vue },
  { path: 'products',          component: ErpProductList.vue },
  { path: 'products/:id',      component: ErpProductDetail.vue },
  { path: 'requests',          component: ErpPurchaseRequestList.vue },
  { path: 'orders',            component: ErpPurchaseOrderList.vue },
  { path: 'orders/:id',        component: ErpPurchaseOrderDetail.vue },
]}
```

### 5.3 Pinia Stores 패턴

```typescript
// stores/erp/useErpAccountStore.ts
export const useErpAccountStore = defineStore('erp-account', () => {
  const accounts = ref<ErpAccount[]>([])
  const loading = ref(false)

  async function fetchAccounts(workspaceId: string) { ... }
  async function createAccount(data: CreateAccountDto) { ... }

  return { accounts, loading, fetchAccounts, createAccount }
})
```

### 5.4 공통 컴포넌트 재사용

| 기존 컴포넌트 | ERP 활용 |
|--------------|----------|
| `ItemDetailPanel.vue` | ERP 레코드 상세 패널로 재사용 |
| `RichTextEditor.vue` | 견적서/계약서 본문 편집 |
| `ConfirmModal.vue` | 발주 승인/반려 확인 |
| `NotificationDropdown.vue` | ERP 알림 통합 (견적 도착, 발주 승인 필요) |
| `SettingsPopup.vue` | ERP 설정 탭 추가 (파이프라인, 계정과목) |

### 5.5 ERP 특화 UI 컴포넌트 (신규)

| 컴포넌트 | 설명 |
|---------|------|
| `ErpDataGrid.vue` | ERP 목록 조회 (검색/필터/정렬/페이지네이션 + 엑셀 다운로드) |
| `ErpFormPanel.vue` | ERP 전표/문서 입력 폼 (헤더 + 라인 아이템 구조) |
| `ErpLineItemsEditor.vue` | 견적/발주 항목 라인 에디터 (실시간 합계 계산) |
| `ErpKanbanPipeline.vue` | 영업 파이프라인 칸반 뷰 (KanbanBoard.vue 기반) |
| `ErpChartWidget.vue` | 매출/구매 통계 차트 |
| `AccountSelector.vue` | 거래처 검색/선택 컴포넌트 (기존 PersonColumn 패턴) |
| `ProductSelector.vue` | 상품 검색/선택 컴포넌트 |

---

## 6. Backend 구현 패턴

### 6.1 패키지 구조 (Phase 1 예시)

```
com.nak.backend.erp/
├── config/
│   └── ErpMyBatisConfig.java        (선택적: 별도 DB 세션)
├── common/
│   ├── dto/ErpPageRequest.java      (공통 페이징)
│   ├── dto/ErpPageResponse.java
│   └── util/DocumentNoGenerator.java (전표번호 자동 채번)
├── crm/
│   ├── controller/
│   │   ├── AccountController.java
│   │   ├── ContactController.java
│   │   ├── OpportunityController.java
│   │   ├── QuoteController.java
│   │   └── ContractController.java
│   ├── dto/       (AccountDto, OpportunityDto, QuoteDto ...)
│   ├── mapper/    (AccountMapper.xml, QuoteMapper.xml ...)
│   └── service/   (AccountService, OpportunityService ...)
├── procurement/
│   ├── controller/
│   │   ├── ProductController.java
│   │   ├── PurchaseRequestController.java
│   │   └── PurchaseOrderController.java
│   ├── dto/
│   ├── mapper/
│   └── service/
└── integration/
    ├── ItemLinkService.java          (PM ↔ ERP 연결)
    └── ActivitySyncService.java      (활동 로그 연동)
```

### 6.2 컨트롤러 패턴 (기존과 동일)

```java
@RestController
@RequestMapping("/erp/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(
        @RequestParam String workspaceId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(accountService.getAccounts(workspaceId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto dto) {
        return ResponseEntity.ok(accountService.createAccount(dto));
    }
}
```

### 6.3 권한 체계 확장

기존 `workspace_members.role`을 ERP까지 확장:

| Role | PM 접근 | CRM 접근 | 구매 접근 | 회계 접근 |
|------|---------|---------|---------|---------|
| owner | 전체 | 전체 | 전체 | 전체 |
| admin | 전체 | 전체 | 전체 | 제한 |
| member | 일반 | 일반 | 요청 가능 | 불가 |
| guest | 읽기 | 읽기 | 불가 | 불가 |

ERP 전용 권한을 세분화하려면 별도 권한 테이블 추가:

```sql
CREATE TABLE erp_module_permissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module          VARCHAR(30) NOT NULL,  -- 'crm' | 'procurement' | 'finance' | 'hr'
    permission      VARCHAR(20) NOT NULL,  -- 'view' | 'create' | 'approve' | 'admin'
    created_by      UUID NOT NULL REFERENCES users(id),
    UNIQUE (workspace_id, user_id, module)
);
```

---

## 7. 개발 로드맵

### Phase 1 (4-6주) — CRM + 구매/자재

| 주차 | 작업 |
|------|------|
| 1주차 | DB 스키마 생성, Backend: erp/common (번호생성기, 공통 DTO), Account CRUD |
| 2주차 | Backend: Opportunity (파이프라인), Quote, Contract CRUD |
| 3주차 | Backend: Product, PurchaseRequest, PurchaseOrder CRUD + 입고 |
| 4주차 | Frontend: Sidebar ERP 메뉴, 라우트, ErpDataGrid 공통 컴포넌트 |
| 5주차 | Frontend: CRM 페이지 (Accounts, Opportunities, Quotes) |
| 6주차 | Frontend: 구매 페이지 (Products, PR, PO) + PM 연동 (item 링크) + 테스트 |

### Phase 2 (3-4주) — 재무/회계

| 주차 | 작업 |
|------|------|
| 1주차 | 계정과목, 회계기간, 전표 CRUD |
| 2주차 | 매출/매입 전표, 세금계산서 |
| 3주차 | 예산 관리, Frontend 재무 페이지 |

### Phase 3 (3-4주) — 인사/급여

| 주차 | 작업 |
|------|------|
| 1주차 | 직원 마스터, 부서 관리, 근태 |
| 2주차 | 연차/휴가 신청 워크플로우 |
| 3주차 | 급여 명세서, 평가 |

---

## 8. 리스크 및 고려사항

### 8.1 데이터 격리
- ERP 데이터는 기본적으로 workspace 단위로 격리하나, 단일 기업 영업 대상 모듈(견적서/세금계산서 등)은 예외로 합니다.
- 민감 정보(급여, 매출)는 별도 암호화 컬럼 또는 애플리케이션 레벨 암호화 고려

### 8.2 성능
- ERP는 대량 데이터(거래처 수천, 전표 수만)를 다루므로 페이지네이션 필수
- 검색을 위한 Full-text index (PostgreSQL `tsvector`) 도입 검토
- 대시보드 집계는 별도 Materialized View 또는 Redis 캐싱 고려

### 8.3 감사 추적
- 모든 ERP 트랜잭션은 변경 이력 추적 필수 (created_by, updated_at 기본 포함)
- 중요 문서(견적, 계약, 발주)는 SOFT DELETE 불가, status 기반 취소 처리

### 8.4 프론트엔드 번들 크기
- ERP 모듈은 Lazy Loading (`() => import()`) 으로 분할
- Phase별로 별도 청크로 빌드되어 메인 앱 번들에 영향 최소화

---

## 9. 결론

**ProjectOS는 현재의 PM 시스템 위에 ERP 모듈을 추가하기에 이상적인 구조**입니다:

1. **모듈형 백엔드** — 각 도메인이 독립된 controller/service/mapper 패키지로 이미 분리되어 있어 ERP 모듈 추가가 자연스러움
2. **EAV 데이터 모델** — `board_columns` + `item_values` 패턴을 그대로 ERP 필드 확장에 활용 가능
3. **일관된 인증** — JWT + workspace 멤버십을 ERP까지 동일하게 적용 가능
4. **확장 가능한 프론트엔드** — Vue Router lazy loading + Pinia 모듈형 스토어로 ERP 화면 추가 용이
5. **Workspace 기반 멀티테넌시** — 일반 ERP 데이터가 workspace_id로 격리되어 기업 단위 분리 가능 (단일 기업 대상 영업 모듈 예외)

**권장 접근법**: Phase 1 (CRM + 구매)를 MVP로 먼저 개발하여 실사용 피드백을 받은 후, Phase 2/3로 확장하는 것을 제안합니다.
