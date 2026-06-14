<script setup lang="ts">
import { ref, computed } from 'vue'
import { Calendar, ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps<{
  board: any
}>()

// Date helpers
const toDate = (d: string) => {
  if (!d) return null
  // Parse YYYY-MM-DD as local date to avoid UTC offset with the grid (which uses local dates)
  const parts = d.split('-').map(Number)
  if (parts.length === 3 && !parts.some(isNaN))
    return new Date(parts[0], parts[1] - 1, parts[2])
  const dt = new Date(d)
  return isNaN(dt.getTime()) ? null : dt
}
const formatYMD = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
const DAY_MS = 86400000

// State for horizontal scroll / week offset
const weekOffset = ref(0)
const weeksToShow = ref(8)

// Find timeline column (stores JSON {start, end}) or first date column
const timelineCol = computed(() =>
  props.board.columns?.find((c: any) => c.columnType === 'timeline' || c.type === 'timeline')
)
const dateCol = computed(() =>
  props.board.columns?.find((c: any) => c.columnType === 'date' || c.type === 'date')
)

// Status column for item colors
const statusCol = computed(() =>
  props.board.columns?.find((c: any) => c.columnType === 'status' || c.type === 'status')
)
const getStatusOptions = computed(() => {
  const col = statusCol.value
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
  return [
    { label: '시작 전', color: '#C4C4C4' },
    { label: '진행 중', color: '#FDAB3D' },
    { label: '완료', color: '#00C875' },
    { label: '막힘', color: '#E2445C' }
  ]
})
const getItemStatusColor = (item: any): string | null => {
  const col = statusCol.value
  if (!col) return null
  const statusName = item.values?.[col.id] || item.values?.['status'] || null
  if (!statusName) return null
  const matched = getStatusOptions.value.find((o: any) => o.label === statusName)
  return matched ? matched.color : null
}

// Determine overall date range from items OR default to current month
const allDateRanges = computed(() => {
  const ranges: { start: Date; end: Date; item: any; group: any; color: string }[] = []
  const groups = props.board.groups || []
  const groupColors = ['#579BFC', '#A25DDC', '#00C875', '#FDAB3D', '#E2445C', '#0086C0', '#FF6B6B', '#45B7D1']

  groups.forEach((group: any, gi: number) => {
    ;(group.items || []).forEach((item: any) => {
      const statusColor = getItemStatusColor(item)
      const color = statusColor || group.color || groupColors[gi % groupColors.length]
      let startStr = ''
      let endStr = ''

      if (timelineCol.value) {
        const raw = item.values?.[timelineCol.value.id] || item.values?.['timeline'] || ''
        if (raw) {
          try {
            const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
            startStr = parsed.start || ''
            endStr = parsed.end || ''
          } catch { /* ignore */ }
        }
      }

      if (!startStr && dateCol.value) {
        startStr = item.values?.[dateCol.value.id] || item.values?.['date'] || ''
        endStr = startStr
      }

      const start = toDate(startStr)
      const end = toDate(endStr || startStr)
      if (start) {
        ranges.push({ start, end: end || start, item, group, color })
      }
    })
  })
  return ranges
})

// Compute global min/max dates to set the axis
const globalStart = computed(() => {
  if (allDateRanges.value.length === 0) {
    const d = new Date(); d.setDate(d.getDate() - 7)
    return d
  }
  const dates = allDateRanges.value.map(r => r.start.getTime())
  return new Date(Math.min(...dates))
})
const globalEnd = computed(() => {
  if (allDateRanges.value.length === 0) {
    const d = new Date(); d.setDate(d.getDate() + 7 * 8)
    return d
  }
  const dates = allDateRanges.value.map(r => r.end.getTime())
  return new Date(Math.max(...dates))
})

// Snap to week boundaries
const snapToMonday = (d: Date) => {
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  const m = new Date(d); m.setDate(d.getDate() + diff)
  m.setHours(0,0,0,0)
  return m
}

const baseMonday = computed(() => snapToMonday(globalStart.value))

const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const monthLabels = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

// Generate week segments for the visible window
const visibleWeeks = computed(() => {
  const weeks: { monday: Date; days: Date[] }[] = []
  const startMonday = new Date(baseMonday.value)
  startMonday.setDate(startMonday.getDate() + weekOffset.value * 7)

  for (let w = 0; w < weeksToShow.value; w++) {
    const monday = new Date(startMonday)
    monday.setDate(monday.getDate() + w * 7)
    const days: Date[] = []
    for (let d = 0; d < 7; d++) {
      const day = new Date(monday); day.setDate(day.getDate() + d)
      days.push(day)
    }
    weeks.push({ monday, days })
  }
  return weeks
})

const totalDays = computed(() => weeksToShow.value * 7)

const colWidth = 120
const rowHeight = 40
const groupHeaderH = 32
const leftGutter = 200

// Item bar positioning
const barPositions = computed(() => {
  const firstDay = visibleWeeks.value[0]?.monday.getTime() || 0
  return allDateRanges.value.map(r => {
    const startDay = r.start.getTime()
    const endDay = r.end.getTime()
    const x = Math.max(0, (startDay - firstDay) / DAY_MS) * colWidth
    const w = Math.max(colWidth * 0.5, ((endDay - startDay) / DAY_MS + 1) * colWidth)
    return { ...r, x, w }
  })
})

// Group rows
const groupRows = computed(() => {
  const groups = props.board.groups || []
  return groups.map((g: any) => ({
    ...g,
    items: (g.items || []).filter((i: any) =>
      allDateRanges.value.some(r => r.item.id === i.id)
    )
  }))
})

const navigatePrev = () => { weekOffset.value -= weeksToShow.value }
const navigateNext = () => { weekOffset.value += weeksToShow.value }
const navigateToday = () => { weekOffset.value = 0 }

// Today indicator
const todayStr = computed(() => formatYMD(new Date()))
// Local midnight today for positioning (avoids UTC offset mismatch)
const todayDate = computed(() => {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- Toolbar -->
    <div class="flex items-center justify-between px-4 py-2 border-b border-slate-200 dark:border-slate-700">
      <div class="flex items-center gap-2">
        <button @click="navigatePrev" class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none bg-transparent text-slate-500">
          <ChevronLeft class="w-4 h-4" />
        </button>
        <button @click="navigateToday" class="px-3 py-1.5 text-xs font-bold rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none bg-transparent text-slate-600 dark:text-slate-300">
          Today
        </button>
        <button @click="navigateNext" class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none bg-transparent text-slate-500">
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>
      <div class="text-xs font-semibold text-slate-500 dark:text-slate-400">
        <Calendar class="w-3.5 h-3.5 inline mr-1" />
        <span v-if="visibleWeeks.length">
          {{ monthLabels[visibleWeeks[0].monday.getMonth()] }} {{ visibleWeeks[0].monday.getFullYear() }}
          – {{ monthLabels[visibleWeeks[visibleWeeks.length-1].days[6].getMonth()] }} {{ visibleWeeks[visibleWeeks.length-1].days[6].getFullYear() }}
        </span>
      </div>
    </div>

    <!-- Gantt Chart Area -->
    <div class="flex-1 overflow-auto">
      <div class="inline-flex min-w-full">
        <!-- Left gutter (group + item names) -->
        <div class="sticky left-0 z-20 bg-white dark:bg-slate-900 shrink-0">
          <!-- Spacer for the time header row -->
          <div class="h-14 border-b border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900" style="width: 200px"></div>
          <!-- Group rows -->
          <div v-for="group in groupRows" :key="group.id">
            <div class="h-7 flex items-center px-3 text-xs font-bold text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50 border-b border-slate-100 dark:border-slate-800" style="width: 200px; min-height: 28px">
              {{ group.title }}
            </div>
            <div v-for="item in group.items" :key="item.id">
              <div class="flex items-center px-3 text-xs text-slate-700 dark:text-slate-300 border-b border-slate-100 dark:border-slate-800 truncate" :style="{ height: rowHeight + 'px', width: leftGutter + 'px' }">
                {{ item.name }}
              </div>
            </div>
          </div>
          <div v-if="groupRows.length === 0" class="flex items-center px-3 text-xs text-slate-400 py-4">
            No items with dates
          </div>
        </div>

        <!-- Timeline grid (scrollable horizontally) -->
        <div class="overflow-x-auto">
          <div :style="{ width: totalDays * colWidth + 'px' }">
            <!-- Day headers row -->
            <div class="flex border-b border-slate-200 dark:border-slate-700 sticky top-0 z-10 bg-white dark:bg-slate-900" style="height: 28px">
              <div v-for="(week, wi) in visibleWeeks" :key="wi" class="flex">
                <div v-for="(day, di) in week.days" :key="di"
                  class="flex items-center justify-center text-[10px] font-semibold border-r border-slate-100 dark:border-slate-800"
                  :class="formatYMD(day) === todayStr ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-500 dark:text-slate-400'"
                  :style="{ width: colWidth + 'px', height: '28px' }"
                >
                  {{ dayLabels[di] }} {{ day.getDate() }}
                </div>
              </div>
            </div>

            <!-- Month separator row -->
            <div class="flex border-b border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-800/30" style="height: 14px">
              <template v-for="(week, wi) in visibleWeeks" :key="'m'+wi">
                <div v-for="(day, di) in week.days" :key="'md'+di"
                  class="border-r border-slate-100 dark:border-slate-800 text-[9px] font-bold text-slate-400 dark:text-slate-500 flex items-center justify-start pl-0.5"
                  :style="{ width: colWidth + 'px', height: '14px' }"
                >
                  <span v-if="di === 0">{{ monthLabels[day.getMonth()] }} {{ day.getFullYear() }}</span>
                  <span v-else-if="day.getDate() === 1 && di > 0">{{ monthLabels[day.getMonth()] }}</span>
                </div>
              </template>
            </div>

            <!-- Bars area -->
            <div class="relative">
              <!-- Grid lines -->
              <div v-for="(week, wi) in visibleWeeks" :key="'g'+wi" class="absolute top-0" :style="{ left: wi * 7 * colWidth + 'px', width: 7 * colWidth + 'px', height: '100%' }">
                <div v-for="di in 6" :key="di" class="absolute top-0 h-full border-r border-slate-100 dark:border-slate-800/50"
                  :style="{ left: di * colWidth + 'px' }">
                </div>
              </div>

              <!-- Today line -->
              <div class="absolute top-0 w-0.5 bg-indigo-500/60 z-10" :style="{ left: ((todayDate.getTime() - visibleWeeks[0]?.monday.getTime()) / DAY_MS) * colWidth + 'px', height: '100%' }"></div>

              <!-- Group/item rows for bar placement -->
              <div v-for="group in groupRows" :key="'b'+group.id">
                <div :style="{ height: groupHeaderH + 'px' }"></div>
                <div v-for="item in group.items" :key="'bi'+item.id" class="relative border-b border-slate-100 dark:border-slate-800" :style="{ height: rowHeight + 'px' }">
                  <template v-for="bar in barPositions" :key="bar.item.id">
                    <div v-if="bar.item.id === item.id"
                      class="absolute top-1/2 -translate-y-1/2 rounded-full px-2 py-1 text-[10px] font-semibold text-white truncate shadow-sm cursor-pointer hover:opacity-90 transition-opacity z-[1]"
                      :style="{
                        left: bar.x + 'px',
                        width: Math.max(bar.w, 30) + 'px',
                        backgroundColor: bar.color
                      }"
                      :title="item.name"
                    >
                      {{ item.name }}
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
