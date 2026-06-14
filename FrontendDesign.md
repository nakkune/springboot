# ProjectOS 프론트엔드 설계 문서 (Vue.js)

> **문서 상태:** 초안 (Draft)
> **작성 목적:** `ProjectMng.md` 및 `Schema.sql` 기반 프론트엔드(Vue.js) 아키텍처 및 UI/UX 설계
> **대상 환경:** 브라우저 (SPA)

---

## 1. 개요 및 설계 철학

본 문서는 Monday.com 수준의 사용성과 시각적 완성도를 목표로 하는 **ProjectOS**의 프론트엔드 설계 명세입니다. 10년 차 시니어 개발자 및 디자이너 관점에서 **최상의 사용자 경험(UX)**, **고성능(Performance)**, 그리고 **확장 가능한 아키텍처(Scalable Architecture)**를 보장하도록 설계되었습니다.

### 1.1 핵심 설계 목표
- **Dynamic & Premium UI**: 단순한 MVP를 넘어, 미세한 애니메이션(Micro-animations), 글래스모피즘(Glassmorphism), 부드러운 그라데이션 및 반응형 인터랙션을 적용한 프리미엄 Work OS 환경 제공.
- **성능 최적화 (Rendering Performance)**: 10,000개 이상의 행을 렌더링해야 하는 Board 뷰의 특성을 고려한 가상 스크롤(Virtual Scrolling) 및 최적화된 DOM 업데이트 처리.
- **모듈화 및 재사용성**: 복잡한 EAV(Entity-Attribute-Value) 구조를 유연하게 렌더링할 수 있는 동적 컴포넌트 아키텍처 채택.

---

## 2. 기술 스택

| 분류 | 기술 및 라이브러리 | 선정 이유 |
|------|-------------------|----------|
| **Core** | Vue 3 (Composition API) | 직관적인 반응성, 로직 재사용성 및 대규모 애플리케이션의 유지보수 용이 |
| **Build Tool** | Vite | 압도적으로 빠른 HMR(Hot Module Replacement) 및 빌드 속도 |
| **State Management**| Pinia | Vue 3에 최적화된 직관적인 상태 관리 및 타입 추론 |
| **Routing** | Vue Router 4 | 중첩 라우팅 및 동적 뷰(Board/Kanban 등) 로드 최적화 |
| **Styling** | Vanilla CSS (Scoped) / CSS Modules | 커스텀 디자인 시스템의 유연한 제어, 트렌디한 시각 효과(다크모드 등) 완벽 통제 |
| **HTTP Client** | Axios | 인터셉터를 통한 인증 토큰 관리, API 응답 통합 처리 |
| **Drag & Drop** | VueDraggablePlus (Sortable.js) | 칸반 보드 및 아이템 순서 변경을 위한 고성능 드래그 앤 드롭 |
| **Icons** | Phosphor Icons / Lucide | 프리미엄 느낌의 모던하고 일관된 벡터 아이콘 |

---

## 3. UI/UX 디자인 시스템

10년 차 시니어 디자이너 관점에서의 **"Wow" 팩터**를 주기 위한 디자인 가이드라인입니다.

### 3.1 Color Palette & Typography
- **Primary Color**: `#4F46E5` (Indigo) ~ `#6366F1` (부드러운 그라데이션 적용)
- **Background**: `#0F172A` (Deep Slate - 다크모드 기본) / `#F8FAFC` (라이트모드)
- **Surface**: 글래스모피즘 효과 (반투명 배경 + 블러 `backdrop-filter: blur(12px)`)
- **Typography**: `Inter`, `Outfit` (구글 폰트 적용). 헤딩은 두껍고 둥근 `Outfit`을, 데이터 보드는 가독성 높은 `Inter`를 사용.

### 3.2 Micro-animations (미세 애니메이션)
- 호버 시 아이템의 `transform: translateY(-2px)` 및 `box-shadow` 변화.
- 보드 뷰 및 칸반 뷰 전환 시 Vue의 `<Transition>`과 `<TransitionGroup>`을 활용한 부드러운 레이아웃 재배치 애니메이션.
- 데이터 저장/동기화 시 상태 표시기에 부드러운 펄스(Pulse) 이펙트 적용.

---

## 4. 프론트엔드 디렉토리 구조 (FSD - Feature-Sliced Design 변형)

확장성을 고려하여 도메인 중심의 디렉토리 구조를 설계합니다.

```text
src/
├── assets/            # 폰트, 글로벌 CSS, 이미지 (index.css)
├── components/        # 전역 공통 컴포넌트 (UI Kit)
│   ├── ui/            # Button, Input, Modal, Dropdown 등 (디자인 시스템)
│   └── layout/        # Sidebar, Header, GlobalNavigationBar
├── features/          # 핵심 도메인별 모듈 (비즈니스 로직 + 컴포넌트)
│   ├── auth/          # 로그인, OAuth, 세션 관리
│   ├── workspaces/    # 워크스페이스, 팀 관리
│   ├── projects/      # 프로젝트 목록, 네비게이션
│   ├── board/         # 메인 Board 뷰, Virtual Scroll 테이블
│   ├── kanban/        # 칸반 뷰, D&D 로직
│   └── automations/   # 자동화 규칙 빌더 UI
├── stores/            # Pinia 글로벌 상태 (authStore, boardStore 등)
├── router/            # Vue Router 설정
├── services/          # API 통신 (Axios 인스턴스, 엔드포인트별 API)
├── utils/             # 날짜 처리, 색상 변환, 포매터 등 공통 유틸
├── views/             # 라우팅 최상위 페이지 단위 컴포넌트
└── App.vue            # 애플리케이션 엔트리
```

---

## 5. 상태 관리 전략 (Pinia)

복잡한 데이터 의존성을 효과적으로 관리하기 위해 도메인별 Store를 분리합니다.

1. **`useAuthStore`**: 유저 세션, 토큰 정보, 내 프로필 데이터.
2. **`useWorkspaceStore`**: 현재 선택된 워크스페이스, 소속된 팀 목록.
3. **`useProjectStore`**: 현재 선택된 프로젝트 및 하위 보드 목록 (Sidebar 렌더링용).
4. **`useBoardStore`**: 
   - **가장 핵심적인 Store**.
   - 현재 보드의 컬럼 정보 (`board_columns`).
   - 그룹 및 아이템 정보 (`board_groups`, `items`).
   - EAV 패턴으로 설계된 값들 (`item_values`)을 정규화(Normalization)하여 프론트엔드에서 테이블 형태로 바인딩하기 쉽게 변환.

---

## 6. 핵심 뷰 및 컴포넌트 설계

### 6.1 Board 뷰 (`features/board/`)
- `Schema.sql`의 `items`와 `board_columns`, `item_values`를 결합하여 스프레드시트 형태의 그리드 구현.
- **성능 문제 해결**: `Vue Virtual Scroller`를 도입하여 10,000개 아이템 중 화면에 보이는 50~100개만 DOM에 렌더링.
- **동적 컬럼 렌더링**: 컬럼의 `column_type` (text, status, person, date 등)에 따라 동적 컴포넌트 `<component :is="columnComponentMap[col.type]" />`로 렌더링.
- **인라인 편집**: 셀(Cell) 클릭 시 즉시 수정 가능한 상태(Edit Mode)로 전환되며 변경 시 API 디바운싱(Debouncing) 적용.

### 6.2 Kanban 뷰 (`features/kanban/`)
- 보드의 상태(Status) 컬럼을 기준으로 그룹화.
- 카드 이동(Drag & Drop) 시 `item_values`의 상태 값을 업데이트하는 API 호출.
- `<TransitionGroup>`을 적용하여 카드 이동 시 스무스한 재정렬 애니메이션 구현.

### 6.3 Automation 빌더 (`features/automations/`)
- `trigger_config`, `condition_config`, `action_config` JSONB 데이터를 생성하는 시각적 빌더 컴포넌트.
- 문장형 인터페이스 차용: "When `[상태]` changes to `[완료]`, then `[알림]` to `[담당자]`" 형태의 드롭다운 조합 UI 제공.

---

## 7. API 및 통신 전략

- **토큰 관리**: 로그인 시 발급받은 JWT를 HttpOnly 쿠키 또는 런타임 메모리에 보관. Axios Request Interceptor를 통해 모든 API 요청 헤더에 `Authorization: Bearer` 주입.
- **낙관적 업데이트 (Optimistic UI)**: 상태 변경(예: 태스크 완료 처리) 시 서버의 응답을 기다리지 않고 Vue의 상태(Pinia)를 즉시 변경하여 딜레이 없는 UX 제공. 서버 응답이 실패할 경우 원래 상태로 롤백.
- **실시간 통신 (추후 확장 고려)**: 팀 협업 툴의 특성상 Server-Sent Events (SSE) 또는 WebSocket(Socket.io)을 연결하여 다른 사용자가 보드 수정 시 실시간 갱신 처리.

---

## 8. SEO 및 접근성 (A11y)
- SPA 특성상 관리자 대시보드가 주력이지만, 메인 랜딩 페이지 및 공유 대시보드(Shareable Dashboard)의 경우 시맨틱 HTML(`header`, `main`, `section`, `article`)을 준수하여 구성.
- 드래그 앤 드롭 요소 및 버튼에 키보드 네비게이션 및 `aria-label`, `role` 속성 지원.
- 공유된 대시보드의 메타 태그 업데이트(Vue Meta 등) 적용.

---
*본 설계 문서는 MVP 개발부터 최종 서비스까지 프론트엔드 팀의 가이드라인으로 사용됩니다.*
