import { defineStore } from 'pinia'
import api from '@/services/api'
import { ref } from 'vue'

export const useBoardStore = defineStore('board', () => {
  const currentBoard = ref<any>(null)
  const columns = ref<any[]>([])
  const groups = ref<any[]>([])
  const items = ref<any[]>([])
  const itemValues = ref<any[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Lazy Loading: 보드 ID로 보드 데이터 및 하위 엔티티들을 로드
  const fetchBoardData = async (boardId: string) => {
    isLoading.value = true
    error.value = null
    try {
      // 병렬로 API 호출 (각각의 컨트롤러 엔드포인트 참조)
      const [boardRes, colsRes, groupsRes, itemsRes, itemValuesRes] = await Promise.all([
        api.get(`/boards/${boardId}`).catch(() => null) as any,
        api.get(`/board-columns/board/${boardId}`).catch(() => []) as any,
        api.get(`/board-groups/board/${boardId}`).catch(() => []) as any,
        api.get(`/items/board/${boardId}`).catch(() => []) as any,
        api.get(`/item-values/board/${boardId}`).catch(() => []) as any
      ])

      currentBoard.value = boardRes
      columns.value = colsRes || []
      groups.value = groupsRes || []
      items.value = itemsRes || []
      itemValues.value = itemValuesRes || []
      
    } catch (e: any) {
      console.error(e)
      error.value = 'Failed to load board data'
    } finally {
      isLoading.value = false
    }
  }

  // 아이템 순서/그룹 변경 시 API 호출 액션
  const moveItem = async (itemId: string, newGroupId: string, newPosition: number) => {
    try {
      // 프론트엔드 상태 즉시 반영 (낙관적 업데이트)
      const itemToMove = items.value.find(i => i.id === itemId)
      if (itemToMove) {
        itemToMove.groupId = newGroupId
        itemToMove.position = newPosition
      }
      
      // 백엔드 API 호출 (실제 API 구현에 맞춰 수정 필요)
      await api.put(`/items/${itemId}`, {
        groupId: newGroupId,
        position: newPosition
      })
    } catch (e) {
      console.error('Failed to move item', e)
      // 롤백 로직 생략 (MVP)
    }
  }

  return {
    currentBoard,
    columns,
    groups,
    items,
    itemValues,
    isLoading,
    error,
    fetchBoardData,
    moveItem
  }
})
