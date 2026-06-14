<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type Employee } from '@/stores/erp/useHrStore'
import { Loader2, CalendarCheck, Check, X, Plus, ChevronDown, User, ShieldAlert } from 'lucide-vue-next'

const router = useRouter()
const hrStore = useHrStore()

const filterStatus = ref('')
const showCreateModal = ref(false)
const activeTab = ref<'my-leaves' | 'team-approvals'>('my-leaves')
const leaveTypes = ref<{ code: string; label: string }[]>([])
const leaveStatuses = ref<{ code: string; label: string }[]>([])

const formData = ref({
  employeeId: '',
  leaveType: 'annual',
  startDate: '',
  endDate: '',
  totalDays: 1,
  reason: '',
  createdBy: '',
})

const employeeList = computed(() => hrStore.employees)
const leaves = computed(() => hrStore.leaveRequests)
const pendingLeaves = computed(() => hrStore.pendingLeaves)

// 로그인 사용자가 부서장인지 여부 판별 (부서 목록 중 manager_id와 매칭)
const isManager = computed(() => {
  const currentEmp = hrStore.currentEmployee
  if (!currentEmp) return false
  return hrStore.departments.some(dept => dept.managerId === currentEmp.id)
})

onMounted(async () => {
  const userId = localStorage.getItem('userId') || ''
  if (userId) {
    await hrStore.fetchEmployeeByUserId(userId)
  }
  await hrStore.fetchDepartments()
  await hrStore.fetchEmployees({ size: 1000 })
  
  // DB에서 LEAVE_TYPE 공통 코드 리스트 로드
  try {
    const codeData = await hrStore.fetchCodes('LEAVE_TYPE')
    if (codeData && Array.isArray(codeData)) {
      leaveTypes.value = codeData.map(c => ({ code: c.code, label: c.label }))
    }
  } catch (e) {
    console.error('공통 코드 LEAVE_TYPE 로드 실패, 백업 로컬 데이터 사용:', e)
  }

  if (leaveTypes.value.length === 0) {
    leaveTypes.value = [
      { code: 'annual', label: '연차' },
      { code: 'half_annual', label: '반차' },
      { code: 'morning_half', label: '오전반차' },
      { code: 'afternoon_half', label: '오후반차' },
      { code: 'sick', label: '병가' },
      { code: 'personal', label: '개인 휴가' },
      { code: 'maternity', label: '출산 휴가' },
      { code: 'etc', label: '기타' }
    ]
  }

  // DB에서 LEAVE_STATUS 공통 코드 리스트 로드
  try {
    const statusData = await hrStore.fetchCodes('LEAVE_STATUS')
    if (statusData && Array.isArray(statusData)) {
      leaveStatuses.value = statusData.map(c => ({ code: c.code, label: c.label }))
    }
  } catch (e) {
    console.error('공통 코드 LEAVE_STATUS 로드 실패, 백업 로컬 데이터 사용:', e)
  }

  if (leaveStatuses.value.length === 0) {
    leaveStatuses.value = [
      { code: 'pending', label: '대기' },
      { code: 'approved', label: '승인' },
      { code: 'rejected', label: '반려' },
      { code: 'cancelled', label: '취소' }
    ]
  }

  // 만약 부서장이면 자동으로 부서 승인 대기 탭을 기본 뷰로 보여주는 것도 편리함
  if (isManager.value) {
    activeTab.value = 'team-approvals'
  }
  
  await loadLeaves()
})

async function loadLeaves() {
  const currentEmp = hrStore.currentEmployee
  if (!currentEmp) {
    // 예외 방지: 아직 프로필 조회가 완료되지 않은 경우 전체 직원 기준으로 로드하지 않고 대기
    return
  }
  
  if (activeTab.value === 'team-approvals') {
    await hrStore.fetchLeaves({ managerId: currentEmp.id, status: filterStatus.value || undefined })
  } else {
    await hrStore.fetchLeaves({ employeeId: currentEmp.id, status: filterStatus.value || undefined })
  }
}

async function changeTab(tab: 'my-leaves' | 'team-approvals') {
  activeTab.value = tab
  filterStatus.value = ''
  await loadLeaves()
}

function onFilterChange() {
  loadLeaves()
}

function openCreateModal() {
  formData.value = {
    employeeId: hrStore.currentEmployee?.id || '',
    leaveType: 'annual',
    startDate: '',
    endDate: '',
    totalDays: 1,
    reason: '',
    createdBy: localStorage.getItem('userId') || '',
  }
  showCreateModal.value = true
}

const isHalfDayType = (type: string) => {
  return ['half_annual', 'morning_half', 'afternoon_half'].includes(type)
}

function recalcDays() {
  const type = formData.value.leaveType
  
  if (isHalfDayType(type)) {
    if (formData.value.startDate) {
      formData.value.endDate = formData.value.startDate
    }
    formData.value.totalDays = 0.5
  } else {
    if (formData.value.startDate && formData.value.endDate) {
      const start = new Date(formData.value.startDate)
      const end = new Date(formData.value.endDate)
      const diff = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1
      formData.value.totalDays = diff > 0 ? diff : 1
    }
  }
}

async function submitLeave() {
  await hrStore.createLeave(formData.value as any)
  showCreateModal.value = false
  await loadLeaves()
}

async function handleApprove(id: string) {
  if (!window.confirm('이 휴가 신청을 승인하시겠습니까?')) return
  const currentEmp = hrStore.currentEmployee
  if (!currentEmp) return
  await hrStore.approveLeave(id, currentEmp.id)
  await loadLeaves()
}

async function handleReject(id: string) {
  const reason = window.prompt('반려 사유를 입력하세요:')
  if (reason === null) return
  if (!reason.trim()) {
    window.alert('반려 사유를 입력해야 합니다.')
    return
  }
  const currentEmp = hrStore.currentEmployee
  if (!currentEmp) return
  await hrStore.rejectLeave(id, currentEmp.id, reason)
  await loadLeaves()
}

async function handleCancel(id: string) {
  if (!window.confirm('이 휴가 신청을 취소하시겠습니까?')) return
  await hrStore.cancelLeave(id)
  await loadLeaves()
}

function leaveTypeLabel(type: string): string {
  const match = leaveTypes.value.find(t => t.code === type)
  return match ? match.label : type
}

function statusBadge(status: string): string {
  switch (status) {
    case 'pending': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
    case 'approved': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
    case 'rejected': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
    case 'cancelled': return 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400'
    default: return 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
  }
}

function getStatusText(status: string): string {
  const match = leaveStatuses.value.find(s => s.code === status)
  return match ? match.label : status
}
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <CalendarCheck class="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
          휴가 신청 및 승인
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">부서장 승인 및 연차 신청을 관리하는 HR 시스템</p>
      </div>
      <button
        @click="openCreateModal"
        class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors cursor-pointer border-none shadow-md hover:shadow-indigo-500/20"
      >
        <Plus class="w-4 h-4" /> 새 휴가 신청
      </button>
    </div>

    <!-- 탭 인터페이스 -->
    <div class="flex border-b border-slate-200 dark:border-slate-800 mb-6 gap-6">
      <button
        @click="changeTab('my-leaves')"
        class="pb-3 text-sm font-semibold border-b-2 transition-all cursor-pointer bg-transparent border-transparent"
        :class="activeTab === 'my-leaves' ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 font-bold' : 'text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'"
      >
        내 휴가 신청 내역
      </button>
      <button
        v-if="isManager"
        @click="changeTab('team-approvals')"
        class="pb-3 text-sm font-semibold border-b-2 transition-all cursor-pointer bg-transparent border-transparent flex items-center gap-1.5"
        :class="activeTab === 'team-approvals' ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 font-bold' : 'text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'"
      >
        <ShieldAlert class="w-4 h-4" />
        부서 휴가 승인 대기
        <span v-if="pendingLeaves.length > 0" class="inline-flex items-center justify-center px-1.5 py-0.5 text-xxs font-bold leading-none text-white bg-amber-500 rounded-full">
          {{ pendingLeaves.length }}
        </span>
      </button>
    </div>

    <!-- Pending summary (부서장용 알림) -->
    <div v-if="activeTab === 'team-approvals' && pendingLeaves.length > 0" class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg p-4 mb-4 flex items-center gap-3 animate-pulse">
      <ShieldAlert class="w-5 h-5 text-amber-600 dark:text-amber-400" />
      <span class="text-sm text-amber-800 dark:text-amber-300 font-medium">
        결재 대기 중인 부서원의 휴가 신청이 <strong class="text-amber-600 dark:text-amber-400">{{ pendingLeaves.length }}건</strong> 있습니다. 빠른 검토를 바랍니다.
      </span>
    </div>

    <!-- Filters -->
    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4 mb-4 shadow-sm">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span class="text-sm text-slate-500 dark:text-slate-400">
          {{ activeTab === 'team-approvals' ? '부서 팀원들의 신청 목록' : '로그인한 사원의 휴가 목록' }}
        </span>
        <div class="w-36">
          <select
            v-model="filterStatus"
            @change="onFilterChange"
            class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
          >
            <option value="">전체 상태</option>
            <option v-for="status in leaveStatuses" :key="status.code" :value="status.code">{{ status.label }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Leave Table -->
    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
      <div v-if="hrStore.loading" class="flex items-center justify-center py-16">
        <Loader2 class="w-8 h-8 text-indigo-500 animate-spin" />
      </div>
      <div v-else-if="leaves.length === 0" class="flex flex-col items-center justify-center py-16 text-slate-400 dark:text-slate-500">
        <CalendarCheck class="w-12 h-12 mb-3 text-slate-300 dark:text-slate-700" />
        <p class="text-sm font-medium">조회된 휴가 신청이 없습니다.</p>
        <p class="text-xs mt-1">새로운 연차 및 반차를 신청해 주세요.</p>
      </div>
      <table v-else class="w-full text-sm">
        <thead class="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-200 dark:border-slate-700">
          <tr>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">신청인</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">유형</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">기간</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">일수</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">상태</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">사유</th>
            <th class="text-right px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          <tr v-for="leave in leaves" :key="leave.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors">
            <td class="px-4 py-3 font-medium text-slate-900 dark:text-white">
              <span class="flex items-center gap-1.5">
                <User class="w-4 h-4 text-slate-400" />
                {{ leave.employeeName || leave.employeeNo }}
              </span>
            </td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300 font-medium">{{ leaveTypeLabel(leave.leaveType) }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ leave.startDate }} ~ {{ leave.endDate }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300 font-semibold">{{ leave.totalDays }}일</td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold" :class="statusBadge(leave.status)">
                {{ getStatusText(leave.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-slate-500 dark:text-slate-400 max-w-[240px]">
              <div class="truncate font-medium text-slate-700 dark:text-slate-300" :title="leave.reason">{{ leave.reason }}</div>
              <div v-if="leave.status === 'rejected' && leave.rejectReason" class="text-xs text-rose-600 dark:text-rose-400 mt-1 font-semibold flex items-center gap-1">
                <span class="inline-block px-1.5 py-0.5 bg-rose-100 dark:bg-rose-950/40 rounded text-xxs font-bold text-rose-700 dark:text-rose-300">반려 사유</span>
                <span class="truncate" :title="leave.rejectReason">{{ leave.rejectReason }}</span>
              </div>
            </td>
            <td class="px-4 py-3 text-right">
              <!-- 부서장 승인 대기 탭일 때의 관리 액션 (승인 / 반려) -->
              <div v-if="activeTab === 'team-approvals' && leave.status === 'pending'" class="flex items-center justify-end gap-1">
                <button
                  @click="handleApprove(leave.id)"
                  class="p-1.5 rounded hover:bg-green-100 dark:hover:bg-green-900/30 text-green-600 dark:text-green-400 cursor-pointer border-none bg-transparent transition-all"
                  title="승인"
                >
                  <Check class="w-5 h-5 font-bold" />
                </button>
                <button
                  @click="handleReject(leave.id)"
                  class="p-1.5 rounded hover:bg-red-100 dark:hover:bg-red-900/30 text-red-600 dark:text-red-400 cursor-pointer border-none bg-transparent transition-all"
                  title="반려"
                >
                  <X class="w-5 h-5 font-bold" />
                </button>
              </div>
              
              <!-- 내 휴가 탭일 때의 관리 액션 (취소 가능) -->
              <div v-else-if="activeTab === 'my-leaves' && (leave.status === 'pending' || leave.status === 'approved')" class="flex items-center justify-end">
                <button
                  @click="handleCancel(leave.id)"
                  class="flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded hover:bg-rose-100 hover:text-rose-600 dark:hover:bg-rose-900/30 dark:hover:text-rose-400 text-slate-500 cursor-pointer border border-slate-200 dark:border-slate-700 bg-transparent transition-all"
                  title="취소"
                >
                  신청 취소
                </button>
              </div>
              
              <!-- 상태 정보 표시 -->
              <span v-else-if="leave.status === 'cancelled'" class="text-xs text-slate-400">취소됨</span>
              <span v-else-if="leave.approverName" class="text-xs text-slate-500 dark:text-slate-400 font-medium">{{ leave.approverName }} 승인</span>
              <span v-else class="text-xs text-slate-400">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create Modal -->
    <Teleport to="body">
      <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="showCreateModal = false">
        <div class="bg-white dark:bg-slate-900 rounded-xl shadow-2xl border border-slate-200 dark:border-slate-800 w-full max-w-lg mx-4">
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800">
            <h2 class="text-lg font-bold text-slate-900 dark:text-white">새 휴가 신청</h2>
            <button @click="showCreateModal = false" class="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none text-slate-500">
              <X class="w-5 h-5" />
            </button>
          </div>
          <div class="p-6 space-y-4">
            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">신청인</label>
              <input 
                type="text" 
                :value="hrStore.currentEmployee ? `${hrStore.currentEmployee.employeeNo} - ${hrStore.currentEmployee.employeeName || hrStore.currentEmployee.employeeNo}` : ''" 
                disabled 
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-500" 
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">휴가 유형</label>
              <select v-model="formData.leaveType" @change="recalcDays" class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white">
                <option v-for="type in leaveTypes" :key="type.code" :value="type.code">{{ type.label }}</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">시작일</label>
                <input v-model="formData.startDate" type="date" @change="recalcDays" class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white" />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">종료일</label>
                <input 
                  v-model="formData.endDate" 
                  type="date" 
                  @change="recalcDays" 
                  :disabled="isHalfDayType(formData.leaveType)" 
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white" 
                  :class="isHalfDayType(formData.leaveType) ? 'bg-slate-100 dark:bg-slate-800/60 text-slate-400 dark:text-slate-500 cursor-not-allowed' : ''"
                />
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">사용 일수</label>
              <div class="text-sm font-bold text-indigo-600 dark:text-indigo-400">총 {{ formData.totalDays }}일</div>
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">사유</label>
              <textarea v-model="formData.reason" rows="3" placeholder="휴가 사유를 작성해 주세요." class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white resize-none"></textarea>
            </div>
          </div>
          <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-200 dark:border-slate-800">
            <button @click="showCreateModal = false" class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg cursor-pointer border-none bg-transparent">취소</button>
            <button @click="submitLeave" :disabled="!formData.reason || !formData.startDate || !formData.endDate" class="px-4 py-2 text-sm font-medium bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white rounded-lg cursor-pointer border-none">신청하기</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
