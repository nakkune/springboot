<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type Employee } from '@/stores/erp/useHrStore'
import { Loader2, Clock, Pencil, Plus, X, Search } from 'lucide-vue-next'
import DatePicker from '@/components/ui/DatePicker.vue'

const router = useRouter()
const hrStore = useHrStore()

const selectedEmployeeId = ref('')
const todayStr = new Date().toISOString().slice(0, 10)
const [year, month] = todayStr.split('-')
const startDate = ref(`${year}-${month}-01`)
const endDate = ref(todayStr)
const editingRecord = ref<string | null>(null)
const editMemo = ref('')
const selectedStatus = ref('')

// 수기 등록 팝업 모달 상태
const showFormModal = ref(false)
const modalFormData = ref({
  employeeId: '',
  workDate: new Date().toISOString().slice(0, 10),
  checkIn: '',
  checkOut: '',
  status: 'present',
  memo: ''
})
const saving = ref(false)

const employeeList = computed(() => hrStore.employees)
const attendanceList = computed(() => hrStore.attendanceRecords)

const stats = computed(() => {
  const total = attendanceList.value.length
  const present = attendanceList.value.filter(a => a.status === 'present').length
  const late = attendanceList.value.filter(a => a.status === 'late').length
  const absent = attendanceList.value.filter(a => a.status === 'absent').length
  return { total, present, late, absent }
})

onMounted(async () => {
  await hrStore.fetchEmployees({ size: 1000 })
  selectedEmployeeId.value = '' // 기본값: 전체 직원
  await loadAttendance()
})

async function loadAttendance() {
  await hrStore.fetchAttendance(
    selectedEmployeeId.value || undefined,
    startDate.value,
    endDate.value,
    selectedStatus.value || undefined
  )
}

async function onEmployeeChange() {
  await loadAttendance()
}

async function onDateChange() {
  await loadAttendance()
}

async function onStatusChange() {
  await loadAttendance()
}

function openCreateModal() {
  modalFormData.value = {
    employeeId: hrStore.employees.length > 0 ? hrStore.employees[0].id : '',
    workDate: new Date().toISOString().slice(0, 10),
    checkIn: '',
    checkOut: '',
    status: 'present',
    memo: ''
  }
  showFormModal.value = true
}

async function saveAttendance() {
  if (!modalFormData.value.employeeId) {
    alert('직원을 선택해 주세요.')
    return
  }
  if (!modalFormData.value.workDate) {
    alert('근무 일자를 입력해 주세요.')
    return
  }

  saving.value = true
  try {
    const payload = { ...modalFormData.value }

    // HH:mm 형식의 시간 값을 YYYY-MM-DDTHH:mm:ss+09:00 OffsetDateTime 양식으로 변환
    if (payload.checkIn) {
      payload.checkIn = `${payload.workDate}T${payload.checkIn}:00+09:00`
    } else {
      payload.checkIn = null as any
    }

    if (payload.checkOut) {
      payload.checkOut = `${payload.workDate}T${payload.checkOut}:00+09:00`
    } else {
      payload.checkOut = null as any
    }

    await hrStore.createAttendance(payload)
    showFormModal.value = false
    await loadAttendance()
  } catch (e: any) {
    alert(e?.response?.data?.error || '근태 등록 실패')
  } finally {
    saving.value = false
  }
}

function startEdit(recordId: string, memo: string) {
  editingRecord.value = recordId
  editMemo.value = memo
}

async function saveEdit(recordId: string) {
  await hrStore.updateAttendance(recordId, { memo: editMemo.value } as any)
  editingRecord.value = null
  await loadAttendance()
}

function statusColor(status: string): string {
  switch (status) {
    case 'present': return 'text-green-600 dark:text-green-400'
    case 'late': return 'text-amber-600 dark:text-amber-400'
    case 'early_leave': return 'text-orange-600 dark:text-orange-400'
    case 'absent': return 'text-red-600 dark:text-red-400'
    default: return 'text-slate-500'
  }
}

function statusBadge(status: string): string {
  switch (status) {
    case 'present': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
    case 'late': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
    case 'early_leave': return 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400'
    case 'absent': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
    default: return 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
  }
}
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">근태</h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">일일 근태 기록 및 출퇴근 관리</p>
      </div>
      <button
        @click="openCreateModal"
        class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors cursor-pointer border-none"
      >
        <Plus class="w-4 h-4" />
        근태 등록
      </button>
    </div>

    <!-- Controls -->
    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4 mb-4">
      <div class="flex flex-wrap items-center gap-3">
        <div class="w-56">
          <select
            v-model="selectedEmployeeId"
            @change="onEmployeeChange"
            class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
          >
            <option value="">전체 직원</option>
            <option v-for="emp in employeeList" :key="emp.id" :value="emp.id">
              {{ emp.employeeNo }} - {{ emp.employeeName || emp.employeeNo }}
            </option>
          </select>
        </div>
        <div class="w-44">
          <select
            v-model="selectedStatus"
            @change="onStatusChange"
            class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
          >
            <option value="">전체 상태</option>
            <option value="present">출근 (Present)</option>
            <option value="late">지각 (Late)</option>
            <option value="early_leave">조퇴 (Early Leave)</option>
            <option value="absent">결근 (Absent)</option>
          </select>
        </div>
        <div class="w-40">
          <DatePicker
            v-model="startDate"
            @change="onDateChange"
          />
        </div>
        <span class="text-slate-400 dark:text-slate-500 font-medium">~</span>
        <div class="w-40">
          <DatePicker
            v-model="endDate"
            @change="onDateChange"
          />
        </div>
        <button
          @click="loadAttendance"
          class="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white rounded-lg text-sm font-medium transition-colors cursor-pointer border-none shrink-0"
        >
          <Search class="w-4 h-4" />
          검색
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-4 gap-4 mb-4">
      <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4">
        <p class="text-xs text-slate-500 dark:text-slate-400">전체</p>
        <p class="text-2xl font-bold text-slate-900 dark:text-white mt-1">{{ stats.total }}</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-lg border border-green-200 dark:border-green-900 p-4">
        <p class="text-xs text-slate-500 dark:text-slate-400">출근</p>
        <p class="text-2xl font-bold text-green-600 dark:text-green-400 mt-1">{{ stats.present }}</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-lg border border-amber-200 dark:border-amber-900 p-4">
        <p class="text-xs text-slate-500 dark:text-slate-400">지각</p>
        <p class="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">{{ stats.late }}</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-lg border border-red-200 dark:border-red-900 p-4">
        <p class="text-xs text-slate-500 dark:text-slate-400">결근</p>
        <p class="text-2xl font-bold text-red-600 dark:text-red-400 mt-1">{{ stats.absent }}</p>
      </div>
    </div>

    <!-- Attendance Table -->
    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 overflow-hidden">
      <div v-if="hrStore.loading" class="flex items-center justify-center py-16">
        <Loader2 class="w-8 h-8 text-indigo-500 animate-spin" />
      </div>
      <div v-else-if="attendanceList.length === 0" class="flex flex-col items-center justify-center py-16 text-slate-400 dark:text-slate-500">
        <Clock class="w-12 h-12 mb-3" />
        <p class="text-sm font-medium">근태 기록이 없습니다</p>
        <p class="text-xs mt-1">출근 기록을 시작하세요.</p>
      </div>
      <table v-else class="w-full text-sm">
        <thead class="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-200 dark:border-slate-700">
          <tr>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">사번</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">이름</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">날짜</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">출근</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">퇴근</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">근무시간</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">초과근무</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">상태</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">메모</th>
            <th class="text-right px-4 py-3 font-semibold text-slate-600 dark:text-slate-400"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          <tr v-for="rec in attendanceList" :key="rec.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors">
            <td class="px-4 py-3 font-medium text-slate-900 dark:text-white">{{ rec.employeeNo || '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300 font-medium">{{ rec.employeeName || '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ rec.workDate }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ rec.checkIn ? new Date(rec.checkIn).toLocaleTimeString() : '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ rec.checkOut ? new Date(rec.checkOut).toLocaleTimeString() : '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ rec.workHours || '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ rec.overtimeHours || '-' }}</td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium" :class="statusBadge(rec.status)">
                {{ rec.status }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div v-if="editingRecord === rec.id" class="flex items-center gap-1">
                <input v-model="editMemo" class="w-28 px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white" />
                <button @click="saveEdit(rec.id)" class="text-xs text-indigo-600 hover:text-indigo-800 cursor-pointer border-none bg-transparent">저장</button>
              </div>
              <span v-else class="text-slate-500 dark:text-slate-400 text-xs">{{ rec.memo || '-' }}</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button @click="startEdit(rec.id, rec.memo || '')" class="p-1 hover:bg-slate-100 dark:hover:bg-slate-800 rounded cursor-pointer border-none text-slate-400 hover:text-indigo-600">
                <Pencil class="w-3.5 h-3.5" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create Modal -->
    <Teleport to="body">
      <div
        v-if="showFormModal"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
        @click.self="showFormModal = false"
      >
        <div class="bg-white dark:bg-slate-900 rounded-xl shadow-2xl border-2 border-slate-300 dark:border-slate-700 ring-1 ring-slate-200/50 dark:ring-slate-800/50 w-full max-w-md mx-4 max-h-[85vh] overflow-y-auto">
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800">
            <h2 class="text-lg font-bold text-slate-900 dark:text-white">근태 등록</h2>
            <button
              @click="showFormModal = false"
              class="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none text-slate-500"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">대상 직원</label>
              <select
                v-model="modalFormData.employeeId"
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
              >
                <option value="" disabled>직원을 선택하세요</option>
                <option v-for="emp in employeeList" :key="emp.id" :value="emp.id">
                  {{ emp.employeeNo }} - {{ emp.employeeName || emp.employeeNo }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">근무 일자</label>
              <DatePicker
                v-model="modalFormData.workDate"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">출근 시각</label>
                <input
                  v-model="modalFormData.checkIn"
                  type="time"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">퇴근 시각</label>
                <input
                  v-model="modalFormData.checkOut"
                  type="time"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">근태 상태</label>
              <select
                v-model="modalFormData.status"
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
              >
                <option value="present">출근 (Present)</option>
                <option value="late">지각 (Late)</option>
                <option value="early_leave">조퇴 (Early Leave)</option>
                <option value="absent">결근 (Absent)</option>
              </select>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">메모 / 특이사항</label>
              <textarea
                v-model="modalFormData.memo"
                rows="2"
                placeholder="지각 사유 등 특이사항을 적어주세요."
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40 resize-none"
              ></textarea>
            </div>
          </div>

          <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-200 dark:border-slate-800">
            <button
              @click="showFormModal = false"
              class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors cursor-pointer border-none"
            >
              취소
            </button>
            <button
              @click="saveAttendance"
              :disabled="saving"
              class="px-4 py-2 text-sm font-medium bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white rounded-lg transition-colors flex items-center gap-2 cursor-pointer border-none"
            >
              <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
              등록
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
