<!-- src/features/board/columns/DateColumn.vue -->
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Calendar, ChevronLeft, ChevronRight, X } from 'lucide-vue-next'

const props = defineProps<{
  value: string | null | undefined
  itemId: string
  columnId: string
}>()

const emit = defineEmits<{
  (e: 'update', data: { itemId: string, columnId: string, value: string }): void
}>()

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)

// 현재 달력에서 보고 있는 연도와 월 (1~12)
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)

// 선택된 날짜 파싱
const selectedDate = computed(() => {
  if (!props.value) return null
  const d = new Date(props.value)
  return isNaN(d.getTime()) ? null : d
})

// 달력 토글 (열려 있으면 닫고, 닫혀 있으면 열기)
const toggleCalendar = () => {
  if (isOpen.value) {
    isOpen.value = false
  } else {
    if (selectedDate.value) {
      currentYear.value = selectedDate.value.getFullYear()
      currentMonth.value = selectedDate.value.getMonth() + 1
    } else {
      const today = new Date()
      currentYear.value = today.getFullYear()
      currentMonth.value = today.getMonth() + 1
    }
    isOpen.value = true
  }
}

// 달력 닫기
const closeCalendar = () => {
  isOpen.value = false
}

// 외부 클릭 시 달력 닫기
const handleClickOutside = (event: MouseEvent) => {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    closeCalendar()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 이전 달로 이동
const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

// 다음 달로 이동
const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

// 날짜 선택 시 포맷하여 부모에게 전달
const selectDate = (day: number) => {
  const yyyy = currentYear.value
  const mm = String(currentMonth.value).padStart(2, '0')
  const dd = String(day).padStart(2, '0')
  const dateStr = `${yyyy}-${mm}-${dd}`
  
  emit('update', {
    itemId: props.itemId,
    columnId: props.columnId,
    value: dateStr
  })
  closeCalendar()
}

// 날짜 비우기
const clearDate = () => {
  emit('update', {
    itemId: props.itemId,
    columnId: props.columnId,
    value: ''
  })
  closeCalendar()
}

// 오늘 날짜로 즉시 지정 및 달력 연/월 포커스 동기화 (TS1345 void 연산 방지)
const selectToday = () => {
  const today = new Date()
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth() + 1
  selectDate(today.getDate())
}

// 달력 그리드를 위한 일자 계산
const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value, 0).getDate()
})

const startDayOfWeek = computed(() => {
  // 0: 일요일, 1: 월요일, ...
  return new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
})

// 달력에 표시할 날짜 배열 생성
const calendarCells = computed(() => {
  const cells = []
  
  // 이전 달의 패딩 공간 생성
  for (let i = 0; i < startDayOfWeek.value; i++) {
    cells.push({ day: null, isCurrentMonth: false })
  }
  
  // 이번 달 일자 채우기
  for (let d = 1; d <= daysInMonth.value; d++) {
    cells.push({ day: d, isCurrentMonth: true })
  }
  
  return cells
})

// 오늘 날짜인지 판별
const isToday = (day: number) => {
  const today = new Date()
  return (
    today.getDate() === day &&
    today.getMonth() + 1 === currentMonth.value &&
    today.getFullYear() === currentYear.value
  )
}

// 선택된 날짜인지 판별
const isSelected = (day: number) => {
  if (!selectedDate.value) return false
  return (
    selectedDate.value.getDate() === day &&
    selectedDate.value.getMonth() + 1 === currentMonth.value &&
    selectedDate.value.getFullYear() === currentYear.value
  )
}

// 화면에 보일 날짜 포맷 (예: 2026-05-29)
const formattedValue = computed(() => {
  if (!props.value) return ''
  return props.value
})
</script>

<template>
  <div 
    ref="containerRef" 
    class="relative w-full h-full flex items-center justify-center"
    :class="{ 'z-50': isOpen }"
  >
    
    <!-- 셀 클릭 영역 (10년 차 시니어: 은은한 실선 테두리가 들어간 고급 Input Box 디자인) -->
    <div 
      @click.stop="toggleCalendar"
      class="w-full max-w-[105px] h-[26px] flex items-center justify-between px-2 cursor-pointer transition-all rounded-md bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:border-slate-350 dark:hover:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800/40 shadow-sm group/datecell"
      :class="[
        isOpen ? 'border-indigo-400 ring-2 ring-indigo-500/10' : ''
      ]"
    >
      <span class="text-[11px] font-semibold text-slate-700 dark:text-slate-200 truncate pr-1">
        {{ formattedValue || 'YYYY-MM-DD' }}
      </span>
      <Calendar 
        class="w-3 h-3 text-slate-400 group-hover/datecell:text-indigo-500 transition-colors shrink-0"
        :class="{ 'text-indigo-500': isOpen }"
      />
    </div>

    <!-- 🌟 [10년차 시니어 UI/UX 디자인]: 커스텀 프리미엄 달력 팝업 (테두리 선색을 명확히 구현) 🌟 -->
    <transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="transform scale-95 opacity-0 -translate-y-2"
      enter-to-class="transform scale-100 opacity-100 translate-y-0"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="transform scale-100 opacity-100 translate-y-0"
      leave-to-class="transform scale-95 opacity-0 -translate-y-2"
    >
      <div 
        v-if="isOpen" 
        class="absolute top-full left-1/2 -translate-x-1/2 mt-1 w-[176px] min-w-[176px] bg-white dark:bg-slate-900 rounded-xl shadow-2xl z-[9999] p-2 flex flex-col gap-1.5 transition-all border border-slate-200/60 dark:border-slate-800/80 select-none animate-in fade-in"
      >
        <!-- 10년 차 시니어 설계: 우측 상단 퀵 닫기 X 버튼 탑재 (헤더 레이아웃 방해 방지) -->
        <button 
          @click.stop="closeCalendar"
          class="absolute top-1.5 right-1.5 p-0.5 rounded text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all z-20 cursor-pointer"
          title="닫기"
        >
          <X class="w-2.5 h-2.5" />
        </button>

        <!-- 팝업 헤더 (연/월 조절 및 닫기) -->
        <div class="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-1 pr-5">
          <button 
            @click.stop="prevMonth"
            class="p-0.5 rounded hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400 transition-colors"
          >
            <ChevronLeft class="w-3 h-3" />
          </button>
          
          <span class="text-[10px] font-black text-slate-800 dark:text-slate-100 tracking-tight whitespace-nowrap">
            {{ currentYear }}년 {{ currentMonth }}월
          </span>
          
          <button 
            @click.stop="nextMonth"
            class="p-0.5 rounded hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400 transition-colors"
          >
            <ChevronRight class="w-3 h-3" />
          </button>
        </div>

        <!-- 요일 헤더 -->
        <div class="grid grid-cols-7 gap-0.5 text-center text-[8.5px] font-black text-slate-400 uppercase tracking-wider whitespace-nowrap">
          <span class="text-red-500">일</span>
          <span>월</span>
          <span>화</span>
          <span>수</span>
          <span>목</span>
          <span>금</span>
          <span class="text-blue-500">토</span>
        </div>

        <!-- 달력 날짜 그리드 -->
        <div class="grid grid-cols-7 gap-0.5">
          <div 
            v-for="(cell, index) in calendarCells" 
            :key="index"
            class="h-5 flex items-center justify-center text-[9px] font-bold rounded relative"
          >
            <button
              v-if="cell.day"
              @click.stop="selectDate(cell.day)"
              class="w-full h-full flex items-center justify-center rounded transition-all focus:outline-none"
              :class="[
                isSelected(cell.day) 
                  ? 'bg-indigo-600 text-white font-extrabold shadow-sm' 
                  : isToday(cell.day)
                    ? 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 font-bold border border-indigo-200/50 dark:border-indigo-900/50'
                    : 'hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
              ]"
            >
              {{ cell.day }}
            </button>
          </div>
        </div>

        <!-- 팝업 푸터 (초기화 및 오늘 날짜 바로 가기) -->
        <div class="flex items-center justify-between border-t border-slate-100 dark:border-slate-800 pt-1.5 mt-0.5 text-[8.5px]">
          <button 
            @click.stop="clearDate"
            class="px-1 py-0.5 rounded text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 font-extrabold transition-colors flex items-center gap-0.5"
          >
            <X class="w-2.5 h-2.5" /> 초기화
          </button>
          
          <button 
            @click.stop="selectToday"
            class="px-1 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 rounded hover:bg-slate-200 dark:hover:bg-slate-700 font-extrabold transition-all"
          >
            오늘 지정
          </button>
        </div>

      </div>
    </transition>

  </div>
</template>

<style scoped>
.animate-in {
  animation: fadeIn 0.15s ease-out forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(100%);
  }
}
</style>
