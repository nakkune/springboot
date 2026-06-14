<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight } from 'lucide-vue-next'

// Props 및 Emits 정의 (v-model 완벽 지원)
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '날짜 선택'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)

// 달력에서 현재 보여주는 연도와 월 (월은 0 ~ 11)
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth())

// 요일 배열 (한국어 설정)
const weekdays = ['일', '월', '화', '수', '목', '금', '토']

// 오늘 날짜 문자열 (YYYY-MM-DD 형식)
const todayStr = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const date = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${date}`
})

// 입력값 변경 시 캘린더 화면 포커싱 동기화
watch(() => props.modelValue, (newVal) => {
  if (newVal && /^\d{4}-\d{2}-\d{2}$/.test(newVal)) {
    const [y, m] = newVal.split('-').map(Number)
    viewYear.value = y
    viewMonth.value = m - 1
  }
}, { immediate: true })

// 토글 함수
function toggleCalendar() {
  if (props.disabled) return
  isOpen.value = !isOpen.value
}

// 이전 달로 이동
function prevMonth() {
  if (viewMonth.value === 0) {
    viewMonth.value = 11
    viewYear.value--
  } else {
    viewMonth.value--
  }
}

// 다음 달로 이동
function nextMonth() {
  if (viewMonth.value === 11) {
    viewMonth.value = 0
    viewYear.value++
  } else {
    viewMonth.value++
  }
}

// 날짜 선택 처리
function selectDate(dateStr: string) {
  emit('update:modelValue', dateStr)
  emit('change', dateStr)
  isOpen.value = false
}

// 달력 그리드를 채울 날짜 목록 계산 (7열 x 6행 = 42일 구조)
const calendarDays = computed(() => {
  const year = viewYear.value
  const month = viewMonth.value

  // 이번 달의 첫 날 요일 (0: 일요일 ~ 6: 토요일)
  const firstDayOfWeek = new Date(year, month, 1).getDay()
  
  // 이번 달의 마지막 날짜
  const lastDateOfCurrentMonth = new Date(year, month + 1, 0).getDate()
  
  // 이전 달의 마지막 날짜
  const lastDateOfPrevMonth = new Date(year, month, 0).getDate()

  const days = []

  // 1. 이전 달의 잔여 날짜들 채우기
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const prevDate = lastDateOfPrevMonth - i
    const prevMonthVal = month === 0 ? 11 : month - 1
    const prevYearVal = month === 0 ? year - 1 : year
    const dStr = `${prevYearVal}-${String(prevMonthVal + 1).padStart(2, '0')}-${String(prevDate).padStart(2, '0')}`
    
    days.push({
      day: prevDate,
      dateStr: dStr,
      isCurrentMonth: false
    })
  }

  // 2. 이번 달의 날짜들 채우기
  for (let i = 1; i <= lastDateOfCurrentMonth; i++) {
    const dStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({
      day: i,
      dateStr: dStr,
      isCurrentMonth: true
    })
  }

  // 3. 다음 달의 초입 날짜들 채워서 42칸(7열 6행) 완성하기
  const remainingCells = 42 - days.length
  for (let i = 1; i <= remainingCells; i++) {
    const nextMonthVal = month === 11 ? 0 : month + 1
    const nextYearVal = month === 11 ? year + 1 : year
    const dStr = `${nextYearVal}-${String(nextMonthVal + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    
    days.push({
      day: i,
      dateStr: dStr,
      isCurrentMonth: false
    })
  }

  return days
})

// 외부 영역 클릭 감지하여 닫기
function handleClickOutside(event: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})
</script>

<template>
  <div ref="containerRef" class="relative w-full">
    <!-- 입력 트리거 필드 -->
    <div
      @click="toggleCalendar"
      class="flex items-center justify-between w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white cursor-pointer select-none focus-within:ring-2 focus-within:ring-indigo-500/40 hover:border-slate-400 dark:hover:border-slate-500 transition-colors"
      :class="{ 'opacity-60 cursor-not-allowed': disabled }"
    >
      <span v-if="modelValue" class="font-medium text-slate-900 dark:text-slate-100">
        {{ modelValue }}
      </span>
      <span v-else class="text-slate-400 dark:text-slate-500">
        {{ placeholder }}
      </span>
      <CalendarIcon class="w-4 h-4 text-slate-400 dark:text-slate-500 shrink-0 ml-2" />
    </div>

    <!-- 🌟 커스텀 달력 팝업 창 (핵심 테두리: border-slate-300 / dark:border-slate-700 적용, 크기 20% 축소) -->
    <div
      v-if="isOpen"
      class="absolute left-0 mt-1.5 z-[9999] w-56 p-3 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-xl shadow-xl space-y-2 select-none"
    >
      <!-- 달력 헤더 (연/월 표시 및 조작 - 크기 축소) -->
      <div class="flex items-center justify-between pb-1 border-b border-slate-100 dark:border-slate-800">
        <button
          type="button"
          @click="prevMonth"
          class="p-0.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 dark:text-slate-400 transition-colors cursor-pointer border-none"
        >
          <ChevronLeft class="w-3.5 h-3.5" />
        </button>
        <span class="text-xs font-bold text-slate-800 dark:text-slate-200">
          {{ viewYear }}년 {{ viewMonth + 1 }}월
        </span>
        <button
          type="button"
          @click="nextMonth"
          class="p-0.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 dark:text-slate-400 transition-colors cursor-pointer border-none"
        >
          <ChevronRight class="w-3.5 h-3.5" />
        </button>
      </div>

      <!-- 요일 텍스트 그리드 (글자 크기 축소) -->
      <div class="grid grid-cols-7 gap-1 text-center">
        <span
          v-for="(day, idx) in weekdays"
          :key="day"
          class="text-[10px] font-semibold"
          :class="[
            idx === 0 ? 'text-red-500' : idx === 6 ? 'text-blue-500' : 'text-slate-400 dark:text-slate-500'
          ]"
        >
          {{ day }}
        </span>
      </div>

      <!-- 날짜 목록 그리드 (날짜 칸 및 글자 크기 축소) -->
      <div class="grid grid-cols-7 gap-1 text-center">
        <button
          v-for="d in calendarDays"
          :key="d.dateStr"
          type="button"
          @click="selectDate(d.dateStr)"
          class="w-6 h-6 mx-auto flex items-center justify-center rounded-lg text-[10px] font-semibold transition-colors cursor-pointer border-none"
          :class="[
            // 1. 현재 달과 다른 달 날짜 색상 대비 구분
            d.isCurrentMonth
              ? 'text-slate-800 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
              : 'text-slate-300 dark:text-slate-600 hover:bg-slate-100/50 dark:hover:bg-slate-800/50',
            
            // 2. 오늘 날짜 스타일링 (선명한 파란색 링)
            d.dateStr === todayStr && d.dateStr !== modelValue ? 'border border-indigo-500 text-indigo-600 dark:text-indigo-400' : '',
            
            // 3. 선택된 날짜 하이라이트 (브랜드 컬러 배경 채우기)
            d.dateStr === modelValue ? 'bg-indigo-600 text-white font-bold hover:bg-indigo-700' : ''
          ]"
        >
          {{ d.day }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 커스텀 데이트피커를 위한 스타일 격리 */
</style>
