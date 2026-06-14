<script setup lang="ts">
import { ref, computed } from 'vue'
import { Calendar, X, ChevronLeft, ChevronRight, Check } from 'lucide-vue-next'

const props = defineProps<{
  value: string
  itemId: string
  columnId: string
}>()

const emit = defineEmits(['update'])

const isOpen = ref(false)
const startDate = ref('')
const endDate = ref('')
const hoverDate = ref('')

// 달력 탐색 연도 및 월
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth()) // 0~11

// 저장된 JSON 값 파싱
const parsedValue = computed(() => {
  if (!props.value) return null
  try {
    return JSON.parse(props.value)
  } catch {
    return null
  }
})

const displayStart = computed(() => parsedValue.value?.start || '')
const displayEnd = computed(() => parsedValue.value?.end || '')

const getTodayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const openEditor = () => {
  startDate.value = displayStart.value
  endDate.value = displayEnd.value
  isOpen.value = true
  
  // 에디터 열릴 때 달력 위치를 시작일 기준으로 맞춤
  if (startDate.value) {
    const d = new Date(startDate.value)
    if (!isNaN(d.getTime())) {
      currentYear.value = d.getFullYear()
      currentMonth.value = d.getMonth()
    }
  } else {
    const d = new Date()
    currentYear.value = d.getFullYear()
    currentMonth.value = d.getMonth()
  }
}

const toggleEditor = () => {
  if (isOpen.value) {
    isOpen.value = false
  } else {
    openEditor()
  }
}

const save = () => {
  const value = JSON.stringify({ start: startDate.value, end: endDate.value })
  emit('update', { itemId: props.itemId, columnId: props.columnId, value })
  isOpen.value = false
}

const clearValue = () => {
  startDate.value = ''
  endDate.value = ''
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: '' })
  isOpen.value = false
}

// 오늘부터 7일간 기본값
const setDefaultRange = () => {
  const today = new Date()
  const weekLater = new Date()
  weekLater.setDate(today.getDate() + 7)
  
  const formatDate = (d: Date) => {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }
  
  startDate.value = formatDate(today)
  endDate.value = formatDate(weekLater)
}

// 에메랄드 뱃지용 텍스트 포맷터 (Monday.com 스타일)
const dateRangeText = computed(() => {
  if (!displayStart.value && !displayEnd.value) return null
  
  const formatDateObj = (dateStr: string) => {
    if (!dateStr) return null
    const [y, m, d] = dateStr.split('-').map(Number)
    return { year: y, month: m, day: d }
  }
  
  const start = formatDateObj(displayStart.value)
  const end = formatDateObj(displayEnd.value)
  
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
  return null
})

// 년/월/일 날짜 형식 포맷터 (YYYY/MM/DD)
const startDateFormat = computed(() => {
  if (!startDate.value) return ''
  const [y, m, d] = startDate.value.split('-')
  return `${y}/${m}/${d}`
})

const endDateFormat = computed(() => {
  if (!endDate.value) return ''
  const [y, m, d] = endDate.value.split('-')
  return `${y}/${m}/${d}`
})

// 총 선택 일수 계산
const selectedDaysCount = computed(() => {
  if (startDate.value && endDate.value) {
    const s = new Date(startDate.value)
    const e = new Date(endDate.value)
    if (!isNaN(s.getTime()) && !isNaN(e.getTime())) {
      return Math.round((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24)) + 1
    }
  } else if (startDate.value && hoverDate.value) {
    const s = new Date(startDate.value)
    const h = new Date(hoverDate.value)
    if (!isNaN(s.getTime()) && !isNaN(h.getTime())) {
      return Math.abs(Math.round((h.getTime() - s.getTime()) / (1000 * 60 * 60 * 24))) + 1
    }
  }
  return 0
})

// 캘린더 날짜 생성을 위한 정보
interface CalendarDay {
  dateStr: string
  dayNum: number
  isCurrentMonth: boolean
  isToday: boolean
}

const calendarDays = computed<CalendarDay[]>(() => {
  const year = currentYear.value
  const month = currentMonth.value

  const firstDay = new Date(year, month, 1)
  const firstDayOfWeek = (firstDay.getDay() + 6) % 7 // 월요일=0, 일요일=6

  const lastDay = new Date(year, month + 1, 0)
  const lastDate = lastDay.getDate()

  const prevMonthLastDay = new Date(year, month, 0)
  const prevMonthLastDate = prevMonthLastDay.getDate()

  const days: CalendarDay[] = []
  const todayStr = getTodayStr()

  // 1. 이전 달 날짜
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const d = prevMonthLastDate - i
    const prevMonth = month === 0 ? 11 : month - 1
    const prevYear = month === 0 ? year - 1 : year
    const dateStr = `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      dateStr,
      dayNum: d,
      isCurrentMonth: false,
      isToday: dateStr === todayStr
    })
  }

  // 2. 이번 달 날짜
  for (let d = 1; d <= lastDate; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      dateStr,
      dayNum: d,
      isCurrentMonth: true,
      isToday: dateStr === todayStr
    })
  }

  // 3. 다음 달 날짜로 42칸 완성
  const remaining = 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const nextMonth = month === 11 ? 0 : month + 1
    const nextYear = month === 11 ? year + 1 : year
    const dateStr = `${nextYear}-${String(nextMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      dateStr,
      dayNum: d,
      isCurrentMonth: false,
      isToday: dateStr === todayStr
    })
  }

  return days
})

// 날짜 탐색 이동 함수
const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

// 연도 범위 리스트 생성 (현재 년도 ± 10년)
const yearsRange = computed(() => {
  const current = new Date().getFullYear()
  const list = []
  for (let y = current - 10; y <= current + 10; y++) {
    list.push(y)
  }
  return list
})

// 날짜 클릭 시 동작
const handleDateClick = (dateStr: string) => {
  if (!startDate.value || (startDate.value && endDate.value)) {
    // 시작일 설정 및 종료일 초기화
    startDate.value = dateStr
    endDate.value = ''
  } else {
    // 종료일 설정 또는 시작일 변경
    if (dateStr < startDate.value) {
      startDate.value = dateStr
      endDate.value = ''
    } else {
      endDate.value = dateStr
    }
  }
}

// 마우스 호버 핸들링
const handleDateMouseEnter = (dateStr: string) => {
  if (startDate.value && !endDate.value) {
    hoverDate.value = dateStr
  }
}

const handleDateMouseLeave = () => {
  hoverDate.value = ''
}

// 캘린더 그리기 도우미
const isSelectedStart = (dateStr: string) => {
  if (startDate.value === dateStr) return true
  // 시작일만 있을 때 호버 날짜가 시작일보다 전이면 호버 날짜가 임시 시작일이 됨
  if (startDate.value && !endDate.value && hoverDate.value) {
    return hoverDate.value === dateStr && hoverDate.value < startDate.value
  }
  return false
}

const isSelectedEnd = (dateStr: string) => {
  if (endDate.value === dateStr) return true
  // 시작일만 있을 때 호버 날짜가 시작일보다 뒤면 호버 날짜가 임시 종료일이 됨
  if (startDate.value && !endDate.value && hoverDate.value) {
    return hoverDate.value === dateStr && hoverDate.value > startDate.value
  }
  return false
}

const isBetweenRange = (dateStr: string) => {
  const start = startDate.value
  const end = endDate.value
  const hover = hoverDate.value

  if (start && end) {
    return dateStr > start && dateStr < end
  }
  if (start && !end && hover) {
    if (hover > start) {
      return dateStr > start && dateStr < hover
    } else if (hover < start) {
      return dateStr > hover && dateStr < start
    }
  }
  return false
}

const hasConnectingTrack = (dateStr: string) => {
  return isSelectedStart(dateStr) || isSelectedEnd(dateStr) || isBetweenRange(dateStr)
}
</script>

<template>
  <div class="relative w-full h-full flex items-center justify-center">
    <!-- 표시 영역: 에메랄드 그린 Monday.com 스타일 뱃지 (달력 활성화 시에도 노출 유지) -->
    <div
      @click.stop="toggleEditor"
      class="w-full h-full flex items-center justify-center cursor-pointer transition-colors"
    >
      <div
        v-if="dateRangeText"
        class="w-fit max-w-[95%] bg-[#00c875] text-white rounded-full px-3 py-1 flex items-center justify-center gap-1 text-[11px] font-semibold hover:scale-[1.02] hover:brightness-[1.03] active:scale-[0.98] transition-all duration-200 shadow-sm"
      >
        <span class="truncate">{{ dateRangeText }}</span>
      </div>
      
      <!-- 날짜 범위가 지정되지 않은 경우의 세련된 비어있는 캡슐 (이미지 매칭: 투명 배경에 은은한 점선 테두리) -->
      <div
        v-else
        class="w-fit max-w-[95%] bg-transparent hover:bg-slate-100/60 dark:hover:bg-slate-800/40 text-slate-400/90 dark:text-slate-500 border border-dashed border-slate-250 dark:border-slate-700/80 rounded-full px-3 py-1 flex items-center justify-center gap-1.5 text-[11px] font-semibold hover:text-slate-600 dark:hover:text-slate-300 transition-all duration-200"
      >
        <Calendar class="w-3 h-3 text-slate-400" />
        <span>기간 설정</span>
      </div>
    </div>

    <!-- 편집 팝업: Monday.com 스타일 날짜 설정 창 (콤팩트 50% 축소형) -->
    <div
      v-if="isOpen"
      class="absolute top-full left-1/2 -translate-x-1/2 mt-1 w-[185px] bg-white/95 dark:bg-slate-900/95 border border-slate-200 dark:border-slate-800 rounded-xl shadow-2xl z-[9999] p-2.5 flex flex-col gap-1.5 backdrop-blur-xl"
      @click.stop
    >
      <!-- 10년 차 시니어 설계: 우측 상단 퀵 닫기 X 버튼 탑재 (헤더 레이아웃 방해 방지) -->
      <button 
        @click.stop="isOpen = false"
        class="absolute top-2.5 right-2.5 p-0.5 rounded text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all z-20 cursor-pointer"
        title="닫기"
      >
        <X class="w-3 h-3" />
      </button>

      <!-- 팝업 타이틀 및 선택 일수 정보 -->
      <div class="flex items-center justify-between pb-1 border-b border-slate-100 dark:border-slate-800 pr-5">
        <span class="text-[10px] font-bold text-slate-800 dark:text-white">날짜 설정</span>
        <span v-if="selectedDaysCount > 0" class="text-[8.5px] text-slate-500 dark:text-slate-400 font-semibold bg-slate-100 dark:bg-slate-800 px-1 py-0.2 rounded">
          {{ selectedDaysCount }}일
        </span>
      </div>

      <!-- 듀얼 날짜 인풋 상자 -->
      <div class="grid grid-cols-2 gap-1">
        <div class="flex flex-col gap-1">
          <input
            type="text"
            :value="startDateFormat"
            placeholder="시작일"
            readonly
            class="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded px-1 py-0.8 text-[9px] text-center text-slate-700 dark:text-slate-200 font-mono outline-none transition-colors"
          />
        </div>
        <div class="flex flex-col gap-1">
          <input
            type="text"
            :value="endDateFormat"
            placeholder="종료일"
            readonly
            class="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded px-1 py-0.8 text-[9px] text-center text-slate-700 dark:text-slate-200 font-mono outline-none transition-colors"
          />
        </div>
      </div>

      <!-- 달 제어 영역 (이전/이후 버튼 및 드롭다운) -->
      <div class="flex items-center justify-between px-0.5 mt-0.5">
        <div class="flex items-center gap-0.2">
          <!-- 년 선택 드롭다운 -->
          <select
            v-model="currentYear"
            class="bg-transparent border-none text-[9.5px] font-bold text-slate-700 dark:text-slate-200 focus:outline-none cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800 rounded px-0.5 py-0.2"
          >
            <option v-for="y in yearsRange" :key="y" :value="y" class="bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-100">
              {{ y }}년
            </option>
          </select>

          <!-- 월 선택 드롭다운 -->
          <select
            v-model="currentMonth"
            class="bg-transparent border-none text-[9.5px] font-bold text-slate-700 dark:text-slate-200 focus:outline-none cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800 rounded px-0.5 py-0.2"
          >
            <option v-for="(m, idx) in 12" :key="idx" :value="idx" class="bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-100">
              {{ idx + 1 }}월
            </option>
          </select>
        </div>
        
        <div class="flex items-center gap-0.2">
          <button
            @click="prevMonth"
            class="p-0.5 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400 rounded transition-colors"
          >
            <ChevronLeft class="w-3 h-3" />
          </button>
          <button
            @click="nextMonth"
            class="p-0.5 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400 rounded transition-colors"
          >
            <ChevronRight class="w-3 h-3" />
          </button>
        </div>
      </div>

      <!-- 요일 헤더 -->
      <div class="grid grid-cols-7 text-center text-[8.5px] font-bold text-slate-400 dark:text-slate-500 mt-0.5 mb-0.5">
        <div>월</div>
        <div>화</div>
        <div>수</div>
        <div>목</div>
        <div>금</div>
        <div>토</div>
        <div>일</div>
      </div>

      <!-- 달력 날짜 그리드 -->
      <div class="grid grid-cols-7 gap-y-0.5">
        <div v-for="day in calendarDays" :key="day.dateStr">
          <div class="relative w-full h-5.5 flex items-center justify-center">
            <!-- 범위 배경 트랙 (Monday.com 스타일 날짜 범위 띠) -->
            <div
              v-if="hasConnectingTrack(day.dateStr)"
              class="absolute inset-y-0.5 bg-blue-50 dark:bg-blue-950/40 -z-0"
              :class="{
                'left-1/2 w-1/2': isSelectedStart(day.dateStr) && (endDate || hoverDate),
                'right-1/2 w-1/2': isSelectedEnd(day.dateStr),
                'w-full': isBetweenRange(day.dateStr),
                'rounded-l-full': isSelectedStart(day.dateStr) && (endDate || hoverDate),
                'rounded-r-full': isSelectedEnd(day.dateStr)
              }"
            ></div>

            <!-- 날짜 원형 버튼 -->
            <button
              @click="handleDateClick(day.dateStr)"
              @mouseenter="handleDateMouseEnter(day.dateStr)"
              @mouseleave="handleDateMouseLeave"
              class="relative w-5 h-5 rounded-full flex items-center justify-center text-[9.5px] font-semibold transition-all z-10"
              :class="[
                day.isCurrentMonth 
                  ? 'text-slate-700 dark:text-slate-200' 
                  : 'text-slate-400/40 dark:text-slate-600/40',
                isSelectedStart(day.dateStr) || isSelectedEnd(day.dateStr)
                  ? 'bg-[#0070e0] text-white font-bold shadow-md shadow-blue-500/25 hover:bg-[#005cb8]'
                  : 'hover:bg-slate-100 dark:hover:bg-slate-800/80',
                day.isToday && !isSelectedStart(day.dateStr) && !isSelectedEnd(day.dateStr)
                  ? 'border-b border-[#0070e0] text-[#0070e0] dark:text-blue-400 font-bold rounded-none'
                  : ''
              ]"
            >
              {{ day.dayNum }}
            </button>
          </div>
        </div>
      </div>

      <!-- 하단 버튼 영역 -->
      <div class="flex items-center justify-between pt-1.5 border-t border-slate-100 dark:border-slate-800 mt-0.5">
        <button
          @click.stop="clearValue"
          class="text-[9.5px] text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300 font-bold px-1.5 py-0.5 rounded hover:bg-red-50 dark:hover:bg-red-950/20 transition-colors"
        >
          초기화
        </button>
        
        <div class="flex gap-0.8">
          <button
            @click.stop="setDefaultRange"
            class="text-[9.5px] text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 font-bold px-1.5 py-0.5 rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            7일
          </button>
          <button
            @click.stop="save"
            class="text-[9.5px] text-white font-bold px-2 py-0.8 rounded bg-[#0070e0] hover:bg-[#005cb8] shadow-sm hover:shadow transition-colors"
          >
            저장
          </button>
          <button
            @click.stop="isOpen = false"
            class="text-[9.5px] text-slate-400 hover:text-slate-600 dark:text-slate-500 dark:hover:text-slate-300 font-bold px-1 py-0.8 rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X class="w-3 h-3" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

