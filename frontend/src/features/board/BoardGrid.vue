// src/features/board/BoardGrid.vue
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ChevronDown, ChevronRight, Plus, MoreHorizontal, Trash2, Maximize2, X, Settings } from 'lucide-vue-next'
import draggable from 'vuedraggable'
import DropdownColumn from '@/features/board/columns/DropdownColumn.vue'
import StatusColumn from '@/features/board/columns/StatusColumn.vue'
import TagsColumn from '@/features/board/columns/TagsColumn.vue'
import FileColumn from '@/features/board/columns/FileColumn.vue'
import RichTextColumn from '@/features/board/columns/RichTextColumn.vue'
import DateColumn from '@/features/board/columns/DateColumn.vue'
import TimelineColumn from '@/features/board/columns/TimelineColumn.vue'
import TextColumn from '@/features/board/columns/TextColumn.vue'
import PriorityColumn from '@/features/board/columns/PriorityColumn.vue'
import BoardGridSubItem from './BoardGridSubItem.vue'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'

const props = defineProps<{
  board: any
}>()

const workspaceStore = useWorkspaceStore()
const assigneeOptions = computed(() => {
  const memberNames = workspaceStore.currentWorkspaceMembers.map((m: any) => m.fullName)
  return [...memberNames, '미지정']
})

const emit = defineEmits<{
  'open-detail': [itemId: string]
  'item-move': [data: any]
  'add-item': [data: { name: string; groupId: string }]
  'add-sub-item': [data: { name: string; groupId: string; parentItemId: string }]
  'delete-item': [itemId: string]
  'add-group': [groupName: string]
  'delete-group': [groupId: string]
  'update-item-value': [data: { itemId: string; columnId: string; value: string }]
  'update-item-name': [data: { itemId: string; newName: string }]
  'update-group-title': [data: { groupId: string; newTitle: string }]
  'add-column': [data: { name: string; type: string }]
  'update-column': [data: { columnId: string; name: string }]
  'delete-column': [columnId: string]
  'update-column-settings': [data: { columnId: string; settings: string }]
  'reorder-columns': [newColumns: any[]]
}>()

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

const computedBoard = computed(() => props.board)

const localColumns = ref<any[]>([])
watch(
  () => computedBoard.value.columns,
  (newCols) => {
    if (newCols) {
      localColumns.value = [...newCols]
    }
  },
  { immediate: true, deep: true }
)

const startDateColId = computed(() => {
  const col = computedBoard.value.columns.find(
    (c: any) => c.name === '시작일' && (c.columnType === 'date' || c.type === 'date')
  )
  return col ? col.id : null
})

const endDateColId = computed(() => {
  const col = computedBoard.value.columns.find(
    (c: any) => c.name === '마감일' && (c.columnType === 'date' || c.type === 'date')
  )
  return col ? col.id : null
})

const timelineBounds = computed(() => {
  if (!computedBoard.value.groups) return { min: null, max: null }
  
  const allItems = computedBoard.value.groups.flatMap((g: any) => g.items || [])
  const dates = allItems.flatMap((item: any) => {
    const s = startDateColId.value ? item.values[startDateColId.value] : null
    const e = endDateColId.value ? item.values[endDateColId.value] : null
    return [s, e].filter((v: any) => v) as string[]
  })
  
  if (dates.length === 0) return { min: null, max: null }
  const sorted = dates.map((d: any) => new Date(d)).sort((a: any, b: any) => a.getTime() - b.getTime())
  return { min: sorted[0], max: sorted[sorted.length - 1] }
})

function getTimelineProgress(item: any): number {
  if (!startDateColId.value || !endDateColId.value) return 0
  const start = item.values[startDateColId.value]
  const end = item.values[endDateColId.value]
  if (!start || !end) return 0
  const startDate = new Date(start)
  const endDate = new Date(end)
  const bounds = timelineBounds.value || { min: null, max: null }
  const min = bounds.min
  const max = bounds.max
  if (!min || !max) return 0
  const total = max.getTime() - min.getTime()
  const elapsed = endDate.getTime() - min.getTime()
  return Math.round((elapsed / total) * 100)
}

const getDDayText = (item: any): string | null => {
  if (!endDateColId.value) return null
  const endVal = item.values[endDateColId.value]
  if (!endVal) return null
  
  const end = new Date(endVal)
  end.setHours(0, 0, 0, 0)
  
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  const diffTime = end.getTime() - today.getTime()
  const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays > 0) {
    return `D-${diffDays}`
  } else if (diffDays === 0) {
    return 'D-Day'
  } else {
    return `D+${Math.abs(diffDays)} (지연)`
  }
}

const getDotProgress = (item: any): string => {
  const progress = getTimelineProgress(item)
  const filledCount = Math.min(5, Math.max(0, Math.round(progress / 20)))
  const emptyCount = 5 - filledCount
  return '●'.repeat(filledCount) + '○'.repeat(emptyCount)
}

const getDDayBadgeStyle = (item: any) => {
  const progress = getTimelineProgress(item)
  const dday = getDDayText(item)
  
  const isDelayed = dday && dday.includes('지연')
  
  if (isDelayed || progress >= 80) {
    return {
      color: '#e11d48',
      backgroundColor: '#fff1f2',
      borderColor: '#fecdd3'
    }
  } else if (progress >= 50) {
    return {
      color: '#d97706',
      backgroundColor: '#fef3c7',
      borderColor: '#fde68a'
    }
  }
  return {
    color: '#059669',
    backgroundColor: '#ecfdf5',
    borderColor: '#a7f3d0'
  }
}

const activePersonDropdownItemId = ref<string | null>(null)

const togglePersonDropdown = (itemId: string) => {
  if (activePersonDropdownItemId.value === itemId) {
    activePersonDropdownItemId.value = null
  } else {
    activePersonDropdownItemId.value = itemId
  }
}

const selectPerson = (itemId: string, columnId: string, value: string) => {
  emit('update-item-value', { itemId, columnId, value })
  activePersonDropdownItemId.value = null
}

const handleUpdateItemValue = (data: { itemId: string, columnId: string, value: string }) => {
  emit('update-item-value', data)
}

const handleAddItem = (event: Event, groupId: string) => {
  const input = event.target as HTMLInputElement
  const name = input.value.trim()
  if (!name) return

  emit('add-item', { name, groupId })
  input.value = ''
}

const handleAddGroup = (event: Event) => {
  const input = event.target as HTMLInputElement
  const name = input.value.trim()
  if (!name) return

  emit('add-group', name)
  input.value = ''
}

const expandedItems = ref<Record<string, boolean>>({})

const toggleExpandItem = (itemId: string) => {
  expandedItems.value[itemId] = !expandedItems.value[itemId]
}

const handleSubItemSubmit = (event: Event, groupId: string, parentItemId: string) => {
  const target = event.target as HTMLInputElement
  const name = target.value.trim()
  if (!name) return
  emit('add-sub-item', { name, groupId, parentItemId })
  target.value = ''
}

const activeHeaderColId = ref<string | null>(null)
const isAddColumnMenuOpen = ref(false)
const editingColName = ref('')

const toggleHeaderMenu = (col: any) => {
  if (activeHeaderColId.value === col.id) {
    activeHeaderColId.value = null
  } else {
    activeHeaderColId.value = col.id
    editingColName.value = col.name
    isAddColumnMenuOpen.value = false
  }
}

const handleUpdateColumnName = (colId: string) => {
  if (!editingColName.value.trim()) return
  emit('update-column', { columnId: colId, name: editingColName.value.trim() })
  activeHeaderColId.value = null
}

const handleDeleteColumn = (colId: string) => {
  if(confirm('정말 이 컬럼을 삭제하시겠습니까? 데이터도 함께 삭제됩니다.')) {
    emit('delete-column', colId)
  }
  activeHeaderColId.value = null
}

const toggleAddColumnMenu = () => {
  isAddColumnMenuOpen.value = !isAddColumnMenuOpen.value
  activeHeaderColId.value = null
}

const handleAddColumn = (type: string, name: string) => {
  emit('add-column', { name, type })
  isAddColumnMenuOpen.value = false
}

const handleColumnChange = () => {
  emit('reorder-columns', localColumns.value)
}

const columnTypes = [
  { type: 'status', name: '상태', icon: '🚥', color: 'bg-emerald-500' },
  { type: 'text', name: '텍스트', icon: 'Aa', color: 'bg-blue-500' },
  { type: 'person', name: '담당자', icon: '👤', color: 'bg-indigo-500' },
  { type: 'date', name: '날짜', icon: '📅', color: 'bg-orange-500' },
  { type: 'timeline', name: '타임라인', icon: '⏳', color: 'bg-gradient-to-r from-indigo-500 to-purple-500' },
  { type: 'dropdown', name: '드롭다운', icon: '🔽', color: 'bg-purple-500' },
  { type: 'file', name: '파일', icon: '📎', color: 'bg-pink-500' },
  { type: 'richtext', name: '웹 에디터', icon: '📝', color: 'bg-rose-500' },
  { type: 'priority', name: '우선순위', icon: '⭐', color: 'bg-amber-400' }
]

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

const editingOptionsColId = ref<string | null>(null)
const editingOptions = ref<string[]>([])
const newOptionText = ref('')

const startEditOptions = (col: any) => {
  editingOptionsColId.value = col.id
  editingOptions.value = [...getDropdownOptions(col)]
  newOptionText.value = ''
}

const addOption = () => {
  const text = newOptionText.value.trim()
  if (text && !editingOptions.value.includes(text)) {
    editingOptions.value.push(text)
    newOptionText.value = ''
  }
}

const removeOption = (index: number) => {
  editingOptions.value.splice(index, 1)
}

const saveOptions = (colId: string) => {
  const settings = JSON.stringify({ options: editingOptions.value })
  emit('update-column-settings', { columnId: colId, settings })
  editingOptionsColId.value = null
  activeHeaderColId.value = null
}

const cancelEditOptions = () => {
  editingOptionsColId.value = null
}

const editingStatusColId = ref<string | null>(null)
const editingStatusOptions = ref<{ id?: string; label: string; color: string }[]>([])
const newStatusLabel = ref('')
const newStatusColor = ref('#64748b')

const startEditStatusOptions = (col: any) => {
  editingStatusColId.value = col.id
  editingStatusOptions.value = getStatusOptions(col).map(o => ({ ...o }))
  newStatusLabel.value = ''
  newStatusColor.value = '#64748b'
}

const addStatusOption = () => {
  const label = newStatusLabel.value.trim()
  if (label && !editingStatusOptions.value.some(o => o.label === label)) {
    const maxId = editingStatusOptions.value.reduce((max, o) => Math.max(max, parseInt(o.id || '0')), 0)
    editingStatusOptions.value.push({ id: String(maxId + 1), label, color: newStatusColor.value })
    newStatusLabel.value = ''
    newStatusColor.value = '#64748b'
  }
}

const removeStatusOption = (index: number) => {
  editingStatusOptions.value.splice(index, 1)
}

const saveStatusOptions = (colId: string) => {
  const settings = JSON.stringify({ options: editingStatusOptions.value })
  emit('update-column-settings', { columnId: colId, settings })
  editingStatusColId.value = null
  activeHeaderColId.value = null
}

const cancelEditStatusOptions = () => {
  editingStatusColId.value = null
}
</script>

<template>
  <div class="flex-1 overflow-auto w-full h-full text-sm select-none">
    <!-- 가로/세로 정렬 및 너비 불일치를 근본적으로 해결하기 위한 프리미엄 테이블 래퍼 장착 -->
    <div class="min-w-max flex flex-col min-h-full">
      <!-- Headers (Dynamic Columns) -->
      <div class="flex border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/50 sticky top-0 z-40 w-full">
      <div class="w-12 border-r border-slate-200 dark:border-slate-700 shrink-0"></div> <!-- Select box col -->
      <div class="w-[600px] shrink-0 border-r border-slate-200 dark:border-slate-700 px-4 py-2 font-medium text-slate-600 dark:text-slate-300 flex items-center">
        Item
      </div>
      <!-- 🌟 [10년차 시니어 UI/UX]: 컬럼 순서 드래그 앤 드롭 바인더 (Item 제외한 가로 드래그) 🌟 -->
      <draggable 
        v-model="localColumns" 
        item-key="id" 
        class="flex flex-1"
        tag="div"
        ghost-class="column-ghost"
        handle=".column-drag-handle"
        @change="handleColumnChange"
      >
        <template #item="{ element: col }">
          <div 
            class="shrink-0 border-r border-slate-200 dark:border-slate-700 px-2 sm:px-4 py-2 font-medium text-slate-600 dark:text-slate-300 text-center flex justify-center items-center relative group/colheader column-drag-handle cursor-grab active:cursor-grabbing hover:bg-slate-100/50 dark:hover:bg-slate-800/20 transition-all select-none"
            :class="col.columnType === 'timeline' ? 'w-[180px]' : 'w-[120px]'"
          >
            <div @click.stop="toggleHeaderMenu(col)" class="cursor-pointer hover:text-indigo-500 transition-colors flex items-center gap-1 w-full justify-center overflow-hidden">
              <span class="truncate">{{ col.name }}</span>
              <ChevronDown class="w-3 h-3 opacity-0 group-hover/colheader:opacity-100 transition-opacity shrink-0" />
            </div>
            
            <!-- 컬럼 헤더 편집/삭제/옵션 플로팅 드롭다운 (10년 차 시니어 프리미엄 UI) -->
            <div v-if="activeHeaderColId === col.id" class="absolute top-full left-1/2 -translate-x-1/2 mt-1 w-52 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl shadow-2xl z-50 p-2 flex flex-col gap-1" @click.stop>
              
              <!-- Option A: 드롭다운 옵션 편집 모드 -->
              <template v-if="editingOptionsColId === col.id">
                <div class="flex items-center justify-between px-1 py-0.5">
                  <span class="text-[11px] font-bold text-slate-400 tracking-wide">옵션 편집</span>
                  <button @click.stop="cancelEditOptions" class="text-[10px] text-slate-500 hover:text-white font-semibold transition-colors px-1.5 py-0.5 rounded hover:bg-slate-800">뒤로</button>
                </div>
                <div class="flex flex-col gap-0.5 max-h-[160px] overflow-y-auto px-0.5">
                  <div v-for="(opt, i) in editingOptions" :key="i" class="flex items-center gap-1 group/opt">
                    <input 
                      :value="opt"
                      @input="editingOptions[i] = ($event.target as HTMLInputElement).value"
                      class="flex-1 px-1.5 py-1 text-[11px] bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded outline-none focus:border-indigo-500 text-slate-800 dark:text-white transition-colors"
                    />
                    <button @click.stop="removeOption(i)" class="opacity-0 group-hover/opt:opacity-100 text-red-400 hover:text-red-300 transition-all p-0.5">
                      <X class="w-3 h-3" />
                    </button>
                  </div>
                </div>
                <div class="flex gap-1 items-center">
                  <input 
                    v-model="newOptionText" 
                    @keyup.enter="addOption"
                    class="flex-1 px-1.5 py-1 text-[11px] bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded outline-none focus:border-indigo-500 text-slate-800 dark:text-white placeholder-slate-400 transition-colors"
                    placeholder="새 옵션 입력 후 Enter"
                  />
                  <button @click.stop="addOption" class="px-2 py-1 text-[11px] bg-indigo-600 text-white rounded hover:bg-indigo-500 font-bold shrink-0 transition-colors">+</button>
                </div>
                <div class="h-px bg-slate-200 dark:bg-slate-800 my-0.5"></div>
                <button @click.stop="saveOptions(col.id)" class="w-full text-center px-2 py-1.5 text-[11px] rounded-lg bg-indigo-600 text-white font-bold hover:bg-indigo-500 transition-colors tracking-wide">옵션 저장</button>
              </template>

              <!-- Option C: 상태 컬럼 옵션 편집 모드 (color + label) (10년 차 시니어 설계) -->
              <template v-else-if="editingStatusColId === col.id">
                <div class="flex items-center justify-between px-1 py-0.5">
                  <span class="text-[11px] font-bold text-slate-400 tracking-wide">상태 옵션 편집</span>
                  <button @click.stop="cancelEditStatusOptions" class="text-[10px] text-slate-500 hover:text-white font-semibold transition-colors px-1.5 py-0.5 rounded hover:bg-slate-800">뒤로</button>
                </div>
                <div class="flex flex-col gap-1 max-h-[180px] overflow-y-auto px-0.5">
                  <div v-for="(opt, i) in editingStatusOptions" :key="i" class="flex items-center gap-1 group/opt">
                    <!-- 컬러 미리보기 + 피커 -->
                    <div class="relative">
                      <div 
                        class="w-5 h-5 rounded border border-slate-600 shrink-0 cursor-pointer"
                        :style="{ backgroundColor: opt.color }"
                        @click.stop="() => {}"
                      >
                        <input 
                          type="color"
                          :value="opt.color"
                          @input="editingStatusOptions[i].color = ($event.target as HTMLInputElement).value"
                          class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                        />
                      </div>
                    </div>
                    <input 
                      :value="opt.label"
                      @input="editingStatusOptions[i].label = ($event.target as HTMLInputElement).value"
                      class="flex-1 px-1.5 py-1 text-[11px] bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded outline-none focus:border-indigo-500 text-slate-800 dark:text-white transition-colors"
                    />
                    <button @click.stop="removeStatusOption(i)" class="opacity-0 group-hover/opt:opacity-100 text-red-400 hover:text-red-300 transition-all p-0.5">
                      <X class="w-3 h-3" />
                    </button>
                  </div>
                </div>
                <div class="flex flex-col gap-1">
                  <div class="flex gap-1 items-center">
                    <input 
                      v-model="newStatusLabel" 
                      @keyup.enter="addStatusOption"
                      class="flex-1 px-1.5 py-1 text-[11px] bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded outline-none focus:border-indigo-500 text-slate-800 dark:text-white placeholder-slate-400 transition-colors"
                      placeholder="새 상태명 입력 후 Enter"
                    />
                    <button @click.stop="addStatusOption" class="px-2 py-1 text-[11px] bg-indigo-600 text-white rounded hover:bg-indigo-500 font-bold shrink-0 transition-colors">+</button>
                  </div>
                  <div class="flex items-center gap-2">
                    <label class="text-[10px] text-slate-400 font-medium">컬러:</label>
                    <div class="relative">
                      <div class="w-6 h-6 rounded border border-slate-600" :style="{ backgroundColor: newStatusColor }"></div>
                      <input 
                        type="color"
                        v-model="newStatusColor"
                        class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                      />
                    </div>
                  </div>
                </div>
                <div class="h-px bg-slate-200 dark:bg-slate-800 my-0.5"></div>
                <button @click.stop="saveStatusOptions(col.id)" class="w-full text-center px-2 py-1.5 text-[11px] rounded-lg bg-indigo-600 text-white font-bold hover:bg-indigo-500 transition-colors tracking-wide">상태 옵션 저장</button>
              </template>
              
              <!-- Option B: 일반 설정 모드 -->
              <template v-else>
                <div class="px-1 py-0.5 text-[11px] font-bold text-slate-400 tracking-wide">컬럼 설정</div>
                <input 
                  v-model="editingColName" 
                  @keyup.enter="handleUpdateColumnName(col.id)"
                  class="w-full px-2 py-1.5 text-sm bg-slate-100 dark:bg-slate-800 border-none rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none transition-all"
                  placeholder="컬럼 이름"
                  autofocus
                />
                <button @click="handleUpdateColumnName(col.id)" class="w-full text-left px-3 py-1.5 text-xs rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-indigo-600 dark:text-indigo-400 font-medium mt-0.5 transition-colors">이름 저장</button>
                <div v-if="col.columnType === 'dropdown' || col.columnType === 'status'" class="h-px bg-slate-200 dark:bg-slate-800 my-1"></div>
                <button 
                  v-if="col.columnType === 'dropdown'" 
                  @click.stop="startEditOptions(col)" 
                  class="w-full text-left px-3 py-1.5 text-xs rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-indigo-600 dark:text-indigo-400 font-medium flex items-center gap-2 transition-colors"
                >
                  <Settings class="w-3 h-3" /> 옵션 편집
                </button>
                <button 
                  v-if="col.columnType === 'status'" 
                  @click.stop="startEditStatusOptions(col)" 
                  class="w-full text-left px-3 py-1.5 text-xs rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-indigo-600 dark:text-indigo-400 font-medium flex items-center gap-2 transition-colors"
                >
                  <Settings class="w-3 h-3" /> 상태 옵션 편집
                </button>
                <div class="h-px bg-slate-200 dark:bg-slate-800 my-1"></div>
                <button @click="handleDeleteColumn(col.id)" class="w-full text-left px-3 py-1.5 text-xs rounded-lg hover:bg-red-50 dark:hover:bg-red-500/10 text-red-600 font-medium flex items-center gap-2 transition-colors">
                  <Trash2 class="w-3.5 h-3.5" /> 컬럼 삭제
                </button>
              </template>
            </div>
          </div>
        </template>
      </draggable>
      
      <!-- [NEW] 컬럼 추가 버튼 및 드롭다운 -->
      <div class="w-12 flex items-center justify-center shrink-0 relative">
        <div @click="toggleAddColumnMenu" class="w-full h-full flex items-center justify-center cursor-pointer hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors">
          <Plus class="w-4 h-4 text-slate-500" />
        </div>
        
        <div v-if="isAddColumnMenuOpen" class="absolute top-full right-0 mt-1 w-56 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl shadow-2xl z-50 p-2 flex flex-col gap-0.5">
          <div class="px-2 py-1.5 text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">새 컬럼 추가</div>
          <div 
            v-for="ctype in columnTypes" 
            :key="ctype.type"
            @click="handleAddColumn(ctype.type, ctype.name)"
            class="w-full flex items-center gap-3 px-3 py-2 text-sm rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors group/ctype"
          >
            <div class="w-6 h-6 rounded flex items-center justify-center text-white text-xs shadow-sm" :class="ctype.color">{{ ctype.icon }}</div>
            <span class="font-medium text-slate-700 dark:text-slate-200">{{ ctype.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Groups & Items -->
    <div v-for="group in board.groups" :key="group.id" class="flex-1 flex flex-col mb-4">
      
      <!-- Group Header -->
      <div class="flex items-center px-4 py-3 sticky top-[41px] bg-white dark:bg-slate-900 z-10 group/header">
        <ChevronDown class="w-4 h-4 mr-2" :style="{ color: group.color }" />
        <input 
          type="text"
          :value="group.title"
          @change="(e) => $emit('update-group-title', { groupId: group.id, newTitle: (e.target as HTMLInputElement).value })"
          class="font-semibold text-base bg-transparent outline-none hover:bg-slate-100 dark:hover:bg-slate-800 focus:bg-slate-100 dark:focus:bg-slate-800 rounded px-1 -ml-1 transition-colors cursor-text"
          :style="{ color: group.color }"
        />
        <span class="ml-2 text-slate-400 text-xs">{{ group.items?.length || 0 }} Items</span>
        <div class="ml-3 opacity-0 group-hover/header:opacity-100 transition-opacity flex items-center">
          <button 
            @click="emit('delete-group', group.id)"
            class="p-1.5 rounded-lg hover:bg-red-500/10 text-slate-400 hover:text-red-500 transition-colors"
            title="그룹 삭제"
          >
            <Trash2 class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- Group Items (Draggable) -->
      <draggable 
        :list="group.items" 
        group="items" 
        item-key="id" 
        handle=".drag-handle"
        ghost-class="bg-slate-100"
        @change="(e: any) => $emit('item-move', { event: e, groupId: group.id })"
      >
        <template #item="{ element: item }">
          <div class="flex flex-col border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 group/row transition-colors">
            
            <!-- Main Row Content -->
            <div class="flex w-full items-stretch">
              <!-- Selection & Drag Handle -->
              <div class="w-12 border-r border-slate-200 dark:border-slate-700 flex items-center justify-center shrink-0">
                <div class="w-4 h-4 rounded border border-slate-300 dark:border-slate-600 opacity-0 group-hover/row:opacity-100 cursor-pointer drag-handle hover:bg-slate-200"></div>
              </div>
            
              <!-- Item Name (Editable) -->
              <div 
                class="w-[600px] shrink-0 border-r border-slate-200 dark:border-slate-700 px-4 py-2 flex items-center justify-between group/name" 
              >
                <div class="flex items-center w-full min-w-0 pr-2">
                  <!-- 하위 아이템 토글 버튼 -->
                  <button 
                    @click.stop="toggleExpandItem(item.id)"
                    class="mr-2 p-0.5 rounded-md hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-400 transition-colors shrink-0 flex items-center justify-center bg-slate-100 dark:bg-slate-800"
                  >
                    <ChevronDown v-if="expandedItems[item.id]" class="w-3.5 h-3.5 text-indigo-500" />
                    <ChevronRight v-else class="w-3.5 h-3.5" />
                  </button>
                  <input 
                    type="text" 
                    :value="item.name"
                    :title="item.name"
                    @change="(e) => $emit('update-item-name', { itemId: item.id, newName: (e.target as HTMLInputElement).value })"
                    class="flex-1 min-w-0 bg-transparent outline-none group-hover/name:text-indigo-400 transition-colors font-medium cursor-text truncate"
                  />
                  
                  <!-- 10년 차 시니어 설계: D-Day 및 5단계 마이크로 도트 진행률 배지 탑재 -->
                  <span 
                    v-if="getDDayText(item)"
                    class="ml-2 shrink-0 px-2 py-0.5 rounded-full border text-[10px] font-black font-mono shadow-sm flex items-center gap-1.5 transition-all duration-300"
                    :style="getDDayBadgeStyle(item)"
                  >
                    <span>{{ getDDayText(item) }}</span>
                    <span class="tracking-widest opacity-80">{{ getDotProgress(item) }}</span>
                  </span>
                </div>
                <div class="flex items-center gap-1 opacity-0 group-hover/row:opacity-100 shrink-0">
                  <button @click.stop="$emit('open-detail', item.id)" class="p-1 hover:bg-slate-200 dark:hover:bg-slate-700 rounded text-slate-400" title="상세 보기">
                    <Maximize2 class="w-3.5 h-3.5" />
                  </button>
                  <button @click.stop="$emit('delete-item', item.id)" class="p-1 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-slate-400 hover:text-red-500 transition-colors" title="삭제">
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            
              <!-- Dynamic Values -->
              <!-- 🌟 [10년차 시니어 UI/UX]: 헤더의 draggable flex 컨테이너와 너비 정렬을 완벽하게 맞추기 위해 flex-1 래퍼 장착 🌟 -->
              <div class="flex flex-1">
                <div 
                  v-for="col in localColumns" 
                  :key="col.id + '-' + (item.values[col.id] || item.values[col.columnType] || 'empty')" 
                  class="shrink-0 border-r border-slate-200 dark:border-slate-700 flex items-center justify-center relative p-1"
                  :class="col.columnType === 'timeline' ? 'w-[180px]' : 'w-[120px]'"
                >
                  <!-- Status Cell (드롭다운컬럼과 동일한 settings 기반 동적 옵션 + 컬러 지원) -->
                  <StatusColumn 
                    v-if="col.columnType === 'status'"
                    :value="item.values[col.id] || item.values[col.columnType]" 
                    :itemId="item.id" 
                    :columnId="col.id" 
                    :options="getStatusOptions(col)" 
                    @update="handleUpdateItemValue"
                  />
                  
                  <!-- 10년 차 시니어 설계: 담당자 지정 및 + 아이콘 나란히 노출 및 컬러링 개선 (이미지 동기화 완료) -->
                  <div v-else-if="col.columnType === 'person'" class="w-full h-full flex items-center justify-center cursor-pointer relative hover:z-[100]"
                    @click.stop="togglePersonDropdown(item.id)"
                  >
                    <div v-if="item.values[col.id] || item.values[col.columnType]" class="w-7 h-7 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px] font-black border border-slate-200 dark:border-slate-700 relative group/person hover:scale-105 hover:z-[100] transition-all duration-150 shadow-sm shrink-0">
                      {{ (item.values[col.id] || item.values[col.columnType]).substring(0, 1) }}
                      
                      <!-- 🌟 [10년차 시니어 UI/UX 디자인]: 프리미엄 툴팁 프로필 카드 (왼쪽 팝업 - 라이트/다크 동적 대응) 🌟 -->
                      <div 
                        class="absolute right-full top-1/2 -translate-x-1/2 mr-2 w-max min-w-[120px] bg-white/95 dark:bg-slate-900/95 text-slate-800 dark:text-slate-100 rounded-xl shadow-2xl border border-slate-200/80 dark:border-slate-800/80 p-2.5 flex flex-col items-center gap-1.5 opacity-0 pointer-events-none group-hover/person:opacity-100 group-hover/person:pointer-events-auto transition-all duration-200 transform translate-x-1 group-hover/person:translate-x-0 z-[99999] backdrop-blur-md"
                        @click.stop
                      >
                        <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center text-xs font-black text-white border border-white/20 shadow-md">
                          {{ (item.values[col.id] || item.values[col.columnType]).substring(0, 1) }}
                        </div>
                        <div class="flex flex-col items-center">
                          <span class="text-xs font-black tracking-tight whitespace-nowrap">{{ item.values[col.id] || item.values[col.columnType] }}</span>
                          <span class="text-[9px] text-slate-400 font-medium tracking-wide">담당자</span>
                        </div>
                        <div class="absolute left-full top-1/2 -translate-x-1/2 -mr-0.5 border-4 border-transparent border-l-slate-900/95 dark:border-l-slate-950/95"></div>
                      </div>
                    </div>
                    <!-- 10년 차 시니어 프리미엄 UI: 담당자가 미지정일 때만 아담한 둥근 점선 +를 1개 노출하여 지정을 유도 (중복 원 노출 방지) -->
                    <div v-else class="w-7 h-7 rounded-full bg-transparent flex items-center justify-center border border-dashed border-slate-300 dark:border-slate-650 hover:bg-slate-100 dark:hover:bg-slate-850 hover:border-indigo-400 transition-colors shrink-0">
                      <Plus class="w-3.5 h-3.5 text-indigo-500 dark:text-indigo-400" />
                    </div>
                    
                    <!-- Person Dropdown Popover -->
                    <div 
                      v-if="activePersonDropdownItemId === item.id" 
                      class="absolute top-full left-0 mt-1 w-[150%] -ml-[25%] bg-slate-800 border border-slate-700 rounded-xl shadow-2xl z-[999] p-1.5 flex flex-col gap-1 overflow-hidden"
                    >
                      <div 
                        v-for="opt in assigneeOptions" 
                        :key="opt"
                        @click.stop="selectPerson(item.id, col.id, opt)"
                        class="px-2 py-1.5 rounded-lg text-xs text-white hover:bg-slate-700 transition-colors cursor-pointer flex items-center gap-2"
                      >
                        <div class="w-5 h-5 rounded-full bg-indigo-500 flex items-center justify-center text-[8px] font-bold shrink-0">
                          {{ opt !== '미지정' ? opt.substring(0, 1) : 'N' }}
                        </div>
                        <span class="truncate">{{ opt }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <!-- Date Cell -->
                  <DateColumn 
                    v-else-if="col.columnType === 'date'"
                    :value="item.values[col.id] || item.values[col.columnType]"
                    :itemId="item.id"
                    :columnId="col.id"
                    @update="handleUpdateItemValue"
                  />
                  
                  <!-- Timeline Cell -->
                  <TimelineColumn 
                    v-else-if="col.columnType === 'timeline'"
                    :value="item.values[col.id] || item.values[col.columnType]"
                    :itemId="item.id"
                    :columnId="col.id"
                    @update="handleUpdateItemValue"
                  />
                  
                  <!-- Advanced Columns -->
                  <DropdownColumn 
                    v-else-if="col.columnType === 'dropdown'" 
                    :value="item.values[col.id] || item.values[col.columnType]" 
                    :itemId="item.id" 
                    :columnId="col.id" 
                    :options="getDropdownOptions(col)" 
                    @update="handleUpdateItemValue" 
                  />
                  <TagsColumn 
                    v-else-if="col.columnType === 'tags'" 
                    :value="item.values[col.id] || item.values[col.columnType]" 
                    :itemId="item.id" 
                    :columnId="col.id" 
                    @update="handleUpdateItemValue" 
                  />
                  <FileColumn 
                    v-else-if="col.columnType === 'file'" 
                    :value="item.values[col.id] || item.values[col.columnType]" 
                    :itemId="item.id" 
                    :columnId="col.id" 
                    @update="handleUpdateItemValue" 
                  />
                  <RichTextColumn 
                    v-else-if="col.columnType === 'richtext' || col.columnType === 'editor'" 
                    :value="item.values[col.id] || item.values[col.columnType]" 
                    :itemId="item.id" 
                    :columnId="col.id" 
                    @update="handleUpdateItemValue" 
                  />
                  
                  <!-- Text Cell -->
                  <TextColumn 
                    v-else-if="col.columnType === 'text'"
                    :value="item.values[col.id] || item.values[col.columnType]"
                    :itemId="item.id"
                    :columnId="col.id"
                    @update="handleUpdateItemValue"
                  />
                  
                  <!-- Priority Cell -->
                  <PriorityColumn
                    v-else-if="col.columnType === 'priority'"
                    :value="item.values[col.id] || item.values[col.columnType]"
                    :itemId="item.id"
                    :columnId="col.id"
                    @update="handleUpdateItemValue"
                  />
                  
                  <!-- Default Cell -->
                  <div v-else class="w-full h-full flex items-center justify-center px-1">
                    <div class="truncate text-center w-full">
                      {{ item.values[col.id] || item.values[col.columnType] || '-' }}
                    </div>
                  </div>
                </div>
              </div>
              <div class="w-12 shrink-0"></div>
            </div>
            
            <!-- [NEW] 하위 아이템 (Sub-items) 영역 -->
            <BoardGridSubItem 
              v-if="expandedItems[item.id]"
              :item="item"
              :groupId="group.id"
              :board="board"
              :depth="1"
              @update-item-value="handleUpdateItemValue"
              @update-item-name="(data) => $emit('update-item-name', data)"
              @add-sub-item="(data) => $emit('add-sub-item', data)"
              @open-detail="(id) => $emit('open-detail', id)"
              @delete-item="(id) => $emit('delete-item', id)"
            />
          </div>
        </template>
      </draggable>
      
      <!-- Add Item Row -->
      <div class="flex w-full items-stretch border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 hover:bg-white dark:hover:bg-slate-800 transition-colors">
        <div class="w-12 border-r border-slate-200 dark:border-slate-700 shrink-0"></div>
        <div class="w-[600px] shrink-0 border-r border-slate-200 dark:border-slate-700 px-1.5 py-1.5 flex items-center">
          <input 
            type="text" 
            placeholder="+ Add Item (Enter 입력 시 실시간 업무 추가)" 
            class="w-full bg-transparent border border-transparent rounded-lg px-3 py-1.5 text-xs text-slate-500 placeholder-slate-400 dark:placeholder-slate-500 transition-all duration-200 outline-none hover:border-slate-300 dark:hover:border-slate-700 hover:bg-slate-100/50 dark:hover:bg-slate-800/40 hover:text-slate-700 dark:hover:text-slate-200 focus:border-indigo-500 dark:focus:border-indigo-400 focus:bg-white dark:focus:bg-slate-900 focus:text-slate-900 dark:focus:text-white focus:ring-2 focus:ring-indigo-500/25 focus:shadow-sm font-medium" 
            @keyup.enter="handleAddItem($event, group.id)"
          />
        </div>
        <!-- 나머지 빈 공간 채우기 -->
        <!-- 🌟 [10년차 시니어 UI/UX]: 헤더의 draggable flex 컨테이너와 너비 정렬을 완벽하게 맞추기 위해 flex-1 래퍼 장착 🌟 -->
        <div class="flex flex-1">
          <div 
            v-for="col in localColumns" 
            :key="'empty-add-'+col.id" 
            class="shrink-0 border-r border-slate-200 dark:border-slate-700 flex items-center justify-center"
            :class="col.columnType === 'timeline' ? 'w-[180px]' : 'w-[120px]'"
          >
            &nbsp;
          </div>
        </div>
        <div class="w-12 shrink-0"></div>
      </div>
    </div>

    <div class="mt-4 mb-8 px-4 flex items-center">
      <div class="flex items-center gap-2 px-4 py-2 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 hover:border-indigo-400 dark:hover:border-indigo-500 rounded-xl w-[400px] shadow-sm hover:shadow-md transition-all duration-200 focus-within:ring-2 focus-within:ring-indigo-500/50">
        <Plus class="w-4 h-4 text-indigo-500" />
        <input 
          type="text" 
          placeholder="새로운 그룹 추가 (엔터 입력)"
          class="bg-transparent border-none outline-none text-sm font-semibold text-slate-700 dark:text-slate-200 w-full placeholder-slate-400"
          @keyup.enter="handleAddGroup"
        />
      </div>
    </div>
  </div>
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

/* [10년 차 시니어 프리미엄 UI/UX]: 컬럼 드래그 시 고스트 요소 스타일 정의 */
:deep(.column-ghost) {
  opacity: 0.4 !important;
  background-color: rgba(238, 242, 255, 0.5) !important; /* bg-indigo-50/50 */
}
.dark :deep(.column-ghost) {
  background-color: rgba(30, 27, 120, 0.2) !important; /* dark:bg-indigo-950/20 */
}
</style>
