<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useBoardStore } from '@/stores/useBoardStore'
import { X, Calendar, User, MessageSquare, CheckCircle2, Tag, Paperclip, UploadCloud, Download, Activity } from 'lucide-vue-next'
import api from '@/services/api' // 백엔드 실시간 동기화를 위한 api 임포트
import CommentInput from '@/features/item/CommentInput.vue'
import TagsColumn from '@/features/board/columns/TagsColumn.vue'
import DropdownColumn from '@/features/board/columns/DropdownColumn.vue'
import FileColumn from '@/features/board/columns/FileColumn.vue'
import RichTextColumn from '@/features/board/columns/RichTextColumn.vue'
import TextColumn from '@/features/board/columns/TextColumn.vue'
import TimelineColumn from '@/features/board/columns/TimelineColumn.vue' // [NEW] 타임라인 컬럼
import DateColumn from '@/features/board/columns/DateColumn.vue' // [NEW] 날짜 컬럼
import StatusColumn from '@/features/board/columns/StatusColumn.vue' // [NEW] 상태 컬럼
import PriorityColumn from '@/features/board/columns/PriorityColumn.vue' // [NEW] 우선순위 컬럼

const props = defineProps<{
  itemId: string | null
  isOpen: boolean
}>()

const emit = defineEmits<{
  'close': []
}>()

const boardStore = useBoardStore()
const commentText = ref('')
const commentListContainer = ref<HTMLElement | null>(null)

import { useWorkspaceStore } from '@/stores/useWorkspaceStore' // [NEW] 워크스페이스 멤버 연계 스토어 임포트

const workspaceStore = useWorkspaceStore()

// 10년 차 시니어 설계: 인라인 편집 컨트롤을 위한 반응형 토글 상태
const editingColId = ref<string | null>(null)

const startEditing = (colId: string) => {
  editingColId.value = colId
}
const stopEditing = () => {
  editingColId.value = null
}

// [NEW] 하드코딩된 목록 대신 DB 실제 가입된 유저 멤버 정보들을 동적 연동
const assigneeOptions = computed(() => {
  const memberNames = workspaceStore.currentWorkspaceMembers.map((m: any) => m.fullName)
  return [...memberNames, '미지정']
})

const statusColors: Record<string, string> = {
  '완료': 'text-emerald-600 dark:text-emerald-400 font-bold',
  '진행 중': 'text-blue-600 dark:text-blue-400 font-bold',
  '시작 전': 'text-slate-500 dark:text-slate-400 font-bold'
}

// [NEW] computed 재계산 시 껌벅임 및 폴백 덮어쓰기 초기화 방지를 위한 로컬 리액티브 버퍼 선언
const localValues = ref<Record<string, string>>({})

// EAV JSON 저장을 위한 댓글 전송 전용 가상 UUID 컬럼 정의
const COMMENTS_COL_ID = '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3d2e3c'

const localComments = ref<any[]>([])

// 10년 차 시니어 설계: 선택된 아이템의 데이터를 Store에서 찾아 EAV 보정을 거쳐 양방향 바인딩
const item = computed(() => {
  if (!props.itemId) return null
  const rawItem = boardStore.items.find((i: any) => i.id === props.itemId)
  if (!rawItem) {
    return {
      id: props.itemId,
      name: '새로운 업무',
      values: {
        status: '시작 전',
        assignee: '미지정',
        date: ''
      }
    }
  }

  // 백엔드 EAV의 column_id 데이터를 column_type(status, assignee 등)과 매핑 보정
  const resolvedValues: Record<string, any> = {}
  
  const rawCols = boardStore.columns.length > 0 ? boardStore.columns : [
    { id: 'status', name: '상태', type: 'status' },
    { id: 'assignee', name: '담당자', type: 'person' },
    { id: 'date', name: '마감일', type: 'date' }
  ]
  
  // [NEW] 10년 차 시니어 설계: 백엔드 컬럼 명세(columnType)와 프론트엔드 명세(type)를 완벽하게 정규화
  const activeCols = rawCols.map((col: any) => ({
    ...col,
    type: col.columnType || col.type || col.column_type
  }))
  
  // [FIX] 전역 EAV 스토어(boardStore.itemValues)에서 값 직접 조인
  const itemValueList = boardStore.itemValues.filter((v: any) => v.itemId === props.itemId)

  activeCols.forEach((col: any) => {
    // 1. 전역 EAV Store에서 값 추출 시도
    const foundValue = itemValueList.find((v: any) => v.columnId === col.id)
    let val = foundValue ? (foundValue.valueText || foundValue.valueJson || foundValue.valueDate || foundValue.valueNumber) : undefined
    
    // 2. DB에 값이 없다면 구형 캐시(rawValues)에 의존하지 않고 명확한 빈값 처리
    if (val === undefined || val === null) {
      val = ''
    }
    
    resolvedValues[col.id] = val
    resolvedValues[col.type] = val
  })
  
  // 댓글 가상 UUID 밸류도 함께 resolvedValues에 바인딩
  const commentsValue = itemValueList.find((v: any) => v.columnId === COMMENTS_COL_ID)?.valueJson
  if (commentsValue !== undefined && commentsValue !== null) {
    resolvedValues[COMMENTS_COL_ID] = commentsValue
  } else {
    resolvedValues[COMMENTS_COL_ID] = ''
  }
  
  return {
    ...rawItem,
    values: resolvedValues
  }
})

// [MODIFY] 아이템 ID 변경 시 로컬 리액티브 버퍼 캐시에 안전하게 복사 (초기화 및 덮어쓰기 버그 원천 소멸)
watch(
  () => [props.itemId, boardStore.columns, boardStore.itemValues],
  () => {
    if (props.itemId && item.value) {
      // 1. 로컬 렌더링 버퍼 맵에 고정 복사 (동적 컬럼 순회)
      const newLocalValues: Record<string, string> = {}
      boardStore.columns.forEach((col: any) => {
        const val = item.value?.values[col.id] || item.value?.values[col.columnType]
        newLocalValues[col.id] = val || ''
      })
      localValues.value = newLocalValues

      // 2. 댓글 복사
      const rawJson = item.value?.values[COMMENTS_COL_ID] || item.value?.values['comments']
      if (rawJson) {
        try {
          localComments.value = JSON.parse(rawJson)
        } catch (e) {
          console.error('Failed to parse comments JSON', e)
          localComments.value = []
        }
      } else {
        localComments.value = []
      }
    } else {
      localValues.value = {}
      localComments.value = []
    }
  },
  { immediate: true, deep: true }
)

const localActivities = ref<any[]>([])

// 활동 로그 가져오기
const fetchActivityLogs = async (itemId: string) => {
  try {
    const res = await api.get(`/activity-logs/item/${itemId}`) as any[]
    localActivities.value = res.map((log: any) => ({
      id: log.id,
      type: 'activity',
      author: log.actorName || '시스템',
      avatar: log.actorAvatar || '⚙️',
      body: `[${log.action}] ${log.meta}`,
      createdAt: log.createdAt?.replace('T', ' ').substring(0, 16) || ''
    }))
  } catch (err) {
    console.error('Failed to fetch activity logs', err)
    localActivities.value = []
  }
}

const localAttachments = ref<any[]>([])

// [NEW] 드롭다운 옵션 파서 (settings JSONB 동기화)
const getDropdownOptions = (col: any): string[] => {
  if (col.settings) {
    try {
      const parsed = JSON.parse(col.settings)
      if (parsed.options && Array.isArray(parsed.options) && parsed.options.length > 0) {
        return parsed.options
      }
    } catch {}
  }
  return ['옵션 A', '옵션 B', '옵션 C']
}

// [NEW] 상태 옵션 파서 (컬러 동기화 포함)
const getStatusOptions = (col: any): { id?: string; label: string; color: string }[] => {
  if (col.settings) {
    try {
      const parsed = JSON.parse(col.settings)
      if (parsed.options && Array.isArray(parsed.options) && parsed.options.length > 0) {
        if (typeof parsed.options[0] === 'object') {
          return parsed.options as { label: string; color: string }[]
        }
      }
    } catch {}
  }
  return [
    { id: '1', label: '완료', color: '#00C875' },
    { id: '2', label: '진행 중', color: '#FDAB3D' },
    { id: '3', label: '시작 전', color: '#C4C4C4' },
    { id: '4', label: '막힘', color: '#E2445C' }
  ]
}

// 첨부파일 가져오기
const fetchAttachments = async (itemId: string) => {
  try {
    const res = await api.get(`/attachments/item/${itemId}`) as any[]
    localAttachments.value = res
  } catch (err) {
    console.error('Failed to fetch attachments', err)
    localAttachments.value = []
  }
}

watch(
  () => props.itemId,
  (newId) => {
    if (newId) {
      fetchActivityLogs(newId)
      fetchAttachments(newId)
    } else {
      localActivities.value = []
      localAttachments.value = []
    }
  },
  { immediate: true }
)

// 탭 상태 (피드백 vs 업무 히스토리)
const activeTab = ref<'feedback' | 'history'>('feedback')

const fileInput = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)
const isUploading = ref(false)

const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileSelect = async (event: Event) => {
  const files = (event.target as HTMLInputElement).files
  if (files && files.length > 0) {
    await uploadFile(files[0])
  }
}

const handleDrop = async (event: DragEvent) => {
  isDragging.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    await uploadFile(files[0])
  }
}

// blob 다운로드 헬퍼 (브라우저에서 이미지가 열리지 않고 무조건 다운로드)
const downloadAttachment = async (storageUrl: string, fileName: string) => {
  try {
    const url = 'http://localhost:9090/api' + storageUrl
    const response = await fetch(url)
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('Download failed', e)
  }
}

const uploadFile = async (file: File) => {
  if (!props.itemId) return
  isUploading.value = true
  const formData = new FormData()
  formData.append('itemId', props.itemId)
  formData.append('file', file)

  try {
    const res = await api.post('/attachments/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }) as any
    localAttachments.value.unshift(res)
    fetchActivityLogs(props.itemId) // Refresh activities to show the log
  } catch (err) {
    console.error('Failed to upload file', err)
  } finally {
    isUploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

// 실시간 댓글 EAV JSON 업씽킹 핸들러
const handleCommentSubmit = async (data: { text: string, mentionedUserIds: string[] }) => {
  if (!data.text.trim() || !props.itemId) return

  const userName = localStorage.getItem('userName') || '익명 사용자'
  const avatarUrl = localStorage.getItem('avatarUrl') || userName.substring(0, 1)

  const newComment = {
    id: `c-${Date.now()}`,
    author: userName,
    avatar: avatarUrl,
    body: data.text.trim(),
    createdAt: new Date().toISOString().replace('T', ' ').substring(0, 16),
    mentionedUserIds: data.mentionedUserIds
  }

  localComments.value.push(newComment)
  const jsonStr = JSON.stringify(localComments.value)

  if (item.value) {
    if (!item.value.values) item.value.values = {}
    item.value.values[COMMENTS_COL_ID] = jsonStr
    
    const storeItem = boardStore.items.find((i: any) => i.id === props.itemId)
    if (storeItem) {
      if (!storeItem.values) storeItem.values = {}
      storeItem.values[COMMENTS_COL_ID] = jsonStr
    }
  }

  const existingValue = boardStore.itemValues.find((v: any) => v.itemId === props.itemId && v.columnId === COMMENTS_COL_ID)
  if (existingValue) {
    existingValue.valueJson = jsonStr
  } else {
    boardStore.itemValues.push({
      id: 'temp-' + Date.now(),
      itemId: props.itemId,
      columnId: COMMENTS_COL_ID,
      valueJson: jsonStr
    })
  }

  try {
    await api.post('/item-values', {
      itemId: props.itemId,
      columnId: COMMENTS_COL_ID,
      valueJson: jsonStr,
      valueText: 'Feedback Update'
    })
    fetchActivityLogs(props.itemId)
  } catch (err) {
    console.error('Failed to sync feedback comments to backend', err)
  }

  nextTick(() => {
    if (commentListContainer.value) {
      commentListContainer.value.scrollTo({
        top: commentListContainer.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}

const updateItemName = async (newName: string) => {
  if (!props.itemId) return
  
  const target = boardStore.items.find((i: any) => i.id === props.itemId)
  if (target) {
    target.name = newName
  }
  
  try {
    await api.put(`/items/${props.itemId}`, {
      name: newName
    })
  } catch (err) {
    console.error('Failed to sync updated item name to backend', err)
  }
}

const handleClose = () => {
  emit('close')
}

const updateItemValue = async (columnId: string, columnType: string, newValue: string) => {
  if (!props.itemId || !item.value) return
  
  localValues.value[columnId] = newValue
  
  const storeItem = boardStore.items.find((i: any) => i.id === props.itemId)
  if (storeItem) {
    if (!storeItem.values) storeItem.values = {}
    
    storeItem.values[columnId] = newValue
    storeItem.values[columnType] = newValue
  }
  
  const existingValue = boardStore.itemValues.find((v: any) => v.itemId === props.itemId && v.columnId === columnId)
  if (existingValue) {
    existingValue.valueText = newValue
    existingValue.valueDate = newValue
    existingValue.valueJson = newValue
    existingValue.valueNumber = newValue
  } else {
    boardStore.itemValues.push({
      id: 'temp-' + Date.now(),
      itemId: props.itemId,
      columnId: columnId,
      valueText: newValue,
      valueDate: newValue,
      valueJson: newValue,
      valueNumber: newValue
    })
  }
  
  try {
    await api.post('/item-values', {
      itemId: props.itemId,
      columnId: columnId,
      valueText: newValue
    })
  } catch (err) {
    console.error(`Failed to sync [${columnType}] EAV value to backend`, err)
  }
}
</script>

<template>
  <div>
    <!-- 배경 오버레이 (Dimmer) -->
    <div 
      v-if="isOpen" 
      @click="emit('close')"
      class="fixed inset-0 bg-slate-950/20 dark:bg-slate-950/50 backdrop-blur-xs z-40 transition-opacity duration-300"
    ></div>

    <!-- 우측 슬라이드 인 패널 (가로폭 w-[540px]로 쾌적하게 확장) -->
    <aside 
      class="fixed right-0 top-0 bottom-0 w-[540px] bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 shadow-2xl z-50 transform transition-transform duration-300 ease-out flex flex-col text-slate-800 dark:text-slate-200"
      :class="isOpen ? 'translate-x-0' : 'translate-x-full'"
    >
      <!-- 패널 헤더 -->
      <div class="h-14 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between px-6 shrink-0 bg-slate-50/50 dark:bg-slate-900">
        <div class="flex items-center gap-2 text-indigo-600 dark:text-indigo-400 font-semibold text-sm">
          <CheckCircle2 class="w-4 h-4" />
          <span>업무 상세 내역</span>
        </div>
        <button 
          @click.stop="handleClose"
          class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-800 dark:hover:text-white transition-colors"
        >
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- 패널 컨텐트 (상세 정보 폼) -->
      <div v-if="item" class="flex-1 overflow-y-auto px-5 py-4 flex flex-col gap-4 custom-scrollbar bg-white dark:bg-slate-900" ref="commentListContainer">
        
        <!-- 아이템 제목 (수정 가능하게 통합) -->
        <div class="border-b border-slate-100 dark:border-slate-700/50 pb-3">
          <input 
            type="text" 
            :value="item.name" 
            @change="updateItemName(($event.target as HTMLInputElement).value)"
            class="w-full bg-transparent text-lg font-bold text-slate-800 dark:text-white tracking-tight border border-transparent focus:border-indigo-500 rounded px-1 py-0.5 outline-none transition-colors"
            placeholder="업무 제목을 입력해 주세요."
          />
        </div>

        <!-- 메타 정보 그리드 (아담한 크기) -->
        <div class="grid grid-cols-3 gap-2">
          <div 
            v-for="col in boardStore.columns" 
            :key="col.id" 
            class="bg-slate-50 dark:bg-slate-800/20 border border-slate-200 dark:border-slate-800/60 rounded-lg p-2 relative flex flex-col justify-between"
          >
            <div class="flex items-center gap-1 text-slate-500 dark:text-slate-400 text-[10px] font-semibold mb-1">
              <CheckCircle2 v-if="col.columnType === 'status'" class="w-3 h-3 text-indigo-600 dark:text-indigo-400" />
              <User v-else-if="col.columnType === 'person'" class="w-3 h-3 text-indigo-600 dark:text-indigo-400" />
              <Calendar v-else-if="col.columnType === 'date'" class="w-3 h-3 text-amber-500" />
              <Calendar v-else-if="col.columnType === 'timeline'" class="w-3 h-3 text-indigo-600 dark:text-indigo-400" />
              <Tag v-else-if="col.columnType === 'tags'" class="w-3 h-3 text-pink-500 dark:text-pink-400" />
              <div v-else class="w-2 h-2 rounded-full bg-slate-400 dark:bg-slate-600"></div>
              <span class="truncate">{{ col.name }}</span>
            </div>
            
            <!-- STATUS (Table View 공통 컬럼 컴포넌트로 전격 교체) -->
            <template v-if="col.columnType === 'status'">
              <div class="w-full -mx-1">
                <StatusColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  :options="getStatusOptions(col)"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>
            
            <!-- PERSON -->
            <template v-else-if="col.columnType === 'person'">
              <div v-if="editingColId === col.id">
                <select 
                  :value="localValues[col.id] || '미지정'"
                  @change="async (e) => { await updateItemValue(col.id, col.columnType, (e.target as HTMLSelectElement).value); stopEditing() }"
                  @blur="stopEditing"
                  class="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md px-1 py-0.5 text-[10px] text-slate-800 dark:text-white outline-none focus:border-indigo-500 transition-colors cursor-pointer"
                >
                  <option v-for="opt in assigneeOptions" :key="opt" :value="opt" class="bg-white dark:bg-slate-800 text-slate-800 dark:text-white">{{ opt }}</option>
                </select>
              </div>
              <div 
                v-else 
                @click="startEditing(col.id)"
                class="flex items-center gap-1 cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800/50 rounded transition-colors py-0.5"
                title="클릭하여 담당자 지정"
              >
                <div class="w-4 h-4 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-[7px] font-bold text-white shrink-0">
                  {{ localValues[col.id] && localValues[col.id] !== '미지정' ? localValues[col.id][0] : 'N' }}
                </div>
                <span class="text-[10px] text-slate-700 dark:text-slate-200 truncate leading-tight">
                  {{ localValues[col.id] || '미지정' }}
                </span>
              </div>
            </template>
            
            <!-- PRIORITY (Table View 공통 우선순위 별점 컴포넌트 PriorityColumn.vue로 완벽 교체) -->
            <template v-else-if="col.columnType === 'priority'">
              <div class="w-full -mx-1">
                <PriorityColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>
            
            <!-- DATE (Table View의 커스텀 달력 컴포넌트 DateColumn.vue로 완벽 교체) -->
            <template v-else-if="col.columnType === 'date'">
              <div class="w-full -mx-1">
                <DateColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>
            
            <!-- TIMELINE (Monday.com 스타일 기간 막대) -->
            <template v-else-if="col.columnType === 'timeline'">
              <div class="w-full -mx-1">
                <TimelineColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>
            
            <!-- TAGS -->
            <template v-else-if="col.columnType === 'tags'">
              <div class="w-full h-auto -mx-1">
                <TagsColumn 
                  :value="localValues[col.id] || '[]'" 
                  :itemId="props.itemId || ''" 
                  :columnId="col.id" 
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>
            
            <!-- DROPDOWN -->
            <template v-else-if="col.columnType === 'dropdown'">
              <div class="w-full">
                <DropdownColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  :options="getDropdownOptions(col)"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>

            <!-- FILE -->
            <template v-else-if="col.columnType === 'file'">
              <div class="w-full">
                <FileColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>

            <!-- RICHTEXT -->
            <template v-else-if="col.columnType === 'richtext' || col.columnType === 'editor'">
              <div class="w-full">
                <RichTextColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>

            <!-- TEXT -->
            <template v-else-if="col.columnType === 'text'">
              <div class="w-full">
                <TextColumn 
                  :value="localValues[col.id] || ''"
                  :itemId="props.itemId || ''"
                  :columnId="col.id"
                  @update="(data: any) => updateItemValue(data.columnId, col.columnType, data.value)"
                />
              </div>
            </template>

            <!-- DEFAULT TEXT (기타 타입 폴백) -->
            <template v-else>
              <div v-if="editingColId === col.id">
                <input 
                  type="text"
                  :value="localValues[col.id]"
                  @change="async (e) => { await updateItemValue(col.id, col.columnType, (e.target as HTMLInputElement).value); stopEditing() }"
                  @blur="stopEditing"
                  @keyup.enter="(e) => (e.target as HTMLInputElement).blur()"
                  class="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md px-1.5 py-0.5 text-[10px] text-slate-800 dark:text-white outline-none focus:border-indigo-500 transition-colors"
                  placeholder="입력..."
                  autofocus
                />
              </div>
              <div 
                v-else 
                @click="startEditing(col.id)"
                class="text-[10px] text-slate-700 dark:text-slate-200 cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800/50 rounded transition-colors flex items-center justify-center py-0.5 leading-tight"
              >
                <span class="truncate">{{ localValues[col.id] || '-' }}</span>
              </div>
            </template>

          </div>
        </div>

        <!-- 구분선 -->
        <div class="h-px bg-slate-100 dark:bg-slate-800 my-1"></div>

        <!-- 첨부파일 (Attachments) 섹션 -->
        <div class="flex flex-col gap-2">
          <div class="flex items-center gap-1.5 text-slate-700 dark:text-slate-300 font-semibold text-xs">
            <Paperclip class="w-3.5 h-3.5 text-indigo-650 dark:text-indigo-400" />
            <span>첨부파일</span>
          </div>
          
          <!-- 드래그 앤 드롭 업로드 영역 -->
          <div 
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
            @click="triggerFileInput"
            :class="[
              'border-2 border-dashed rounded-lg p-3 flex flex-col items-center justify-center gap-1 cursor-pointer transition-colors',
              isDragging ? 'border-indigo-500 bg-indigo-500/10' : 'border-slate-200 dark:border-slate-700 hover:border-indigo-500/50 dark:hover:border-indigo-400/50 hover:bg-slate-50 dark:hover:bg-slate-800/30'
            ]"
          >
            <input type="file" ref="fileInput" class="hidden" @change="handleFileSelect" />
            <UploadCloud :class="['w-5 h-5', isDragging ? 'text-indigo-500 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500']" />
            <span v-if="isUploading" class="text-[10px] text-indigo-650 dark:text-indigo-300 font-medium animate-pulse">업로드 중...</span>
            <span v-else class="text-[10px] text-slate-500 dark:text-slate-400 text-center font-medium">클릭하거나 파일을 드래그하여 업로드하세요</span>
          </div>

          <!-- 첨부파일 목록 -->
          <div v-if="localAttachments.length > 0" class="grid grid-cols-2 gap-1.5">
            <div 
              v-for="att in localAttachments" 
              :key="att.id"
              class="bg-slate-50/50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-700/60 rounded-md p-1.5 flex items-center justify-between group hover:border-slate-300 dark:hover:border-slate-600 transition-colors"
            >
              <div class="flex items-center gap-1.5 min-w-0">
                <div class="w-6 h-6 rounded bg-slate-100 dark:bg-slate-700/50 flex items-center justify-center shrink-0">
                  <Paperclip class="w-3 h-3 text-slate-400 dark:text-slate-500" />
                </div>
                <div class="flex flex-col min-w-0">
                  <span class="text-[10px] font-medium text-slate-700 dark:text-slate-200 truncate">{{ att.fileName }}</span>
                  <span class="text-[9px] text-slate-400 dark:text-slate-500">{{ (att.fileSize / 1024).toFixed(1) }} KB</span>
                </div>
              </div>
              <button @click="downloadAttachment(att.storageUrl, att.fileName)" class="p-1 rounded-md hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-455 hover:text-slate-800 dark:hover:text-white transition-colors" title="다운로드">
                <Download class="w-3 h-3" />
              </button>
            </div>
          </div>
        </div>

        <!-- 구분선 -->
        <div class="h-px bg-slate-100 dark:bg-slate-800 my-1"></div>

        <!-- 탭 네비게이션 -->
        <div class="flex items-center gap-3 border-b border-slate-200 dark:border-slate-700/50 pb-1.5">
          <button 
            @click="activeTab = 'feedback'"
            :class="['flex items-center gap-1.5 text-xs font-semibold px-1.5 pb-1.5 -mb-1.5 border-b-2 transition-colors', activeTab === 'feedback' ? 'border-indigo-600 dark:border-indigo-400 text-indigo-600 dark:text-indigo-400' : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-300']"
          >
            <MessageSquare class="w-3.5 h-3.5" />
            <span>피드백 ({{ localComments.length }})</span>
          </button>
          <button 
            @click="activeTab = 'history'"
            :class="['flex items-center gap-1.5 text-xs font-semibold px-1.5 pb-1.5 -mb-1.5 border-b-2 transition-colors', activeTab === 'history' ? 'border-indigo-600 dark:border-indigo-400 text-indigo-600 dark:text-indigo-400' : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-300']"
          >
            <Activity class="w-3.5 h-3.5" />
            <span>업무 히스토리 ({{ localActivities.length }})</span>
          </button>
        </div>

        <div class="flex flex-col gap-2">
          <!-- 피드백(댓글) 목록 -->
          <div v-if="activeTab === 'feedback'" class="flex flex-col gap-2">
            <div 
              v-for="comment in localComments" 
              :key="comment.id"
              class="bg-slate-50/50 dark:bg-slate-800/20 border border-slate-200 dark:border-slate-800/60 rounded-lg p-2.5 flex gap-2 hover:border-slate-300 dark:hover:border-slate-700/40 transition-colors"
            >
              <div class="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200 flex items-center justify-center text-[9px] font-bold shrink-0">
                {{ comment.avatar }}
              </div>
              <div class="flex-1 flex flex-col gap-0.5 min-w-0">
                <div class="flex justify-between items-center">
                  <span class="text-[10px] font-bold text-slate-700 dark:text-slate-300">{{ comment.author }}</span>
                  <span class="text-[9px] text-slate-400 dark:text-slate-600 font-medium">{{ comment.createdAt }}</span>
                </div>
                <p class="text-[10px] leading-relaxed break-words text-slate-600 dark:text-slate-400">{{ comment.body }}</p>
              </div>
            </div>
            
            <div v-if="localComments.length === 0" class="text-center py-4 text-slate-500 dark:text-slate-400 text-[10px]">
              아직 등록된 피드백이 없습니다.
            </div>
          </div>

          <!-- 업무 히스토리 목록 -->
          <div v-if="activeTab === 'history'" class="flex flex-col gap-2">
            <div 
              v-for="activity in localActivities" 
              :key="activity.id"
              class="bg-slate-50/30 dark:bg-slate-900/40 border border-slate-150 dark:border-slate-800/40 rounded-lg p-2.5 flex gap-2 transition-colors"
            >
              <div class="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200 flex items-center justify-center text-[9px] font-bold shrink-0">
                {{ activity.avatar }}
              </div>
              <div class="flex-1 flex flex-col gap-0.5 min-w-0">
                <div class="flex justify-between items-center">
                  <span class="text-[10px] font-bold text-slate-700 dark:text-slate-300">{{ activity.author }}</span>
                  <span class="text-[9px] text-slate-400 dark:text-slate-600 font-medium">{{ activity.createdAt }}</span>
                </div>
                <p class="text-[10px] leading-relaxed break-words text-slate-500 italic">{{ activity.body }}</p>
              </div>
            </div>
            
            <div v-if="localActivities.length === 0" class="text-center py-4 text-slate-500 dark:text-slate-400 text-[10px]">
              활동 기록이 없습니다.
            </div>
          </div>
        </div>
      </div>

      <!-- 댓글 입력 폼 (하단 고정, 피드백 탭에서만 노출) -->
      <div v-show="activeTab === 'feedback'" class="p-4 border-t border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md">
        <CommentInput :itemId="itemId || ''" @submit="handleCommentSubmit" />
      </div>
    </aside>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #27272a;
  border-radius: 2px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #3f3f46;
}
</style>
