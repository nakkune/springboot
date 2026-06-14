# ProjectOS 프론트엔드 2단계(Phase 2) 구현 계획

1단계 MVP 구현을 성공적으로 완료한 데 이어, 실제 데이터를 연동하고 고급 UI/UX 기능(칸반, 드래그앤드롭)을 추가하는 2단계 계획입니다.

## User Review Required

> [!IMPORTANT]
> 2단계 작업은 백엔드 서버(9090포트)가 켜져 있어야 완벽한 테스트가 가능합니다. 본 계획 승인 후 프론트엔드 코드가 백엔드 API를 호출하도록 전면 수정되므로, 서버 구동 준비가 되셨는지 확인을 부탁드립니다.

## Open Questions

> [!QUESTION]
> 1. 드래그 앤 드롭 구현을 위해 외부 라이브러리인 `vuedraggable` (Sortable.js 기반)을 추가로 설치하려고 하는데 승인하시나요?
> 2. 상태 관리(Pinia)에 백엔드에서 받아온 전체 `workspace`, `project` 계층 구조 데이터를 한 번에 로드할지, 아니면 사용자가 클릭할 때마다 Lazy Loading을 할지 선호하시는 방식이 있나요? (시니어 관점에서는 초기 로딩 최적화를 위해 Lazy Loading을 권장합니다.)

## Proposed Changes

### 1. 백엔드(9090포트) 실제 API 연동 (Mock 데이터 제거)
- **대상 파일**: `src/services/api.ts`, `src/stores/useBoardStore.ts`, `BoardView.vue`
- 백엔드 REST API(`@GetMapping("/boards/{id}")`, `/board-groups/board/{boardId}`, `/items/board/{boardId}`)를 연동하여 `BoardView.vue`의 Mock 데이터를 실제 데이터 흐름으로 교체.
- Axios 응답 데이터를 프론트엔드 상태(Pinia)로 정규화(Normalization).

### 2. 칸반(Kanban) 뷰 구현
- **대상 파일**: `src/views/BoardView.vue`, `src/features/board/KanbanBoard.vue` [NEW]
- `BoardView.vue`의 탭(Tab) 영역에서 "Kanban" 클릭 시 컴포넌트를 동적으로 스위칭(`<component :is="...">`).
- 상태(Status) 컬럼을 기준으로 그룹을 묶어서(Kanban Column) 렌더링.

### 3. 드래그 앤 드롭 (Drag & Drop) 적용
- **대상 파일**: `src/features/board/BoardGrid.vue`, `KanbanBoard.vue`
- `vuedraggable` 패키지를 설치하여 보드 뷰의 행(Row) 순서 변경 및 칸반 뷰의 카드 이동 구현.
- 이동 시 백엔드의 `/items/{id}`로 위치 및 상태 값 수정 API 비동기 호출.

### 4. 디자인 및 색상/레이아웃 미세 조정
- **대상 파일**: `Sidebar.vue`, `TopHeader.vue`
- TailwindCSS 커스텀 유틸리티를 활용하여 텍스트 및 경계선의 시각적 계층 구조 강화 (글래스모피즘 개선).

---

## Verification Plan

### Automated Tests
- 없음 (본 단계에서는 수동 검증 위주로 진행)

### Manual Verification
- [ ] 백엔드(Spring Boot, 9090) 실행 후 프론트엔드에서 데이터를 정상적으로 불러오는지 브라우저 네트워크 탭 확인.
- [ ] 메인 테이블에서 칸반 뷰로 탭 전환 시 UI가 깜빡임 없이 렌더링되는지 확인.
- [ ] 칸반 카드 드래그 앤 드롭 시 `item-values` API가 호출되는지 확인.
