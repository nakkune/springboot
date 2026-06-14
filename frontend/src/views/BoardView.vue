<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useBoardStore } from '@/stores/useBoardStore'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore' // [NEW] 워크스페이스 멤버 동기화용 스토어 임포트
import BoardGrid from '@/features/board/BoardGrid.vue'
import KanbanBoard from '@/features/board/KanbanBoard.vue'
import TimelineView from '@/features/board/TimelineView.vue'
import CalendarView from '@/features/board/CalendarView.vue'
import DashboardView from '@/views/DashboardView.vue' // [NEW] 대시보드 뷰 동적 렌더링 지원
import ItemDetailPanel from '@/components/board/ItemDetailPanel.vue' 
import AutomationList from '@/features/automations/AutomationList.vue'
import ConfirmModal from '@/components/ui/ConfirmModal.vue' // [NEW] 커스텀 확인 모달 임포트
import { Plus, X, List, LayoutGrid, BarChart2, Sparkles, Calendar, Clock } from 'lucide-vue-next' // [NEW] 프리미엄 뷰 아이콘 및 닫기 임포트
import api from '@/services/api' // [NEW] 백엔드 실시간 동기화를 위한 api 서비스 임포트

const route = useRoute()
const boardStore = useBoardStore()
const workspaceStore = useWorkspaceStore() // 스토어 인스턴스화
// 10년 차 시니어 설계: 다중 뷰 지원을 위한 동적 뷰 탭 상태 및 인터페이스 정의
interface BoardViewTab {
  id: string
  name: string
  type: 'main' | 'kanban' | 'timeline' | 'calendar' | 'dashboard'
}

const views = ref<BoardViewTab[]>([
  { id: 'main', name: 'Main Table', type: 'main' },
  { id: 'kanban', name: 'Kanban', type: 'kanban' },
  { id: 'timeline', name: 'Timeline', type: 'timeline' },
  { id: 'calendar', name: 'Calendar', type: 'calendar' },
  { id: 'dashboard', name: 'Dashboard', type: 'dashboard' }
])

const activeTab = ref('main')

// 현재 활성화된 뷰 조회
const activeView = computed(() => {
  return views.value.find(v => v.id === activeTab.value) || views.value[0]
})

const isAddViewDropdownOpen = ref(false)
const toggleAddViewDropdown = () => {
  isAddViewDropdownOpen.value = !isAddViewDropdownOpen.value
}

const isAutomationListOpen = ref(false)

// 뷰 동적 추가 핸들러
const handleAddNewView = (type: 'main' | 'kanban' | 'timeline' | 'calendar' | 'dashboard') => {
  const typeNames = {
    main: 'Table View',
    kanban: 'Kanban View',
    timeline: 'Timeline View',
    calendar: 'Calendar View',
    dashboard: 'Dashboard View'
  }
  
  const newId = `view-${Date.now()}`
  const count = views.value.filter(v => v.type === type).length + 1
  const name = `${typeNames[type]} ${count}`
  
  views.value.push({
    id: newId,
    name: name,
    type: type
  })
  
  activeTab.value = newId
  isAddViewDropdownOpen.value = false
  triggerToast(`새로운 '${name}' 가 성공적으로 추가되었습니다! 📈`)
}

// 뷰 삭제 핸들러
const handleDeleteView = (viewId: string) => {
  const index = views.value.findIndex(v => v.id === viewId)
  if (index !== -1) {
    views.value.splice(index, 1)
    if (activeTab.value === viewId) {
      activeTab.value = views.value[index - 1]?.id || 'main'
    }
    triggerToast('선택하신 뷰가 삭제되었습니다. 🗑️')
  }
}

// [NEW] 선택된 아이템 관리를 위한 상태 정의
const selectedItemId = ref<string | null>(null)
const isDetailOpen = ref(false)

// [NEW] 세련된 토스트(Toast) 팝업 알림을 위한 상태 정의
const showToast = ref(false)
const toastMessage = ref('')

const openItemDetail = (itemId: string) => {
  selectedItemId.value = itemId
  isDetailOpen.value = true
}

// 샘플 데이터 생성 함수(getSampleValuesByName) 제거됨 (실제 DB 데이터만 활용)

// 스토어 데이터 결합 연동 및 로컬 상태 동기화 바인딩
const computedBoard = computed(() => {
  const defaultBoardId = route.params.id as string || ''
  
  if (boardStore.currentBoard) {
    const rawGroups = boardStore.groups
    
    // [NEW] 10년 차 시니어 설계: 백엔드 컬럼 명세(columnType)와 프론트엔드 명세(type)를 완벽하게 정규화
    const activeCols = boardStore.columns.map((col: any) => ({
      ...col,
      type: col.columnType || col.type || col.column_type
    }))

    // 1. 모든 아이템에 대해 EAV values 매핑 및 기본 구조 세팅
    const resolvedItems = boardStore.items.map((i: any) => {
      const resolvedValues: Record<string, any> = {}
      const itemValueList = boardStore.itemValues.filter((v: any) => v.itemId === i.id)

      activeCols.forEach((col: any) => {
        const foundValue = itemValueList.find((v: any) => v.columnId === col.id)
        if (foundValue) {
          const val = foundValue.valueText || foundValue.valueJson || foundValue.valueDate || foundValue.valueNumber || ''
          resolvedValues[col.id] = val
          resolvedValues[col.columnType] = val
        } else {
          resolvedValues[col.id] = ''
          resolvedValues[col.columnType] = ''
        }
      })
      
      return {
        ...i,
        values: resolvedValues,
        subItems: [] // [NEW] 하위 아이템 배열 초기화
      }
    })

    // 2. 부모-자식 트리 구성 (서브태스크 연결)
    const rootItems: any[] = []
    const itemMap = new Map()
    resolvedItems.forEach((i: any) => itemMap.set(i.id, i))
    
    resolvedItems.forEach((i: any) => {
      if (i.parentItemId && itemMap.has(i.parentItemId)) {
        itemMap.get(i.parentItemId).subItems.push(i)
      } else {
        rootItems.push(i)
      }
    })

    // 3. 백엔드 그룹 리스트를 순회하며 최상위(Root) 아이템만 그룹에 할당
    const boundGroups = rawGroups.map((g: any) => {
      const groupItems = rootItems.filter((i: any) => i.groupId === g.id)
      return {
        ...g,
        items: groupItems
      }
    })

    return {
      ...boardStore.currentBoard,
      groups: boundGroups,
      columns: activeCols
    }
  }
  
  // 백엔드가 비어있거나 아직 호출 전일 때의 정직한 엠프티 보드 스테이트
  return {
    id: defaultBoardId,
    name: boardStore.isLoading ? '보드 로딩 중...' : '보드를 찾을 수 없습니다.',
    description: '유효한 보드 ID로 접근해 주세요.',
    groups: [],
    columns: []
  }
})

// 보드 데이터를 가져오는 통합 함수
const loadBoard = async (boardId: string) => {
  await boardStore.fetchBoardData(boardId)
  
  // [NEW] 10년 차 시니어 설계: 현재 보드가 속한 워크스페이스를 추적하여 해당 워크스페이스의 멤버 목록을 동기화
  let foundWorkspaceId = null;
  // Pinia 스토어의 트리(Workspace -> Project -> Board)를 순회하며 소속 워크스페이스 발굴
  for (const ws of workspaceStore.workspaces) {
    for (const proj of ws.projects) {
      if (proj.boards.some(b => b.id === boardId)) {
        foundWorkspaceId = ws.id;
        break;
      }
    }
    if (foundWorkspaceId) break;
  }
  
  if (foundWorkspaceId) {
    await workspaceStore.fetchWorkspaceMembers(foundWorkspaceId)
  }
}

// 1. 라우트 파라미터 변경 감지 (보드 전환)
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      await loadBoard(newId as string)
    }
  }
)

import { useRouter } from 'vue-router'
const router = useRouter()

onMounted(async () => {
  let boardId = route.params.id as string
  
  // URL에 보드 ID가 없으면 첫 번째 워크스페이스의 첫 번째 보드로 자동 리다이렉트
  if (!boardId) {
    if (workspaceStore.workspaces.length === 0) {
      await workspaceStore.fetchWorkspaces()
    }
    const firstWs = workspaceStore.workspaces[0]
    if (firstWs && firstWs.projects && firstWs.projects.length > 0) {
      const firstProj = firstWs.projects[0]
      if (firstProj && firstProj.boards && firstProj.boards.length > 0) {
        boardId = firstProj.boards[0].id
        router.replace(`/board/${boardId}`)
        return // 워치(watch)가 동작하므로 여기서 종료
      }
    }
    // 그래도 보드가 없으면 대시보드로 튕김
    if (!boardId) {
      triggerToast('접근 가능한 보드가 없습니다. 대시보드로 이동합니다.')
      router.replace('/dashboard')
      return
    }
  }
  
  await loadBoard(boardId)
})

// [NEW] 실시간 신규 업무 동적 생성 핸들러 (10년 차 시니어 설계)
const handleAddItem = async (data: { name: string, groupId: string }) => {
  const defaultBoardId = route.params.id as string
  if (!defaultBoardId) return
  
  const tempId = 'item-' + Date.now()
  
  const newItem = {
    id: tempId,
    boardId: defaultBoardId,
    groupId: data.groupId,
    name: data.name,
    position: boardStore.items.length,
    values: {}
  }
  
  // Pinia 스토어 반응형 배열에 즉시 푸시 (Optimistic UI 즉각 반응형)
  boardStore.items.push(newItem)
  
  try {
    // [NEW] 백엔드 실시간 연동: 신규 아이템 생성 API 호출
    const createdItem = await api.post('/items', {
      boardId: defaultBoardId,
      groupId: data.groupId,
      name: data.name,
      position: newItem.position
    }) as any
    
    // 임시 ID를 백엔드에서 반환한 실제 UUID로 대체
    if (createdItem && createdItem.id) {
      const idx = boardStore.items.findIndex(i => i.id === tempId)
      if (idx !== -1) {
        boardStore.items[idx].id = createdItem.id
        
        // 기본값 자동 추가 로직 생략 (빈 상태로 DB에 아이템만 생성)
      }
    }
    
    triggerToast('신규 업무가 데이터베이스에 성공적으로 추가되었습니다! 🚀')
  } catch (err) {
    console.error('Failed to save item to database', err)
    triggerToast('업무를 추가했으나 서버 데이터베이스 동기화에 실패했습니다.')
  }
}

// [NEW] 하위 아이템(서브태스크) 동적 생성 핸들러
const handleAddSubItem = async (data: { name: string, groupId: string, parentItemId: string }) => {
  const defaultBoardId = route.params.id as string
  if (!defaultBoardId) return
  
  const tempId = 'item-' + Date.now()
  
  const newItem = {
    id: tempId,
    boardId: defaultBoardId,
    groupId: data.groupId,
    parentItemId: data.parentItemId,
    name: data.name,
    position: boardStore.items.length,
    values: {}
  }
  
  boardStore.items.push(newItem)
  
  try {
    const createdItem = await api.post('/items', {
      boardId: defaultBoardId,
      groupId: data.groupId,
      parentItemId: data.parentItemId,
      name: data.name,
      position: newItem.position
    }) as any
    
    if (createdItem && createdItem.id) {
      const idx = boardStore.items.findIndex(i => i.id === tempId)
      if (idx !== -1) {
        boardStore.items[idx].id = createdItem.id
      }
    }
    
    triggerToast('하위 아이템이 성공적으로 추가되었습니다! 🚀')
  } catch (err) {
    console.error('Failed to save sub-item', err)
    triggerToast('서버 동기화에 실패했습니다.')
  }
}

// [NEW] 실시간 신규 그룹 동적 생성 핸들러 (10년 차 시니어 설계)
const handleAddGroup = async (groupName: string) => {
  const defaultBoardId = route.params.id as string
  if (!defaultBoardId) return
  
  // 랜덤 프리미엄 컬러 매칭
  const colors = ['#579BFC', '#A25DDC', '#00C875', '#FDAB3D', '#E2445C', '#0086C0']
  const randomColor = colors[Math.floor(Math.random() * colors.length)]
  
  const tempId = 'group-' + Date.now()
  const newGroup = {
    id: tempId,
    boardId: defaultBoardId,
    title: groupName,
    color: randomColor,
    position: boardStore.groups.length,
    isCollapsed: false,
    items: []
  }
  
  // 낙관적 UI 업데이트
  boardStore.groups.push(newGroup)
  
  try {
    const createdGroup = await api.post('/board-groups', {
      boardId: defaultBoardId,
      title: groupName,
      color: randomColor,
      position: newGroup.position
    }) as any
    
    // 임시 ID 실제 ID로 치환
    if (createdGroup && createdGroup.id) {
      const idx = boardStore.groups.findIndex((g: any) => g.id === tempId)
      if (idx !== -1) {
        boardStore.groups[idx].id = createdGroup.id
      }
    }
    
    triggerToast(`'${groupName}' 그룹이 성공적으로 추가되었습니다! 🗂️`)
  } catch (err) {
    console.error('Failed to save group to database', err)
    triggerToast('그룹을 추가했으나 서버 저장에 실패했습니다.')
  }
}

// --- [NEW] 프리미엄 UI 삭제 모달 상태 관리 ---
const isConfirmModalOpen = ref(false)
const confirmModalTitle = ref('')
const confirmModalMessage = ref('')
const confirmTargetId = ref('')
const confirmType = ref<'group' | 'item'>('item')

// [NEW] 그룹 삭제 요청 (모달 띄우기)
const handleDeleteGroup = (groupId: string) => {
  const targetGroup = boardStore.groups.find((g: any) => g.id === groupId)
  confirmModalTitle.value = '그룹 삭제'
  confirmModalMessage.value = `'${targetGroup ? targetGroup.title : '그룹'}' 을(를) 정말 삭제하시겠습니까? 내부에 포함된 모든 업무도 함께 영구 삭제됩니다.`
  confirmTargetId.value = groupId
  confirmType.value = 'group'
  isConfirmModalOpen.value = true
}

// 실제 그룹 삭제 로직 실행
const executeDeleteGroup = async () => {
  isConfirmModalOpen.value = false
  const groupId = confirmTargetId.value
  
  // 1. 낙관적 UI 업데이트: 스토어에서 즉시 제거
  const targetGroup = boardStore.groups.find((g: any) => g.id === groupId);
  const groupTitle = targetGroup ? targetGroup.title : '그룹';
  boardStore.groups = boardStore.groups.filter((g: any) => g.id !== groupId);
  
  try {
    // 2. 백엔드 API 연동
    await api.delete(`/board-groups/${groupId}`);
    triggerToast(`'${groupTitle}' 그룹이 안전하게 삭제되었습니다. 🗑️`);
  } catch (err) {
    console.error('Failed to delete group from database', err);
    triggerToast('그룹 삭제에 실패했습니다. 서버 연동 상태를 확인해 주세요.');
    // 롤백은 복잡하므로 필요 시 다시 로드 처리 가능. (MVP 스킵)
  }
}

// [NEW] 아이템 삭제 요청 (모달 띄우기)
const handleDeleteItem = (itemId: string) => {
  confirmModalTitle.value = '아이템 삭제'
  confirmModalMessage.value = '이 아이템을 정말 삭제하시겠습니까? 내부에 있는 하위 항목들까지 모두 연쇄 삭제되며 복구할 수 없습니다.'
  confirmTargetId.value = itemId
  confirmType.value = 'item'
  isConfirmModalOpen.value = true
}

// 실제 아이템 연쇄 삭제 로직 실행
const executeDeleteItem = async () => {
  isConfirmModalOpen.value = false
  const itemId = confirmTargetId.value
  
  // 1. 낙관적 UI: 재귀 탐색을 통한 해당 아이템과 자손들 싹 제거
  const idsToDelete = new Set<string>([itemId])
  
  // 모든 자식들을 수집하는 헬퍼 함수
  const collectChildren = (parentId: string) => {
    const children = boardStore.items.filter(i => i.parentItemId === parentId)
    children.forEach(child => {
      if (!idsToDelete.has(child.id)) {
        idsToDelete.add(child.id)
        collectChildren(child.id)
      }
    })
  }
  
  collectChildren(itemId)
  
  // Store에서 필터링
  boardStore.items = boardStore.items.filter(i => !idsToDelete.has(i.id))
  
  try {
    await api.delete(`/items/${itemId}`)
    triggerToast('항목이 성공적으로 삭제되었습니다. 🗑️')
  } catch (err) {
    console.error('Failed to delete item', err)
    triggerToast('아이템 삭제 서버 연동 실패. 새로고침을 권장합니다.')
  }
}

const handleConfirmDelete = () => {
  if (confirmType.value === 'group') {
    executeDeleteGroup()
  } else {
    executeDeleteItem()
  }
}

// --- [NEW] 컬럼 관리 동적 업데이트 (낙관적 UI 및 API 연동) ---
const handleAddColumn = async (data: { name: string, type: string }) => {
  const newColumn = {
    boardId: boardStore.currentBoard.id,
    name: data.name,
    columnType: data.type,
    settings: "{}",
    position: boardStore.columns.length,
    isRequired: false,
    isHidden: false
  }
  
  // 임시 ID 발급 (실제는 DB UUID 사용)
  const tempId = 'temp-' + Date.now()
  boardStore.columns.push({ id: tempId, ...newColumn, type: data.type })
  
  try {
    const res: any = await api.post('/board-columns', newColumn)
    const responseData = res.data ? res.data : res
    
    // 성공 시 임시 객체를 실제 서버 반환 객체로 교체
    const idx = boardStore.columns.findIndex((c: any) => c.id === tempId)
    if (idx !== -1) {
      boardStore.columns[idx] = { ...responseData, type: responseData.columnType || responseData.type || responseData.column_type }
    }
    triggerToast(`'${data.name}' 컬럼이 추가되었습니다.`)
  } catch (err) {
    console.error('Failed to add column', err)
    boardStore.columns = boardStore.columns.filter((c: any) => c.id !== tempId)
    triggerToast('컬럼 추가에 실패했습니다. 다시 시도해주세요.')
  }
}

const handleUpdateColumn = async (data: { columnId: string, name: string }) => {
  const targetCol = boardStore.columns.find((c: any) => c.id === data.columnId)
  if (!targetCol) return
  
  const originalName = targetCol.name
  targetCol.name = data.name // 낙관적 렌더링
  
  try {
    const updateDto = {
      name: data.name,
      columnType: targetCol.columnType || targetCol.type,
      settings: typeof targetCol.settings === 'string' ? targetCol.settings : "{}",
      position: targetCol.position || 0,
      isRequired: targetCol.isRequired || false,
      isHidden: targetCol.isHidden || false
    }
    await api.put(`/board-columns/${data.columnId}`, updateDto)
    triggerToast(`컬럼 이름이 '${data.name}'(으)로 변경되었습니다.`)
  } catch (err) {
    console.error('Failed to update column', err)
    targetCol.name = originalName // 롤백
    triggerToast('컬럼 이름 변경에 실패했습니다.')
  }
}

const handleDeleteColumn = async (columnId: string) => {
  const colIndex = boardStore.columns.findIndex((c: any) => c.id === columnId)
  if (colIndex === -1) return
  
  const deletedCol = boardStore.columns[colIndex]
  boardStore.columns.splice(colIndex, 1) // 낙관적 제거
  
  try {
    await api.delete(`/board-columns/${columnId}`)
    triggerToast(`'${deletedCol.name}' 컬럼이 삭제되었습니다.`)
  } catch (err) {
    console.error('Failed to delete column', err)
    boardStore.columns.splice(colIndex, 0, deletedCol) // 롤백
    triggerToast('컬럼 삭제에 실패했습니다.')
  }
}

// [NEW] 드롭다운 옵션 설정 업데이트 핸들러 (10년 차 시니어 설계)
const handleUpdateColumnSettings = async (data: { columnId: string, settings: string }) => {
  const targetCol = boardStore.columns.find((c: any) => c.id === data.columnId)
  if (!targetCol) return

  const originalSettings = targetCol.settings
  targetCol.settings = data.settings // 낙관적 렌더링

  try {
    await api.put(`/board-columns/${data.columnId}`, {
      name: targetCol.name,
      columnType: targetCol.columnType || targetCol.type,
      settings: data.settings,
      position: targetCol.position || 0,
      isRequired: targetCol.isRequired || false,
      isHidden: targetCol.isHidden || false
    })
    triggerToast('드롭다운 옵션이 업데이트되었습니다.')
  } catch (err) {
    console.error('Failed to update column settings', err)
    targetCol.settings = originalSettings // 롤백
    triggerToast('옵션 저장에 실패했습니다.')
  }
}

// [NEW] 컬럼 가로 순서 재배열 및 DB 영속화 핸들러 (10년 차 시니어 프리미엄 아키텍처)
const handleReorderColumns = async (newColumns: any[]) => {
  // 1. 로컬 스토어 컬럼 순서 선제적 재배치
  boardStore.columns = [...newColumns]
  
  // 2. 인덱스 기반 position값 동적 재부여 및 병렬 API 일괄 통신
  try {
    const promises = boardStore.columns.map((col: any, index: number) => {
      col.position = index
      const updateDto = {
        name: col.name,
        columnType: col.columnType || col.type,
        settings: typeof col.settings === 'string' ? col.settings : "{}",
        position: index,
        isRequired: col.isRequired || false,
        isHidden: col.isHidden || false
      }
      return api.put(`/board-columns/${col.id}`, updateDto)
    })
    
    await Promise.all(promises)
    triggerToast('컬럼 순서가 정상 저장되었습니다. ↕️')
  } catch (err) {
    console.error('Failed to save columns order', err)
    triggerToast('컬럼 순서 저장 중 서버 통신 실패.')
  }
}

// [NEW] 실시간 특정 컬럼 속성(상태 등) 수정 핸들러
const handleUpdateItemValue = async (data: { itemId: string, columnId: string, value: string }) => {
  const targetItemIndex = boardStore.items.findIndex((i: any) => i.id === data.itemId)
  if (targetItemIndex !== -1) {
    const targetItem = boardStore.items[targetItemIndex]
    if (!targetItem.values) targetItem.values = {}
    
    // column_id 매핑 찾기
    const activeCols = boardStore.columns
    const column = activeCols.find((c: any) => c.type === data.columnId || c.id === data.columnId || c.columnType === data.columnId)
    const realColumnId = column ? column.id : data.columnId
    
    // [NEW] 10년 차 시니어 설계: EAV 스토어(itemValues) 직접 업데이트하여 computedBoard 즉각 재계산 트리거 (Optimistic UI)
    const existingValue = boardStore.itemValues.find((v: any) => v.itemId === data.itemId && v.columnId === realColumnId)
    if (existingValue) {
      existingValue.valueText = data.value
      existingValue.valueDate = data.value
      existingValue.valueJson = data.value
      existingValue.valueNumber = data.value
    } else {
      boardStore.itemValues.push({
        id: 'temp-' + Date.now(),
        itemId: data.itemId,
        columnId: realColumnId,
        valueText: data.value,
        valueDate: data.value,
        valueJson: data.value,
        valueNumber: data.value
      })
    }
    
    // [NEW] 백엔드 EAV 테이블(item_values) 실시간 자동 동기화
    try {
      
      await api.post('/item-values', {
        itemId: data.itemId,
        columnId: realColumnId,
        valueText: data.value
      })
      
      triggerToast(`업무의 상태가 '${data.value}'(으)로 갱신되었습니다. DB에 안전하게 보관됩니다. 💾`)
    } catch (e) {
      console.error('Failed to sync item value to backend', e)
      triggerToast('업무 상태를 변경했으나 서버 동기화에 실패했습니다.')
    }
  }
}

// [NEW] 프리미엄 플로팅 토스트 공통 제어 함수
const triggerToast = (msg: string) => {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 2500)
}

// [NEW] 그룹 제목 실시간 수정 (10년 차 시니어 설계)
const handleUpdateGroupTitle = async (data: { groupId: string, newTitle: string }) => {
  if (!data.newTitle || !data.newTitle.trim()) return
  
  // 1. 낙관적 업데이트
  const targetGroup = boardStore.groups.find((g: any) => g.id === data.groupId)
  if (targetGroup) {
    targetGroup.title = data.newTitle
  }
  
  // 2. 서버 동기화
  try {
    const existingDto = {
      id: data.groupId,
      title: data.newTitle
    }
    await api.put(`/board-groups/${data.groupId}`, existingDto)
  } catch (err) {
    console.error('Failed to update group title', err)
    triggerToast('그룹 이름 수정에 실패했습니다.')
  }
}

// [NEW] 아이템 제목 실시간 수정 (10년 차 시니어 설계)
const handleUpdateItemName = async (data: { itemId: string, newName: string }) => {
  if (!data.newName || !data.newName.trim()) return
  
  // 1. 낙관적 업데이트
  const targetItem = boardStore.items.find((i: any) => i.id === data.itemId)
  if (targetItem) {
    targetItem.name = data.newName
  }
  
  // 2. 서버 동기화
  try {
    const existingDto = {
      id: data.itemId,
      name: data.newName
    }
    await api.put(`/items/${data.itemId}`, existingDto)
  } catch (err) {
    console.error('Failed to update item name', err)
    triggerToast('업무 이름 수정에 실패했습니다.')
  }
}

// [NEW] 공유 버튼 활성화 (클립보드 주소 복사 및 토스트 인터랙션)
const handleShare = () => {
  const shareUrl = window.location.href
  navigator.clipboard.writeText(shareUrl).then(() => {
    triggerToast('보드 공유 링크가 클립보드에 성공적으로 복사되었습니다! 🔗')
  }).catch(() => {
    triggerToast('링크 복사에 실패했습니다. 브라우저 권한을 확인해 주세요.')
  })
}

// [NEW] 10년 차 시니어 설계: 드래그앤드롭(D&D) 실시간 백엔드 Reordering API 동기화 핸들러
const handleItemMove = async (data: { event: any, groupId: string }) => {
  const { added, moved } = data.event
  
  let targetItemId = ''
  let newPosition = 0
  let targetGroupId = data.groupId
  
  if (added) {
    targetItemId = added.element.id
    newPosition = added.newIndex
  } else if (moved) {
    targetItemId = moved.element.id
    newPosition = moved.newIndex
  } else {
    return
  }
  
  // 1. 로컬 캐시 즉시 업데이트 (Optimistic UI)
  const itemToMove = boardStore.items.find(i => i.id === targetItemId)
  if (itemToMove) {
    itemToMove.groupId = targetGroupId
    itemToMove.position = newPosition
  }
  
  // 2. 백엔드 D&D 정렬 알고리즘 API 호출
  try {
    await api.put(`/items/${targetItemId}`, {
      groupId: targetGroupId,
      position: newPosition
    })
    triggerToast('업무 순서 및 그룹 정렬이 실시간으로 안전하게 변경되었습니다! 🔁')
  } catch (err) {
    console.error('Failed to sync item move reordering to backend', err)
    triggerToast('순서 변경을 적용했으나 서버 저장에 실패했습니다.')
  }
}
</script>

<template>
  <div class="h-full flex flex-col select-none relative">
    <!-- Board Header -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold mb-1">{{ boardStore.isLoading ? 'Loading...' : computedBoard.name }}</h1>
        <p class="text-sm text-slate-500 dark:text-slate-400">{{ computedBoard.description }}</p>
      </div>
      <div class="flex gap-2">
        <button 
          @click="isAutomationListOpen = true"
          class="bg-slate-800 hover:bg-slate-700 text-white border border-slate-700 shadow-lg rounded-lg px-4 py-2 text-xs font-bold transition-all active:scale-95 cursor-pointer flex items-center gap-1.5"
        >
          <Sparkles class="w-3.5 h-3.5 text-indigo-400" />
          Automations
        </button>
        <button 
          @click="handleShare"
          class="bg-indigo-600 hover:bg-indigo-500 text-white border border-transparent shadow-lg shadow-indigo-600/15 rounded-lg px-4 py-2 text-xs font-bold transition-all active:scale-95 cursor-pointer"
        >
          Share Board
        </button>
      </div>
    </div>

    <!-- Board Tabs (Views) -->
    <div class="flex border-b border-slate-200 dark:border-slate-700 mb-6 gap-6 relative">
      <div 
        v-for="v in views"
        :key="v.id"
        class="pb-2 font-semibold text-xs cursor-pointer transition-colors flex items-center gap-1.5 group/tab relative"
        :class="activeTab === v.id ? 'border-b-2 border-indigo-500 text-indigo-400' : 'text-slate-500 hover:text-slate-300'"
        @click="activeTab = v.id"
      >
        <!-- 뷰 타입별 프리미엄 아이콘 탑재 -->
        <List v-if="v.type === 'main'" class="w-3.5 h-3.5" />
        <LayoutGrid v-else-if="v.type === 'kanban'" class="w-3.5 h-3.5" />
        <Clock v-else-if="v.type === 'timeline'" class="w-3.5 h-3.5" />
        <Calendar v-else-if="v.type === 'calendar'" class="w-3.5 h-3.5" />
        <BarChart2 v-else-if="v.type === 'dashboard'" class="w-3.5 h-3.5" />
        
        <span>{{ v.name }}</span>
        
        <!-- 커스텀 추가 뷰 삭제 버튼 제공 -->
        <button 
          v-if="v.id !== 'main' && v.id !== 'kanban'" 
          @click.stop="handleDeleteView(v.id)"
          class="p-0.5 rounded hover:bg-slate-800 text-slate-600 hover:text-slate-400 transition-colors"
          title="뷰 삭제"
        >
          <X class="w-2.5 h-2.5" />
        </button>
      </div>
      
      <!-- Add View 버튼 및 프리미엄 드롭다운 메뉴 -->
      <div class="relative pb-2">
        <div 
          @click="toggleAddViewDropdown"
          class="text-slate-500 hover:text-slate-300 text-xs cursor-pointer flex items-center gap-1 font-semibold transition-colors"
        >
          <Plus class="w-3.5 h-3.5" /> Add View
        </div>
        
        <!-- Add View Premium Dropdown Menu -->
        <div 
          v-if="isAddViewDropdownOpen" 
          class="absolute left-0 top-full mt-1 w-44 bg-slate-900 border border-slate-800 rounded-xl shadow-2xl z-30 p-1 flex flex-col gap-0.5"
        >
          <div 
            @click="handleAddNewView('main')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg text-2xs font-semibold text-slate-300 hover:text-white hover:bg-indigo-600 cursor-pointer transition-all"
          >
            <List class="w-3.5 h-3.5 text-indigo-400" />
            <span>Table View</span>
          </div>
          <div 
            @click="handleAddNewView('kanban')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg text-2xs font-semibold text-slate-300 hover:text-white hover:bg-indigo-600 cursor-pointer transition-all"
          >
            <LayoutGrid class="w-3.5 h-3.5 text-indigo-400" />
            <span>Kanban View</span>
          </div>
          <div 
            @click="handleAddNewView('timeline')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg text-2xs font-semibold text-slate-300 hover:text-white hover:bg-indigo-600 cursor-pointer transition-all"
          >
            <Clock class="w-3.5 h-3.5 text-indigo-400" />
            <span>Timeline View</span>
          </div>
          <div 
            @click="handleAddNewView('calendar')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg text-2xs font-semibold text-slate-300 hover:text-white hover:bg-indigo-600 cursor-pointer transition-all"
          >
            <Calendar class="w-3.5 h-3.5 text-indigo-400" />
            <span>Calendar View</span>
          </div>
          <div 
            @click="handleAddNewView('dashboard')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg text-2xs font-semibold text-slate-300 hover:text-white hover:bg-indigo-600 cursor-pointer transition-all"
          >
            <BarChart2 class="w-3.5 h-3.5 text-indigo-400" />
            <span>Dashboard View</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Board Content (Dynamic) -->
    <div class="flex-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg shadow-sm overflow-hidden flex flex-col glass">
      <div v-if="boardStore.isLoading" class="p-8 text-center text-slate-500">
        Loading board data...
      </div>
      <template v-else>
        <!-- 실시간 상호작용 이벤트 리스너 바인딩 -->
        <BoardGrid 
          v-if="activeView.type === 'main'" 
          :board="computedBoard" 
          @open-detail="openItemDetail" 
          @add-item="handleAddItem"
          @add-sub-item="handleAddSubItem"
          @add-group="handleAddGroup"
          @delete-group="handleDeleteGroup"
          @delete-item="handleDeleteItem"
          @add-column="handleAddColumn"
          @update-column="handleUpdateColumn"
          @update-column-settings="handleUpdateColumnSettings"
          @delete-column="handleDeleteColumn"
          @reorder-columns="handleReorderColumns"
          @update-item-value="handleUpdateItemValue"
          @update-item-name="handleUpdateItemName"
          @update-group-title="handleUpdateGroupTitle"
          @item-move="handleItemMove"
        />
        <KanbanBoard 
          v-else-if="activeView.type === 'kanban'" 
          :board="computedBoard" 
          @open-detail="openItemDetail" 
          @add-sub-item="handleAddSubItem"
          @delete-item="handleDeleteItem"
          @update-item-value="handleUpdateItemValue"
          @update-item-name="handleUpdateItemName"
        />
        <TimelineView
          v-else-if="activeView.type === 'timeline'"
          :board="computedBoard"
        />
        <CalendarView
          v-else-if="activeView.type === 'calendar'"
          :board="computedBoard"
        />
        <!-- Dashboard View 동적 탑재 (10년 차 시니어 프리미엄 위젯 연동) -->
        <DashboardView 
          v-else-if="activeView.type === 'dashboard'" 
        />
      </template>
    </div>

    <!-- 아이템 상세 정보 패널 -->
    <ItemDetailPanel 
      :itemId="selectedItemId" 
      :isOpen="isDetailOpen" 
      @close="() => { isDetailOpen = false; selectedItemId = null; }" 
    />

    <!-- 커스텀 뷰 삭제 등 기타 확인 모달 공용 렌더링 -->
    <ConfirmModal 
      :isOpen="isConfirmModalOpen"
      :title="confirmModalTitle"
      :message="confirmModalMessage"
      confirmText="영구 삭제"
      @confirm="handleConfirmDelete"
      @cancel="isConfirmModalOpen = false"
    />

    <!-- Automations List Modal -->
    <div v-if="isAutomationListOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-10">
      <div class="w-full max-w-5xl h-full max-h-[800px] bg-slate-900 border border-slate-700 rounded-3xl shadow-2xl relative overflow-hidden flex flex-col">
        <button @click="isAutomationListOpen = false" class="absolute top-5 right-5 p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors z-20">
          <X class="w-5 h-5" />
        </button>
        <div class="flex-1 overflow-hidden relative">
          <AutomationList :boardId="computedBoard.id" />
        </div>
      </div>
    </div>
    <!-- [NEW] 고품격 플로팅 토스트 팝업 (10년 차 시니어 디자인 요소를 가미한 그라데이션) -->
    <Transition name="toast-slide">
      <div 
        v-if="showToast" 
        class="fixed bottom-8 left-1/2 transform -translate-x-1/2 bg-slate-900/95 border border-indigo-500/30 text-indigo-200 backdrop-blur-md px-6 py-3.5 rounded-2xl shadow-2xl z-50 flex items-center gap-2.5 font-bold text-xs"
      >
        <div class="w-2 h-2 rounded-full bg-indigo-500 animate-ping"></div>
        <span>{{ toastMessage }}</span>
      </div>
    </Transition>
  </div>
</template>


<style scoped>
/* 토스트 트랜지션 애니메이션 */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.toast-slide-enter-from {
  opacity: 0;
  transform: translate(-50%, 20px) scale(0.95);
}
.toast-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, 15px) scale(0.95);
}
</style>
