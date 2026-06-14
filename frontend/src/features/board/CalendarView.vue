<script setup lang="ts">
import { ref, computed } from 'vue'
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon } from 'lucide-vue-next'

const props = defineProps<{
  board: any
}>()

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth()) // 0-indexed

const toDate = (d: string) => {
  if (!d) return null
  const parts = d.split('-').map(Number)
  if (parts.length === 3 && !parts.some(isNaN))
    return new Date(parts[0], parts[1] - 1, parts[2])
  const dt = new Date(d)
  return isNaN(dt.getTime()) ? null : dt
}
const formatYMD = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
const todayStr = formatYMD(new Date())

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

const dayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

// Collect all date values from items
const itemsByDate = computed(() => {
  const map: Record<string, { item: any; group: any; color: string }[]> = {}
  const groups = props.board.groups || []
  const groupColors = ['#579BFC', '#A25DDC', '#00C875', '#FDAB3D', '#E2445C', '#0086C0']

  // Find date columns
  const dateColumns = props.board.columns?.filter((c: any) =>
    c.columnType === 'date' || c.type === 'date'
  ) || []

  // Find timeline column
  const timelineCol = props.board.columns?.find((c: any) =>
    c.columnType === 'timeline' || c.type === 'timeline'
  )

  groups.forEach((group: any, gi: number) => {
    ;(group.items || []).forEach((item: any) => {
      const statusColor = getItemStatusColor(item)
      const color = statusColor || group.color || groupColors[gi % groupColors.length]
      const dates: string[] = []

      // Collect dates from date columns
      dateColumns.forEach((col: any) => {
        const val = item.values?.[col.id] || item.values?.['date'] || ''
        if (val) dates.push(val)
      })

      // Collect dates from timeline column
      if (timelineCol) {
        const raw = item.values?.[timelineCol.id] || item.values?.['timeline'] || ''
        if (raw) {
          try {
            const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
            if (parsed.start) {
              const s = toDate(parsed.start)
              const e = toDate(parsed.end || parsed.start)
              if (s && e) {
                // Add all days in the range
                const cursor = new Date(s)
                while (cursor <= e) {
                  dates.push(formatYMD(cursor))
                  cursor.setDate(cursor.getDate() + 1)
                }
              }
            }
          } catch { /* ignore */ }
        }
      }

      // Deduplicate and add to map
      const uniqueDates = [...new Set(dates)]
      uniqueDates.forEach(dateStr => {
        if (!map[dateStr]) map[dateStr] = []
        // Avoid duplicates
        if (!map[dateStr].some(d => d.item.id === item.id)) {
          map[dateStr].push({ item, group, color })
        }
      })
    })
  })

  return map
})

// Generate calendar grid
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDow = firstDay.getDay() // 0=Sun
  const daysInMonth = lastDay.getDate()

  const cells: { date: string; dayNum: number; isCurrentMonth: boolean; isToday: boolean }[] = []

  // Previous month padding
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = startDow - 1; i >= 0; i--) {
    const d = prevMonthLastDay - i
    const m = month === 0 ? 11 : month - 1
    const y = month === 0 ? year - 1 : year
    const dateStr = `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ date: dateStr, dayNum: d, isCurrentMonth: false, isToday: dateStr === todayStr })
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ date: dateStr, dayNum: d, isCurrentMonth: true, isToday: dateStr === todayStr })
  }

  // Next month padding to fill 6 rows (42 cells)
  const remaining = 42 - cells.length
  for (let d = 1; d <= remaining; d++) {
    const m = month === 11 ? 0 : month + 1
    const y = month === 11 ? year + 1 : year
    const dateStr = `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ date: dateStr, dayNum: d, isCurrentMonth: false, isToday: dateStr === todayStr })
  }

  return cells
})

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

const goToday = () => {
  const today = new Date()
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth()
}

const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

const itemsForDate = (dateStr: string) => itemsByDate.value[dateStr] || []
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- Toolbar -->
    <div class="flex items-center justify-between px-4 py-2 border-b border-slate-200 dark:border-slate-700">
      <div class="flex items-center gap-2">
        <button @click="prevMonth" class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none bg-transparent text-slate-500">
          <ChevronLeft class="w-4 h-4" />
        </button>
        <button @click="goToday" class="px-3 py-1.5 text-xs font-bold rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none bg-transparent text-slate-600 dark:text-slate-300">
          Today
        </button>
        <button @click="nextMonth" class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none bg-transparent text-slate-500">
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>
      <div class="text-sm font-bold text-slate-700 dark:text-slate-200">
        <CalendarIcon class="w-4 h-4 inline mr-1.5" />
        {{ monthNames[currentMonth] }} {{ currentYear }}
      </div>
    </div>

    <!-- Calendar Grid -->
    <div class="flex-1 overflow-auto p-3">
      <!-- Day-of-week header -->
      <div class="grid grid-cols-7 mb-1">
        <div v-for="label in dayLabels" :key="label"
          class="text-center text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider py-1"
        >
          {{ label }}
        </div>
      </div>

      <!-- Calendar cells -->
      <div class="grid grid-cols-7 gap-px bg-slate-200 dark:bg-slate-800 rounded-lg overflow-hidden border border-slate-200 dark:border-slate-800">
        <div v-for="(cell, idx) in calendarDays" :key="idx"
          class="bg-white dark:bg-slate-900 min-h-[90px] p-1.5 flex flex-col transition-colors"
          :class="{
            'bg-slate-50/50 dark:bg-slate-900/50': !cell.isCurrentMonth,
            'ring-2 ring-indigo-500/30 dark:ring-indigo-400/30 ring-inset': cell.isToday
          }"
        >
          <!-- Day number -->
          <div class="text-[10px] font-bold mb-0.5"
            :class="{
              'text-slate-400 dark:text-slate-500': !cell.isCurrentMonth,
              'text-slate-700 dark:text-slate-300': cell.isCurrentMonth && !cell.isToday,
              'text-indigo-600 dark:text-indigo-400': cell.isToday
            }"
          >
            {{ cell.dayNum }}
          </div>

          <!-- Items on this date -->
          <div class="flex-1 flex flex-col gap-0.5 overflow-hidden">
            <div v-for="entry in itemsForDate(cell.date).slice(0, 4)" :key="entry.item.id"
              class="text-[9px] leading-tight px-1 py-0.5 rounded font-semibold text-white truncate cursor-pointer hover:opacity-80 transition-opacity"
              :style="{ backgroundColor: entry.color }"
              :title="entry.item.name"
            >
              {{ entry.item.name }}
            </div>
            <div v-if="itemsForDate(cell.date).length > 4"
              class="text-[9px] text-slate-500 dark:text-slate-400 font-semibold px-1"
            >
              +{{ itemsForDate(cell.date).length - 4 }} more
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="Object.keys(itemsByDate).length === 0" class="text-center py-12">
        <CalendarIcon class="w-10 h-10 mx-auto text-slate-400 mb-3" />
        <p class="text-sm text-slate-500 dark:text-slate-400 font-semibold">No dated items yet</p>
        <p class="text-xs text-slate-400 dark:text-slate-500 mt-1">Add a Date or Timeline column to your board and assign dates to items.</p>
      </div>
    </div>
  </div>
</template>
