# ProjectOS 프론트엔드 개발 작업 내역 (working.md)

> **작성일:** 2026-05-26 | **작업 단계:** Phase 2 완료 | **백엔드 포트:** 9090

---

## 📁 프로젝트 구조

```
/home/knh11/spring/boot/
├── ProjectMng.md          # 프로젝트 기획 문서 (PRD)
├── Schema.sql             # PostgreSQL DB 스키마
├── FrontendDesign.md      # 프론트엔드 설계 문서 (Vue.js 기반)
├── implementation_plan.md # Phase 2 구현 계획서
├── working.md             # 현재 파일: 전체 작업 내역 정리
├── backend/               # Spring Boot 백엔드 (포트: 9090)
│   └── src/main/java/com/nak/backend/
│       ├── board/         # BoardController, BoardColumnController, BoardGroupController
│       ├── item/          # ItemController, ItemValueController
│       ├── project/       # ProjectController
│       ├── workspace/     # WorkspaceController
│       ├── team/          # TeamController
│       └── user/          # UserController
└── frontend/              # Vue.js 프론트엔드 (포트: 5173)
    ├── vite.config.ts
    ├── src/
    │   ├── style.css              # TailwindCSS v4 + 디자인 시스템
    │   ├── main.ts                # Pinia, Vue Router 등록
    │   ├── App.vue                # 앱 엔트리 (router-view)
    │   ├── router/index.ts        # 라우팅 설정
    │   ├── services/api.ts        # Axios 인스턴스 (baseURL: localhost:9090)
    │   ├── stores/
    │   │   └── useBoardStore.ts   # Pinia Store (Lazy Loading)
    │   ├── components/
    │   │   └── layout/
    │   │       ├── Sidebar.vue    # 접이식 사이드바 메뉴
    │   │       └── TopHeader.vue  # 상단 헤더 (검색, 알림, 프로필)
    │   ├── views/
    │   │   ├── MainLayout.vue     # 전체 레이아웃 (Sidebar + Header + main)
    │   │   └── BoardView.vue      # 보드 페이지 (탭: MainTable / Kanban)
    │   └── features/
    │       └── board/
    │           ├── BoardGrid.vue  # 스프레드시트형 테이블 뷰 + D&D
    │           └── KanbanBoard.vue # 칸반 뷰 + D&D
```

---

## ✅ Phase 1 완료 내역 (MVP - Board 뷰)

### 1. 기반 설정 및 라이브러리 설치
| 패키지 | 버전 | 용도 |
|--------|------|------|
| `vue-router` | 4.x | 라우팅 |
| `pinia` | 최신 | 상태 관리 |
| `axios` | 최신 | HTTP 통신 (baseURL: `http://localhost:9090`) |
| `lucide-vue-next` | 최신 | 아이콘 |
| `tailwindcss` | v4.0 | 유틸리티 CSS |
| `@tailwindcss/vite` | 최신 | Vite Tailwind 플러그인 |

### 2. TailwindCSS v4 설정
- `style.css`에서 `@import "tailwindcss"` + `@theme`, `@layer` 지시어로 커스텀 디자인 변수 정의
- 커스텀 색상: `--color-primary: #4F46E5`, 다크 배경: `#0F172A`
- 글래스모피즘 `.glass` 유틸리티 추가 (`backdrop-filter: blur(12px)`)
- `vite.config.ts`에 `@tailwindcss/vite` 플러그인 및 `@` 경로 별칭 추가

> ⚠️ **트러블슈팅**: TailwindCSS v4에서 `<style scoped>` 내부의 `@apply` 사용 시 `unknown utility class` 에러 발생 → `<style>` 블록 제거 후 클래스를 템플릿에 직접 인라인으로 적용하여 해결.

### 3. 핵심 컴포넌트 구현
- **`Sidebar.vue`**: 클릭 시 width 확대/축소 애니메이션이 있는 접이식 사이드 네비게이션
- **`TopHeader.vue`**: 전체 검색창, 알림 뱃지, 유저 아바타 표시
- **`MainLayout.vue`**: Sidebar(좌) + TopHeader(상) + `<router-view>`(우) 레이아웃
- **`BoardGrid.vue`**: 스프레드시트형 테이블. 컬럼 타입(`status`, `person`, `date`)별 동적 셀 렌더링

---

## ✅ Phase 2 완료 내역 (칸반, D&D, API 연동)

### 1. 드래그 앤 드롭 패키지 설치
```bash
npm install vuedraggable@next
```
- `vuedraggable@next` (Sortable.js 기반)를 설치하여 Board Grid 및 Kanban Board 양쪽에 모두 적용.

### 2. Pinia Store + Lazy Loading (`useBoardStore.ts`)
```
클릭 시 데이터 로드 흐름:
사용자가 보드 클릭 → fetchBoardData(boardId) 호출 
→ Promise.all로 4개 API 병렬 호출
   ├── GET /boards/{boardId}
   ├── GET /board-columns/board/{boardId}
   ├── GET /board-groups/board/{boardId}
   └── GET /items/board/{boardId}
→ Pinia 상태 업데이트 (columns, groups, items)
```
- 백엔드가 비어있거나 실패할 경우 `.catch(() => null/[])` 폴백 처리
- `moveItem()` 액션: 낙관적 업데이트(Optimistic UI) 후 `PUT /items/{id}` 호출

### 3. 칸반(Kanban) 뷰 (`KanbanBoard.vue`)
- `board.columns`에서 `status` 타입 컬럼 기준으로 **시작 전 / 진행 중 / 완료** 3개 컬럼 자동 생성
- 각 컬럼은 `vuedraggable`로 래핑되어 카드 간 드래그 앤 드롭으로 상태 변경 가능
- 카드 이동 시 `onChange` 핸들러에서 콘솔 로그 후 API 연동 (실제 서버 구동 시 주석 해제)
- 카드에 담당자 이니셜 아바타 + 마감일 표시

### 4. BoardGrid D&D 추가
- 기존 `v-for` 아이템 렌더링 → `<draggable>` 컴포넌트로 교체
- `handle=".drag-handle"` 속성으로 hover 시 나타나는 핸들 영역 지정
- 그룹 간 아이템 이동 시 `@change` 이벤트 emit

### 5. BoardView.vue 리팩토링
- Mock 데이터 인라인 제거 → `useBoardStore`로 이관
- `activeTab` 상태에 따라 `<BoardGrid>` ↔ `<KanbanBoard>` 동적 전환
- `computedBoard`: 백엔드 데이터가 있으면 사용, 없으면 Mock 데이터로 폴백

---

## 🚀 실행 방법

### 프론트엔드 개발 서버 실행
```bash
cd /home/knh11/spring/boot/frontend
npm run dev
# → http://localhost:5173 에서 확인
```

### 백엔드 서버 실행
```bash
cd /home/knh11/spring/boot/backend
./gradlew bootRun
# → http://localhost:9090 에서 실행
```

---

## 🔧 주요 백엔드 API 엔드포인트 (연동 기준)

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/boards/{id}` | 보드 단건 조회 |
| GET | `/board-columns/board/{boardId}` | 보드의 컬럼 목록 |
| GET | `/board-groups/board/{boardId}` | 보드의 그룹 목록 |
| GET | `/items/board/{boardId}` | 보드의 아이템 목록 |
| PUT | `/items/{id}` | 아이템 수정 (순서/그룹 변경 시 사용) |
| GET | `/projects/workspace/{workspaceId}` | 워크스페이스 내 프로젝트 목록 |
| GET | `/workspaces` | 워크스페이스 목록 |

---

## ✅ Phase 3 완료 내역 (고급 계층구조, 대시보드, 상세 패널, 로그인)

### 1. 워크스페이스-프로젝트-보드 지연 로딩
- `useWorkspaceStore.ts` 스토어 구축
- 사이드바(`Sidebar.vue`) 내 트리 지연 조회 비동기 호출 및 로딩 스피너 애니메이션 탑재

### 2. 고품격 대시보드 구현
- `DashboardView.vue` 컴포넌트 추가
- HSL 그라데이션 차트 바, SVG 기반의 번다운 차트(글로우 효과 포함), Monday.com 스타일 상태 배터리 위젯 구현

### 3. 아이템 상세 피드백 패널 구현
- `ItemDetailPanel.vue` 개발 (우측 슬라이드인 모션 + EAV 데이터 수정 폼 + 대화형 댓글 히스토리 피드)
- 테이블 및 칸반 클릭 이벤트와 바인딩 완료

### 4. 프리미엄 글래스모피즘 로그인 화면 및 보안 가드 구현
- `LoginView.vue` 신규 개발 (다크 슬레이트 무드 오로라 구체 비주얼 및 이메일/소셜 로그인 탑재)
- `router/index.ts` 전역 네비게이션 가드(`beforeEach`) 연동을 통한 비인증 우회 차트화 구축
- **백엔드 BCrypt 하이브리드 검증 연동**: `spring-boot-starter-security` 내 `BCryptPasswordEncoder` 기반의 하이브리드(평문/BCrypt 호환) 패스워드 매칭 로직을 `UserService.java`에 완성하고, 프론트엔드 연동 표준 규격을 완료했습니다.
- **백엔드 Context Path 규격 연동**: 백엔드의 `server.servlet.context-path=/api` 정책에 정확히 발맞추어, 프론트엔드 [api.ts](file:///Ubuntu/home/knh11/spring/boot/frontend/src/services/api.ts)의 `baseURL`을 `http://localhost:9090/api`로 정밀 정정 완료했습니다.

---

## 📌 다음 단계 (Phase 4 진행 중)
- [x] 자동화(Automation) 규칙 빌더 UI 구현 및 보드 연동
- [x] 알림(Notification) UI 드롭다운 연동 완료
- [x] **활동 로그 (Activity Logs) 상세 설계 및 구현**
  - **설계 내역**:
    - **Backend**: `ActivityLogDto`, `ActivityLogController`, `ActivityLogService`, `ActivityLogMapper.xml` 추가. Item 상태 및 담당자 변경 시 로직 트리거하여 로그 적재. `GET /activity-logs/item/{itemId}` 명세 지원.
    - **Frontend**: `ItemDetailPanel.vue` 내에 기존 댓글 피드와 함께 "업무 히스토리 & 피드백"이라는 단일 타임라인 피드로 렌더링되도록 통합.
- [x] **첨부파일 (Attachments) 상세 설계 및 구현**
  - **설계 내역**:
    - **Backend**: `AttachmentDto`, `AttachmentController`, `AttachmentService`, `AttachmentMapper.xml` 추가. 파일 메타데이터 관리 및 `POST /attachments/upload` 로 로컬 디렉토리 저장 후 URL 반환.
    - **Frontend**: `ItemDetailPanel.vue` 우측 상단/하단 영역에 첨부파일 목록 렌더링. 드래그 앤 드롭 또는 파일 탐색기로 단건 업로드 API 호출 연동.

---

*이 문서는 개발 진행 상황에 따라 지속적으로 업데이트됩니다.*
