<template>
  <div class="flex flex-col w-full border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30">
    <div class="flex flex-col">
      <!-- 서브태스크 목록 렌더링 -->
      <div v-for="sub in item.subItems" :key="sub.id" class="flex flex-col w-full">
        <div class="flex w-full items-stretch border-b border-slate-100/50 dark:border-slate-800/50 hover:bg-white dark:hover:bg-slate-800 transition-colors group/sub">
          <div class="w-12 flex items-center justify-center shrink-0">
            <div class="w-3.5 h-3.5 rounded border border-slate-300 dark:border-slate-600 opacity-50"></div>
          </div>
          
          <div class="w-[600px] shrink-0 border-r border-slate-200 dark:border-slate-700 py-1.5 flex items-stretch group/subname">
            <!-- 동적 인덴트 적용. depth 기본값 1이면 ml-6, depth 2면 ml-12 등 -->
            <div class="flex-1 flex items-center w-full border-l-2 border-indigo-400 pl-3" :style="{ marginLeft: `${depth * 1.5}rem` }">
              <!-- 더 하위 아이템 토글 버튼 -->
              <button 
                @click.stop="toggleExpand(sub.id)"
                class="mr-2 p-0.5 rounded-md hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-400 transition-colors shrink-0 flex items-center justify-center bg-slate-100 dark:bg-slate-800"
              >
                <ChevronDown v-if="expandedItems[sub.id]" class="w-3 h-3 text-indigo-500" />
                <ChevronRight v-else class="w-3 h-3" />
              </button>
              
              <!-- 10년 차 시니어 설계: 수평 플렉스 정렬을 고려해 flex-1 min-w-0 클래스 지정 -->
              <input 
                type="text" 
                :value="sub.name"
                :title="sub.name"
                @change="(e) => $emit('update-item-name', { itemId: sub.id, newName: (e.target as HTMLInputElement).value })"
                class="flex-1 min-w-0 bg-transparent outline-none group-hover/subname:text-indigo-400 transition-colors text-sm font-medium cursor-text truncate"
              />

              <!-- 10년 차 시니어 설계: 대안 3 - 하위 아이템에도 동일하게 D-Day 및 5단계 마이크로 도트 진행률 배지 탑재 -->
              <span 
                v-if="getDDayText(sub)"
                class="ml-2 shrink-0 px-2 py-0.5 rounded-full border text-[9px] font-black font-mono shadow-sm flex items-center gap-1 transition-all duration-300"
                :style="getDDayBadgeStyle(sub)"
              >
                <span>{{ getDDayText(sub) }}</span>
                <span class="tracking-widest opacity-80">{{ getDotProgress(sub) }}</span>
              </span>

              <div class="flex items-center gap-1 opacity-0 group-hover/sub:opacity-100 px-2 shrink-0">
                <button @click.stop="$emit('open-detail', sub.id)" class="p-1 hover:bg-slate-200 dark:hover:bg-slate-700 rounded text-slate-400" title="상세 보기">
                  <Maximize2 class="w-3 h-3" />
                </button>
                <button @click.stop="$emit('delete-item', sub.id)" class="p-1 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-slate-400 hover:text-red-500 transition-colors" title="삭제">
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
          </div>
          
          <!-- 서브 아이템 동적 컬럼 렌더링 -->
          <!-- 🌟 [10년차 시니어 UI/UX]: 헤더의 draggable flex 컨테이너와 너비 정렬을 완벽하게 맞추기 위해 flex-1 래퍼 장착 🌟 -->
          <div class="flex flex-1">
            <div 
              v-for="col in board.columns" 
              :key="'sub-'+col.id" 
              class="shrink-0 border-r border-slate-200 dark:border-slate-700 flex items-center justify-center relative p-1 text-xs text-slate-600 dark:text-slate-300"
              :class="col.columnType === 'timeline' ? 'w-[180px]' : 'w-[120px]'"
            >
              <!-- Status Cell (드롭다운컬럼과 동일한 settings 기반 동적 옵션 + 컬러 지원) -->
              <StatusColumn 
                v-if="col.columnType === 'status'"
                :value="sub.values[col.id] || sub.values[col.columnType]" 
                :itemId="sub.id" 
                :columnId="col.id" 
                :options="getStatusOptions(col)" 
                @update="handleUpdateItemValue"
              />
              <div v-else-if="col.columnType === 'person'" class="w-full h-full flex items-center justify-center cursor-pointer relative hover:z-[100]" @click.stop="togglePersonDropdown(sub.id)">
                <div v-if="sub.values[col.id] || sub.values[col.columnType]" class="w-6 h-6 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[9px] font-black relative group/person hover:scale-105 hover:z-[100] transition-all duration-150 shadow-sm shrink-0">
                  {{ (sub.values[col.id] || sub.values[col.columnType]).substring(0, 1) }}
                  
                  <!-- 🌟 [10년차 시니어 UI/UX 디자인]: 프리미엄 툴팁 프로필 카드 (왼쪽 팝업 - 라이트/다크 동적 대응) 🌟 -->
                  <div 
                    class="absolute right-full top-1/2 -translate-x-1/2 mr-2 w-max min-w-[120px] bg-white/95 dark:bg-slate-900/95 text-slate-800 dark:text-slate-100 rounded-xl shadow-2xl border border-slate-200/80 dark:border-slate-800/80 p-2.5 flex flex-col items-center gap-1.5 opacity-0 pointer-events-none group-hover/person:opacity-100 group-hover/person:pointer-events-auto transition-all duration-200 transform translate-x-1 group-hover/person:translate-x-0 z-[99999] backdrop-blur-md"
                    @click.stop
                  >
                    <div class="w-7 h-7 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center text-xs font-black text-white border border-white/20 shadow-md">
                      {{ (sub.values[col.id] || sub.values[col.columnType]).substring(0, 1) }}
                    </div>
                    <div class="flex flex-col items-center">
                      <span class="text-xs font-black tracking-tight whitespace-nowrap">{{ sub.values[col.id] || sub.values[col.columnType] }}</span>
                      <span class="text-[9px] text-slate-550 dark:text-slate-400 font-medium tracking-wide">담당자</span>
                    </div>
                    <div class="absolute left-full top-1/2 -translate-x-1/2 -mr-0.5 border-4 border-transparent border-l-white/95 dark:border-l-slate-900/95"></div>
                  </div>
                </div>
                <!-- 10년 차 시니어 프리미엄 UI: 담당자가 미지정일 때만 아담한 둥근 점선 +를 1개 노출하여 지정을 유도 (중복 원 노출 방지) -->
                <div v-else class="w-6 h-6 rounded-full bg-transparent flex items-center justify-center border border-dashed border-slate-300 dark:border-slate-650 hover:bg-slate-100 dark:hover:bg-slate-800 hover:border-indigo-400 transition-colors shrink-0">
                  <Plus class="w-3 h-3 text-indigo-500 dark:text-indigo-400" />
                </div>
                <div v-if="activePersonDropdownItemId === sub.id" class="absolute top-full left-0 mt-1 w-[150%] -ml-[25%] bg-slate-800 border border-slate-700 rounded-xl shadow-2xl z-[999] p-1.5 flex flex-col gap-1 overflow-hidden">
                  <div v-for="opt in assigneeOptions" :key="opt" @click.stop="selectPerson(sub.id, col.id, opt)" class="px-2 py-1.5 rounded-lg text-xs text-white hover:bg-slate-700 transition-colors cursor-pointer flex items-center gap-2"><span class="truncate">{{ opt }}</span></div>
                </div>
              </div>
              <!-- Date Cell (10년 차 시니어 설계: 테두리 컬러링 제어가 자유로운 고품격 커스텀 데이트 피커 탑재) -->
              <DateColumn 
                v-else-if="col.columnType === 'date'"
                :value="sub.values[col.id] || sub.values[col.columnType]"
                :itemId="sub.id"
                :columnId="col.id"
                @update="handleUpdateItemValue"
              />
              
              <!-- Timeline Cell (Monday.com 스타일 기간 막대) -->
              <TimelineColumn 
                v-else-if="col.columnType === 'timeline'"
                :value="sub.values[col.id] || sub.values[col.columnType]"
                :itemId="sub.id"
                :columnId="col.id"
                @update="handleUpdateItemValue"
              />
              
              <!-- Advanced Columns -->
              <DropdownColumn 
                v-else-if="col.columnType === 'dropdown'" 
                :value="sub.values[col.id] || sub.values[col.columnType]" 
                :itemId="sub.id" 
                :columnId="col.id" 
                :options="getDropdownOptions(col)" 
                @update="handleUpdateItemValue" 
              />
              <TagsColumn 
                v-else-if="col.columnType === 'tags'" 
                :value="sub.values[col.id] || sub.values[col.columnType]" 
                :itemId="sub.id" 
                :columnId="col.id" 
                @update="handleUpdateItemValue" 
              />
              <FileColumn 
                v-else-if="col.columnType === 'file'" 
                :value="sub.values[col.id] || sub.values[col.columnType]" 
                :itemId="sub.id" 
                :columnId="col.id" 
                @update="handleUpdateItemValue" 
              />
              <RichTextColumn 
                v-else-if="col.columnType === 'richtext' || col.columnType === 'editor'" 
                :value="sub.values[col.id] || sub.values[col.columnType]" 
                :itemId="sub.id" 
                :columnId="col.id" 
                @update="handleUpdateItemValue" 
              />
              
              <TextColumn 
                v-else-if="col.columnType === 'text'"
                :value="sub.values[col.id] || sub.values[col.columnType]"
                :itemId="sub.id"
                :columnId="col.id"
                @update="handleUpdateItemValue"
              />
              
              <!-- Priority Cell (별표 5개 입력기) -->
              <PriorityColumn
                v-else-if="col.columnType === 'priority'"
                :value="sub.values[col.id] || sub.values[col.columnType]"
                :itemId="sub.id"
                :columnId="col.id"
                @update="handleUpdateItemValue"
              />
              <div v-else class="w-full h-full flex items-center justify-center px-1 truncate">
                  <span class="truncate w-full text-center">{{ sub.values[col.id] || sub.values[col.columnType] || '-' }}</span>
              </div>
            </div>
          </div>
          <div class="w-12 shrink-0"></div>
        </div>

        <!-- [RECURSIVE CALL] 만약 현재 sub 객체가 하위 아이템(subItems)을 가지고 있고 토글이 열려 있다면, 자기 자신을 다시 호출 -->
        <BoardGridSubItem 
          v-if="expandedItems[sub.id] && sub.subItems && sub.subItems.length >= 0"
          :item="sub"
          :groupId="groupId"
          :board="board"
          :depth="depth + 1"
          @update-item-value="(data) => $emit('update-item-value', data)"
          @update-item-name="(data) => $emit('update-item-name', data)"
          @add-sub-item="(data) => $emit('add-sub-item', data)"
          @open-detail="(id) => $emit('open-detail', id)"
          @delete-item="(id) => $emit('delete-item', id)"
        />
      </div>
      
      <!-- 하위 아이템 추가 인풋 -->
      <div class="flex w-full items-stretch border-b border-slate-100/50 dark:border-slate-800/50 group/addsub hover:bg-white dark:hover:bg-slate-800 transition-colors">
        <div class="w-12 flex items-center justify-center shrink-0"></div>
        <div class="w-[600px] shrink-0 border-r border-slate-200 dark:border-slate-700 py-1 flex items-stretch">
          <div class="flex-1 flex items-center w-full border-l-2 border-indigo-400 pl-3 bg-transparent transition-all duration-200" :style="{ marginLeft: `${depth * 1.5}rem` }">
            <div class="flex items-center w-full px-3 py-1.5 border border-transparent rounded-lg transition-all duration-200 hover:border-indigo-200 dark:hover:border-indigo-900/60 hover:bg-indigo-50/30 dark:hover:bg-indigo-950/10 focus-within:border-indigo-500 dark:focus-within:border-indigo-400 focus-within:bg-white dark:focus-within:bg-slate-900 focus-within:ring-2 focus-within:ring-indigo-500/25 focus-within:shadow-sm group/subadd">
              <Plus class="w-3.5 h-3.5 text-indigo-400 mr-2 shrink-0 group-hover/subadd:scale-105 transition-transform" />
              <input 
                type="text"
                :placeholder="depth >= 3 ? '+ 3단계 아이템 추가' : '+ 하위 아이템 추가'"
                class="bg-transparent outline-none text-xs text-indigo-400 dark:text-indigo-300 placeholder-indigo-400/60 dark:placeholder-indigo-400/50 font-semibold w-full cursor-text"
                @keyup.enter="handleSubItemSubmit($event, item.id)"
              />
            </div>
          </div>
        </div>
        <!-- 나머지 빈 공간 채우기 -->
        <!-- 🌟 [10년차 시니어 UI/UX]: 헤더의 draggable flex 컨테이너와 너비 정렬을 완벽하게 맞추기 위해 flex-1 래퍼 장착 🌟 -->
        <div class="flex flex-1">
          <div 
            v-for="col in board.columns" 
            :key="'empty-'+col.id" 
            class="shrink-0 border-r border-slate-200 dark:border-slate-700 flex items-center justify-center"
            :class="col.columnType === 'timeline' ? 'w-[180px]' : 'w-[120px]'"
          >
            &nbsp;
          </div>
        </div>
        <div class="w-12 shrink-0"></div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ChevronRight, ChevronDown, Maximize2, Plus, Trash2, Settings } from 'lucide-vue-next'
import DropdownColumn from './columns/DropdownColumn.vue'
import StatusColumn from './columns/StatusColumn.vue'
import DateColumn from '@/features/board/columns/DateColumn.vue' // [NEW] 커스텀 데이트 피커 임포트
import TimelineColumn from '@/features/board/columns/TimelineColumn.vue' // [NEW] 타임라인 컬럼
import TagsColumn from '@/features/board/columns/TagsColumn.vue'
import FileColumn from '@/features/board/columns/FileColumn.vue'
import RichTextColumn from '@/features/board/columns/RichTextColumn.vue'
import TextColumn from '@/features/board/columns/TextColumn.vue'
import PriorityColumn from '@/features/board/columns/PriorityColumn.vue'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'

// 재귀 컴포넌트 이름 선언 (Vue 3 <script setup>에서는 자체 이름으로 호출 가능하지만 명시적 선언 권장)
defineOptions({
  name: 'BoardGridSubItem'
})

const props = defineProps<{
  item: any
  groupId: string
  board: any
  depth: number
}>()

const emit = defineEmits(['update-item-value', 'update-item-name', 'add-sub-item', 'open-detail', 'delete-item'])

// 시작·마감일 컬럼 ID 추출
const startDateColId = computed(() => {
  const col = props.board.columns?.find(
    (c: any) => c.name === '시작일' && (c.columnType === 'date' || c.type === 'date')
  )
  return col ? col.id : null
})

const endDateColId = computed(() => {
  const col = props.board.columns?.find(
    (c: any) => c.name === '마감일' && (c.columnType === 'date' || c.type === 'date')
  )
  return col ? col.id : null
})

// 전체 아이템 중 최소·최대 날짜 계산
const timelineBounds = computed(() => {
  if (!props.board.groups) return { min: null, max: null }
  
  const allItems = props.board.groups.flatMap((g: any) => g.items || [])
  const dates = allItems.flatMap((item: any) => {
    const s = startDateColId.value ? item.values[startDateColId.value] : null
    const e = endDateColId.value ? item.values[endDateColId.value] : null
    return [s, e].filter((v: any) => v) as string[]
  })
  
  if (dates.length === 0) return { min: null, max: null }
  const sorted = dates.map((d: any) => new Date(d)).sort((a: any, b: any) => a.getTime() - b.getTime())
  return { min: sorted[0], max: sorted[sorted.length - 1] }
})

// 시간 흐름 경과율 계산
const getTimelineProgress = (item: any): number => {
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

// 10년 차 시니어 설계: 대안 3 - 하위 아이템용 D-Day 및 마감 지연일 계산
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

// 10년 차 시니어 설계: 대안 3 - 하위 아이템용 5단계 마이크로 도트 진행률
const getDotProgress = (item: any): string => {
  const progress = getTimelineProgress(item)
  const filledCount = Math.min(5, Math.max(0, Math.round(progress / 20)))
  const emptyCount = 5 - filledCount
  return '●'.repeat(filledCount) + '○'.repeat(emptyCount)
}

// 10년 차 시니어 설계: 대안 3 - 하위 아이템용 위급도별 파스텔 톤 지능형 스타일 정의
const getDDayBadgeStyle = (item: any) => {
  const progress = getTimelineProgress(item)
  const dday = getDDayText(item)
  
  const isDelayed = dday && dday.includes('지연')
  
  if (isDelayed || progress >= 80) {
    // 긴급 및 지연 (부드러운 로즈 레드 테마)
    return {
      color: '#e11d48',
      backgroundColor: '#fff1f2',
      borderColor: '#fecdd3'
    }
  } else if (progress >= 50) {
    // 경고 및 주의 (따뜻한 앰버 오렌지 테마)
    return {
      color: '#d97706',
      backgroundColor: '#fef3c7',
      borderColor: '#fde68a'
    }
  }
  // 안전 (차분한 에메랄드 그린 테마)
  return {
    color: '#059669',
    backgroundColor: '#ecfdf5',
    borderColor: '#a7f3d0'
  }
}

// 상태 옵션을 settings에서 동적으로 읽어오는 헬퍼 (BoardGrid.vue와 동일)
const getStatusOptions = (col: any): { id?: string; label: string; color: string }[] => {
  if (col.settings) {
    try {
      const parsed = JSON.parse(col.settings)
      if (parsed.options && Array.isArray(parsed.options) && parsed.options.length > 0) {
        if (typeof parsed.options[0] === 'object') {
          return parsed.options as { id?: string; label: string; color: string }[]
        }
      }
    } catch {}
  }
  return [
    { id: '3', label: '시작 전', color: '#64748b' },
    { id: '2', label: '진행 중', color: '#3b82f6' },
    { id: '1', label: '완료', color: '#22c55e' }
  ]
}

const workspaceStore = useWorkspaceStore()
const assigneeOptions = computed(() => {
  const memberNames = workspaceStore.currentWorkspaceMembers.map((m: any) => m.fullName)
  return ['미지정', ...memberNames]
})

// 로컬 상태들
const activePersonDropdownItemId = ref<string | null>(null)
const expandedItems = ref<Record<string, boolean>>({}) // 각 로우마다 자신의 자식들 토글 상태 관리

const toggleExpand = (childId: string) => {
  expandedItems.value[childId] = !expandedItems.value[childId]
}

const togglePersonDropdown = (childId: string) => {
  if (activePersonDropdownItemId.value === childId) {
    activePersonDropdownItemId.value = null
  } else {
    activePersonDropdownItemId.value = childId
  }
}

const selectPerson = (childId: string, columnId: string, value: string) => {
  emit('update-item-value', { itemId: childId, columnId, value })
  activePersonDropdownItemId.value = null
}

const handleUpdateItemValue = (data: { itemId: string, columnId: string, value: string }) => {
  emit('update-item-value', data)
}

const handleSubItemSubmit = (event: Event, parentItemId: string) => {
  const target = event.target as HTMLInputElement
  const name = target.value.trim()
  if (!name) return
  // 현재 아이템(부모)의 id를 parentItemId로 사용, 최상위 groupId는 props로 전달받은 것 사용
  emit('add-sub-item', { name, groupId: props.groupId, parentItemId })
  target.value = ''
}

// 드롭다운 옵션 파서 (settings JSONB 동기화)
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
</script>
