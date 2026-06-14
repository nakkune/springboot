<script setup lang="ts">
import { ref, computed } from 'vue'
import draggable from 'vuedraggable'
import { MoreHorizontal, Calendar, ListTree, Plus, Trash2, Eye } from 'lucide-vue-next'
import { useBoardStore } from '@/stores/useBoardStore'
import api from '@/services/api' // 백엔드 실시간 동기화를 위한 API 임포트

const props = defineProps<{
  board: any
}>()

const emit = defineEmits<{
  (e: 'open-detail', itemId: string): void
  (e: 'add-sub-item', data: { name: string; groupId: string; parentItemId: string }): void
  (e: 'delete-item', itemId: string): void
  (e: 'update-item-value', data: { itemId: string; columnId: string; value: string }): void
  (e: 'update-item-name', data: { itemId: string; newName: string }): void
}>()

const boardStore = useBoardStore()

// 하위 아이템 토글(아코디언) 상태 관리 변수
const expandedSubItems = ref<Record<string, boolean>>({})

const toggleSubItems = (itemId: string) => {
  expandedSubItems.value[itemId] = !expandedSubItems.value[itemId]
}

// 10년 차 시니어 설계: 'status' 타입의 컬럼 정보 추출
const statusColumn = computed(() => {
  return props.board.columns?.find((c: any) => c.type === 'status' || c.columnType === 'status')
})

// 10년 차 시니어 설계: 'timeline' 타입의 컬럼 정보 추출
const timelineColumn = computed(() => {
  return props.board.columns?.find((c: any) => c.type === 'timeline' || c.columnType === 'timeline')
})

// 10년 차 시니어 설계: DB 세팅에 정의된 상태 옵션 목록을 동적으로 파싱하여 연동 (막힘 포함)
const getStatusOptions = computed(() => {
  const col = statusColumn.value
  if (col && col.settings) {
    try {
      const parsed = JSON.parse(col.settings)
      if (parsed.options && Array.isArray(parsed.options) && parsed.options.length > 0) {
        return parsed.options.map((o: any) => ({
          label: o.label,
          color: o.color || '#64748b'
        }))
      }
    } catch {}
  }
  // 기본 폴백 세팅 (막힘 포함)
  return [
    { label: '시작 전', color: '#C4C4C4' },
    { label: '진행 중', color: '#FDAB3D' },
    { label: '완료', color: '#00C875' },
    { label: '막힘', color: '#E2445C' }
  ]
})

// 10년 차 시니어 설계: 각 상태별로 보드의 모든 아이템 분류 및 정렬
const kanbanData = computed(() => {
  const options = getStatusOptions.value
  const data: Record<string, any[]> = {}
  
  options.forEach((opt: any) => {
    data[opt.label] = []
  })

  const col = statusColumn.value
  const colId = col ? col.id : null

  props.board.groups?.forEach((group: any) => {
    group.items?.forEach((item: any) => {
      // EAV 방식에 맞춰 상태 값을 추출 (컬럼 ID 매핑 최우선)
      const itemStatus = (colId ? item.values?.[colId] : null) || item.values?.['status'] || '시작 전'
      if (!data[itemStatus]) {
        data[itemStatus] = []
      }
      data[itemStatus].push({ ...item, _originalGroup: group.id })
    })
  })

  return options.map((opt: any) => ({
    title: opt.label,
    color: opt.color,
    items: data[opt.label] || []
  }))
})

// 10년 차 시니어 설계: 개별 아이템의 구체적인 상태명 및 색상 정보 바인딩
const getItemStatusDetail = (item: any) => {
  const col = statusColumn.value
  const colId = col ? col.id : null
  const statusName = (colId ? item.values?.[colId] : null) || item.values?.['status'] || '시작 전'
  
  const options = getStatusOptions.value
  const matched = options.find((o: any) => o.label === statusName)
  
  return {
    label: statusName,
    color: matched ? matched.color : '#64748b'
  }
}

// 10년 차 시니어 설계: 타임라인 데이터 포맷 변환 헬퍼 (Monday.com 감성 캡슐 구현)
const getTimelineText = (item: any) => {
  const col = timelineColumn.value
  if (!col) return null
  const value = item.values?.[col.id] || item.values?.[col.columnType]
  if (!value) return null
  try {
    const parsed = JSON.parse(value)
    if (!parsed.start && !parsed.end) return null
    
    const formatDateObj = (dateStr: string) => {
      if (!dateStr) return null
      const [y, m, d] = dateStr.split('-').map(Number)
      return { year: y, month: m, day: d }
    }
    
    const start = formatDateObj(parsed.start)
    const end = formatDateObj(parsed.end)
    
    if (!start && end) return `${end.month}월 ${end.day}일`
    if (start && !end) return `${start.month}월 ${start.day}일`
    if (start && end) {
      if (start.year !== end.year) {
        return `${start.year}.${String(start.month).padStart(2, '0')}.${String(start.day).padStart(2, '0')} - ${end.year}.${String(end.month).padStart(2, '0')}.${String(end.day).padStart(2, '0')}`
      }
      if (start.month !== end.month) {
        return `${start.month}월 ${start.day}일 - ${end.month}월 ${end.day}일`
      }
      if (start.day === end.day) {
        return `${start.month}월 ${start.day}일`
      }
      return `${start.month}월 ${start.day} - ${end.day}`
    }
  } catch {}
  return null
}

// 10년 차 시니어 설계: 하위 아이템의 완료 진행률 데이터 추출
const getSubItemsProgress = (item: any) => {
  if (!item.subItems || item.subItems.length === 0) return null
  
  const col = statusColumn.value
  const colId = col ? col.id : null
  
  const total = item.subItems.length
  const completed = item.subItems.filter((sub: any) => {
    const status = (colId ? sub.values?.[colId] : null) || sub.values?.['status'] || '시작 전'
    return status === '완료'
  }).length
  
  const percent = Math.round((completed / total) * 100)
  return {
    total,
    completed,
    percent
  }
}

// 하위 항목 실시간 즉각 등록 제출 바인딩
const handleAddSubItemSubmit = (event: Event, groupId: string, parentItemId: string) => {
  const target = event.target as HTMLInputElement
  const name = target.value.trim()
  if (!name) return
  emit('add-sub-item', { name, groupId, parentItemId })
  target.value = ''
  
  // 신규 항목 생성 후 아코디언이 접혀있다면 자동 열기 처리
  expandedSubItems.value[parentItemId] = true
}

// 칸반 드래그앤드롭 시 즉각 스토어 반영 및 백엔드 실시간 동기화
const onChange = async (event: any, newStatus: string) => {
  if (event.added) {
    const item = event.added.element
    const itemId = item.id
    
    // 1. 로컬 캐시 즉시 업데이트 (Optimistic UI)
    const storeItem = boardStore.items.find((i: any) => i.id === itemId)
    if (storeItem) {
      if (!storeItem.values) storeItem.values = {}
      
      const col = statusColumn.value
      const colId = col ? col.id : 'status'
      
      storeItem.values[colId] = newStatus
      storeItem.values['status'] = newStatus
    }
    
    // 2. 백엔드 EAV API 실시간 전송
    try {
      const col = statusColumn.value
      const colId = col ? col.id : 'status'
      
      await api.post('/item-values', {
        itemId: itemId,
        columnId: colId,
        valueText: newStatus
      })
    } catch (err) {
      console.error('Failed to sync kanban status change to backend', err)
    }
  }
}
</script>

<template>
  <div class="flex-1 overflow-x-auto overflow-y-hidden flex h-full bg-slate-50 dark:bg-slate-900/50 p-6 gap-6 select-none">
    
    <div v-for="column in kanbanData" :key="column.title" class="flex flex-col w-80 shrink-0 h-full">
      
      <!-- Column Header -->
      <div class="flex items-center justify-between mb-4 px-1">
        <div class="flex items-center gap-2">
          <!-- DB에서 지정한 세련된 커스텀 상태 컬러 반영 -->
          <div class="w-3 h-3 rounded-full shadow-sm" :style="{ backgroundColor: column.color }"></div>
          <h3 class="font-bold text-sm text-slate-700 dark:text-slate-200 tracking-wide">{{ column.title }}</h3>
          <span class="text-[10px] font-bold text-slate-400 bg-slate-200/60 dark:bg-slate-800 px-2 py-0.5 rounded-full">
            {{ column.items.length }}
          </span>
        </div>
      </div>

      <!-- Draggable Area -->
      <div class="flex-1 bg-slate-100/70 dark:bg-slate-800/30 rounded-2xl p-2.5 overflow-y-auto border border-slate-200/30 dark:border-slate-800/20 hover:border-slate-200 dark:hover:border-slate-700 transition-colors custom-scrollbar">
        <draggable 
          :list="column.items" 
          group="kanban" 
          item-key="id" 
          class="h-full min-h-[150px] flex flex-col gap-2.5"
          ghost-class="opacity-40"
          @change="(e: any) => onChange(e, column.title)"
        >
          <template #item="{ element }">
            <div 
              class="bg-white dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700/60 p-4 rounded-xl shadow-sm cursor-pointer hover:shadow-md hover:border-indigo-500/40 dark:hover:border-indigo-500/30 transition-all group/card active:scale-[0.99] flex flex-col gap-2.5"
              @click="emit('open-detail', element.id)"
            >
              <!-- 아이템 이름 -->
              <div class="flex justify-between items-start">
                <p class="font-semibold text-sm text-slate-800 dark:text-slate-100 leading-snug group-hover/card:text-indigo-500 dark:group-hover/card:text-indigo-400 transition-colors">{{ element.name }}</p>
                <MoreHorizontal class="w-4 h-4 text-slate-400 opacity-0 group-hover/card:opacity-100 hover:text-slate-600 transition-all cursor-pointer shrink-0 ml-2" @click.stop />
              </div>
              
              <!-- 10년 차 시니어 설계: 상태 배지 및 타임라인 시각화 탑재 -->
              <div class="flex flex-wrap gap-1.5 items-center">
                <!-- 상태 표시 배지 -->
                <span 
                  class="text-[10px] font-black px-2.5 py-0.5 rounded-full text-white shadow-sm tracking-wide"
                  :style="{ backgroundColor: getItemStatusDetail(element).color }"
                >
                  {{ getItemStatusDetail(element).label }}
                </span>
                
                <!-- 타임라인 표시 배지 (Monday.com 감성 캡슐) -->
                <span 
                  v-if="getTimelineText(element)"
                  class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-100 dark:border-indigo-900/50 flex items-center gap-1.5 shadow-sm"
                >
                  <Calendar class="w-3 h-3 text-indigo-500/70" />
                  {{ getTimelineText(element) }}
                </span>
              </div>

              <!-- 10년 차 시니어 설계: Monday.com 스타일 하위 아이템(Sub-items) 요약 바 -->
              <div 
                v-if="element.subItems && element.subItems.length > 0"
                @click.stop="toggleSubItems(element.id)"
                class="flex flex-col gap-1.5 mt-1.5 p-2 rounded-xl bg-slate-50/80 dark:bg-slate-900/40 hover:bg-slate-100 dark:hover:bg-slate-900 border border-slate-200/40 dark:border-slate-800/40 transition-all cursor-pointer group/subtoggle"
              >
                <div class="flex items-center justify-between text-[10px] font-bold text-slate-500 dark:text-slate-400">
                  <span class="flex items-center gap-1.5">
                    <ListTree class="w-3.5 h-3.5 text-indigo-500/70" />
                    하위 아이템 {{ getSubItemsProgress(element)?.completed }}/{{ getSubItemsProgress(element)?.total }}
                  </span>
                  <span class="text-[9px] text-indigo-500 dark:text-indigo-400 font-extrabold group-hover/subtoggle:underline transition-all">
                    {{ expandedSubItems[element.id] ? '접기 ▲' : '자세히 보기 ▼' }}
                  </span>
                </div>
                
                <!-- 부드러운 하위 진행률 마이크로 프로그레스 바 -->
                <div class="w-full h-1 bg-slate-200 dark:bg-slate-700/60 rounded-full overflow-hidden shrink-0">
                  <div 
                    class="bg-indigo-500 h-full transition-all duration-300 rounded-full" 
                    :style="{ width: getSubItemsProgress(element)?.percent + '%' }"
                  ></div>
                </div>
              </div>

              <!-- 하위 항목이 없을 때 신규 추가를 촉진하는 심플 단추 버튼 -->
              <div 
                v-else
                @click.stop="toggleSubItems(element.id)"
                class="w-fit text-[10px] font-bold text-slate-400 hover:text-indigo-500 dark:hover:text-indigo-400 transition-colors flex items-center gap-1 mt-1 cursor-pointer pl-1.5"
              >
                <Plus class="w-3 h-3 text-slate-400 hover:text-indigo-400" />
                하위 항목 추가
              </div>

              <!-- 10년 차 시니어 설계: Monday.com 감성 접이식 하위 아이템 디테일 리스트 -->
              <div 
                v-if="expandedSubItems[element.id]"
                class="flex flex-col gap-1.5 mt-1.5 pt-2.5 border-t border-slate-100 dark:border-slate-700/60"
                @click.stop
              >
                <div class="text-[9px] font-bold text-slate-400 tracking-wider mb-1 pl-1">하위 항목 리스트</div>
                
                <div class="flex flex-col gap-1.5 max-h-[180px] overflow-y-auto custom-scrollbar">
                  <div 
                    v-for="sub in element.subItems" 
                    :key="sub.id"
                    class="flex items-center justify-between gap-2 p-1.5 bg-slate-50/50 dark:bg-slate-900/40 border border-slate-200/20 dark:border-slate-800/30 rounded-lg hover:border-slate-300 dark:hover:border-slate-600 transition-all group/subrow"
                  >
                    <!-- 하위 아이템 이름 및 상태 도트 -->
                    <div class="flex items-center gap-2 min-w-0 flex-1">
                      <!-- 상태 도트 -->
                      <div 
                        class="w-2.5 h-2.5 rounded-full shrink-0 shadow-sm"
                        :style="{ backgroundColor: getItemStatusDetail(sub).color }"
                        :title="getItemStatusDetail(sub).label"
                      ></div>
                      
                      <!-- 이름 인라인 변경 -->
                      <input 
                        type="text" 
                        :value="sub.name"
                        @change="(e) => emit('update-item-name', { itemId: sub.id, newName: (e.target as HTMLInputElement).value })"
                        class="w-full bg-transparent border-none outline-none text-xs text-slate-700 dark:text-slate-200 focus:text-indigo-500 dark:focus:text-indigo-400 transition-colors font-medium truncate cursor-text"
                      />
                    </div>
                    
                    <!-- 액션 아이콘들 (상세, 삭제) -->
                    <div class="flex items-center gap-1 opacity-0 group-hover/subrow:opacity-100 shrink-0 transition-opacity">
                      <button 
                        @click.stop="emit('open-detail', sub.id)" 
                        class="p-0.5 hover:bg-slate-200 dark:hover:bg-slate-700 rounded text-slate-400 hover:text-slate-600 dark:hover:text-slate-300"
                        title="상세 보기"
                      >
                        <Eye class="w-3 h-3" />
                      </button>
                      <button 
                        @click.stop="emit('delete-item', sub.id)" 
                        class="p-0.5 hover:bg-red-50 dark:hover:bg-red-950/20 rounded text-slate-400 hover:text-red-500"
                        title="삭제"
                      >
                        <Trash2 class="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
                
                <!-- 하위 항목 실시간 퀵 등록 바 -->
                <div class="flex items-center gap-1.5 mt-1 px-2.5 py-1 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus-within:ring-2 focus-within:ring-indigo-500/25 transition-all">
                  <span class="text-xs text-indigo-400 font-black">+</span>
                  <input 
                    type="text" 
                    placeholder="새 하위 항목 등록 (Enter)" 
                    class="bg-transparent border-none outline-none text-[11px] w-full text-slate-700 dark:text-slate-200 placeholder-slate-400/80 font-medium"
                    @keyup.enter="handleAddSubItemSubmit($event, element._originalGroup || element.groupId, element.id)"
                  />
                </div>
              </div>

              <!-- 하단 푸터 (날짜 표시 & 담당자 아바타) -->
              <div class="flex items-center justify-between mt-1 pt-2.5 border-t border-slate-100 dark:border-slate-700/60">
                <span class="text-[11px] text-slate-400 dark:text-slate-500 font-semibold font-mono">
                  {{ element.values?.date || '일정 미지정' }}
                </span>
                <div v-if="element.values?.assignee" class="w-6 h-6 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px] font-black border border-slate-900 shadow-sm shrink-0">
                  {{ element.values.assignee.substring(0, 1) }}
                </div>
              </div>
            </div>
          </template>
        </draggable>
      </div>

    </div>

  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #334155;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #475569;
}
</style>
