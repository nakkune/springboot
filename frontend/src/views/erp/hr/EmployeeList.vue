<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type Employee, type Department, type CodeDto } from '@/stores/erp/useHrStore'
import api from '@/services/api'
import {
  Search,
  Plus,
  Loader2,
  ChevronLeft,
  ChevronRight,
  X,
  ChevronDown,
  Filter,
  Briefcase,
} from 'lucide-vue-next'

const router = useRouter()
const hrStore = useHrStore()

const searchQuery = ref('')
const selectedDeptId = ref('')
const selectedStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const showFormModal = ref(false)
const editingEmployee = ref<Employee | null>(null)
const formData = ref<Partial<Employee>>({})
const saving = ref(false)
const deptOptions = ref<(Department & { depth: number })[]>([])
const positionOptions = ref<CodeDto[]>([])
const jobTitleOptions = ref<CodeDto[]>([])
const showFilterPanel = ref(false)

// Autocomplete Dropdown 상태 및 데이터
const usersList = ref<any[]>([])
const searchEmailQuery = ref('')
const showEmailDropdown = ref(false)

async function loadUsers() {
  try {
    const data = await api.get('/users')
    usersList.value = (data as any) || []
  } catch (error) {
    console.error('사용자 목록 로드 실패:', error)
    usersList.value = []
  }
}

const filteredUsers = computed(() => {
  const query = searchEmailQuery.value.toLowerCase().trim()
  if (!query) {
    return usersList.value
  }
  return usersList.value.filter(u => 
    (u.email && u.email.toLowerCase().includes(query)) ||
    (u.fullName && u.fullName.toLowerCase().includes(query))
  )
})

function selectUser(user: any) {
  formData.value.email = user.email
  formData.value.userId = user.id
  formData.value.employeeName = user.fullName
  searchEmailQuery.value = user.email
  showEmailDropdown.value = false
}

function handleEmailBlur() {
  setTimeout(() => {
    showEmailDropdown.value = false
  }, 200)
}

function generateNextEmployeeNo(): string {
  const today = new Date()
  const yy = String(today.getFullYear()).slice(-2)
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  const dateStr = `${yy}${mm}${dd}`
  
  const todayEmpNos = hrStore.employees
    .map(e => e.employeeNo)
    .filter(no => no && no.startsWith(dateStr))
    .sort()
    
  if (todayEmpNos.length > 0) {
    const lastNo = todayEmpNos[todayEmpNos.length - 1]
    const seqStr = lastNo.slice(6)
    const nextSeq = parseInt(seqStr, 10) + 1
    return `${dateStr}${String(nextSeq).padStart(3, '0')}`
  }
  
  return `${dateStr}001`
}

watch(searchEmailQuery, (newVal) => {
  const emailVal = newVal ? newVal.trim() : ''
  formData.value.email = emailVal
  
  const matchedUser = usersList.value.find(
    (u) => u.email && u.email.toLowerCase() === emailVal.toLowerCase()
  )
  if (matchedUser) {
    formData.value.userId = matchedUser.id
    formData.value.employeeName = matchedUser.fullName
  } else {
    formData.value.userId = null
    if (!emailVal) {
      formData.value.employeeName = ''
    }
  }
})

watch(() => formData.value.status, (newStatus) => {
  if (newStatus === 'resigned') {
    if (!formData.value.resignationDate) {
      formData.value.resignationDate = new Date().toISOString().split('T')[0]
    }
    formData.value.leaveStartDate = null
    formData.value.leaveEndDate = null
  } else if (newStatus === 'leave') {
    if (!formData.value.leaveStartDate) {
      formData.value.leaveStartDate = new Date().toISOString().split('T')[0]
    }
    formData.value.resignationDate = null
  } else {
    formData.value.resignationDate = null
    formData.value.leaveStartDate = null
    formData.value.leaveEndDate = null
  }
})

/** 공통코드에서 직위/직책 옵션 로드 */
async function loadCodeOptions() {
  try {
    const [positions, titles] = await Promise.all([
      hrStore.fetchCodes('POSITION'),
      hrStore.fetchCodes('JOB_TITLE'),
    ])
    positionOptions.value = positions.filter(c => c.isActive)
    jobTitleOptions.value = titles.filter(c => c.isActive)
  } catch {
    positionOptions.value = []
    jobTitleOptions.value = []
  }
}

/** 트리 구조의 부서 목록을 depth 정보와 함께 평면 리스트로 펼침 */
function flattenTree(nodes: Department[], depth = 0): (Department & { depth: number })[] {
  const result: (Department & { depth: number })[] = []
  for (const node of nodes) {
    result.push({ ...node, depth })
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, depth + 1))
    }
  }
  return result
}

const totalPages = computed(() => Math.max(1, Math.ceil(hrStore.totalCount / pageSize.value)))

onMounted(async () => {
  await Promise.all([loadData(), loadUsers()])
})

async function loadData() {
  await Promise.all([
    hrStore.fetchEmployees({
      departmentId: selectedDeptId.value || undefined,
      status: selectedStatus.value || undefined,
      search: searchQuery.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    }),
    loadDeptOptions(),
  ])
}

async function loadDeptOptions() {
  try {
    const tree = await hrStore.fetchDepartmentTree()
    deptOptions.value = flattenTree(tree)
  } catch {
    // silently fail
  }
}

function onSearch() {
  currentPage.value = 1
  loadData()
}

function onFilterChange() {
  currentPage.value = 1
  loadData()
}

function goToPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadData()
}

function openCreateModal() {
  editingEmployee.value = null
  formData.value = {
    employeeNo: generateNextEmployeeNo(),
    employmentType: 'full_time',
    status: 'active',
    annualLeaveDays: 15,
    birthDate: null,
  }
  searchEmailQuery.value = ''
  loadCodeOptions()
  showFormModal.value = true
}

function openEditModal(emp: Employee) {
  editingEmployee.value = emp
  formData.value = { ...emp }
  searchEmailQuery.value = emp.email || ''
  loadCodeOptions()
  showFormModal.value = true
}

async function saveEmployee() {
  saving.value = true
  try {
    const userId = localStorage.getItem('userId') || ''
    const payload = { ...formData.value }

    // 필수값 보정
    if (!payload.hireDate) {
      payload.hireDate = new Date().toISOString().split('T')[0]
    }
    if (payload.departmentId === '') {
      payload.departmentId = null as any
    }
    if (payload.userId === '') {
      payload.userId = null as any
    }
    if (payload.birthDate === '' || !payload.birthDate) {
      payload.birthDate = null as any
    }
    if (payload.emergencyContact === '') {
      payload.emergencyContact = null as any
    }
    if (payload.emergencyPhone === '') {
      payload.emergencyPhone = null as any
    }
    if (payload.bankName === '') {
      payload.bankName = null as any
    }
    if (payload.bankAccount === '') {
      payload.bankAccount = null as any
    }
    if (payload.annualLeaveDays === null || payload.annualLeaveDays === undefined || (payload.annualLeaveDays as any) === '') {
      payload.annualLeaveDays = 15
    }
    
    // 퇴사 상태 및 퇴사일 데이터 무결성 보정
    if (payload.status === 'resigned') {
      if (!payload.resignationDate || payload.resignationDate === '') {
        payload.resignationDate = new Date().toISOString().split('T')[0]
      }
      payload.leaveStartDate = null as any
      payload.leaveEndDate = null as any
    } else if (payload.status === 'leave') {
      if (!payload.leaveStartDate || payload.leaveStartDate === '') {
        payload.leaveStartDate = new Date().toISOString().split('T')[0]
      }
      if (payload.leaveEndDate === '') {
        payload.leaveEndDate = null as any
      }
      payload.resignationDate = null as any
    } else {
      payload.resignationDate = null as any
      payload.leaveStartDate = null as any
      payload.leaveEndDate = null as any
    }
    
    payload.createdBy = userId

    if (editingEmployee.value) {
      await hrStore.updateEmployee(editingEmployee.value.id, payload)
    } else {
      await hrStore.createEmployee(payload)
    }
    showFormModal.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function confirmDelete(emp: Employee) {
  const msg = `${emp.employeeNo} (${emp.position}) 사원을 삭제하시겠습니까?`
  if (!window.confirm(msg)) return
  await hrStore.deleteEmployee(emp.id)
  await loadData()
}

async function confirmStatusChange(emp: Employee, status: string) {
  if (!window.confirm(`${emp.employeeNo}의 상태를 '${status}'로 변경하시겠습니까?`)) return
  await hrStore.updateEmployeeStatus(emp.id, status)
  await loadData()
}

function statusBadgeClass(status: string): string {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
    case 'leave':
      return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
    case 'resigned':
      return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
    default:
      return 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
  }
}
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">사원</h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
          사원 마스터 데이터 및 조직 구조 관리
        </p>
      </div>
      <button
        @click="openCreateModal"
        class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors cursor-pointer border-none"
      >
        <Plus class="w-4 h-4" />
        사원 추가
      </button>
    </div>

    <!-- Filter Bar -->
    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-4 mb-4">
      <div class="flex flex-wrap items-center gap-3">
        <div class="relative flex-1 min-w-[200px]">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="이름 또는 사번으로 검색..."
            class="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
            @keyup.enter="onSearch"
          />
        </div>
        <div class="w-44">
          <select
            v-model="selectedDeptId"
            @change="onFilterChange"
            class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
          >
            <option value="">전체 부서</option>
            <option
              v-for="dept in deptOptions"
              :key="dept.id"
              :value="dept.id"
            >
              <span v-for="i in dept.depth" :key="i">&nbsp;&nbsp;&nbsp;</span>{{ dept.name }}
            </option>
          </select>
        </div>
        <div class="w-36">
          <select
            v-model="selectedStatus"
            @change="onFilterChange"
            class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
          >
            <option value="">전체 상태</option>
            <option value="active">재직</option>
            <option value="leave">휴직</option>
            <option value="resigned">퇴사</option>
          </select>
        </div>
        <button
          @click="onSearch"
          class="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white rounded-lg text-sm font-medium transition-colors cursor-pointer border-none shrink-0"
        >
          <Search class="w-4 h-4" />
          검색
        </button>
      </div>
    </div>

    <!-- Employee Table -->
    <div class="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 overflow-hidden">
      <!-- Loading State -->
      <div v-if="hrStore.loading" class="flex items-center justify-center py-16">
        <Loader2 class="w-8 h-8 text-indigo-500 animate-spin" />
      </div>

      <!-- Empty State -->
      <div
        v-else-if="hrStore.employees.length === 0"
        class="flex flex-col items-center justify-center py-16 text-slate-400 dark:text-slate-500"
      >
        <Briefcase class="w-12 h-12 mb-3" />
        <p class="text-sm font-medium">사원이 없습니다</p>
        <p class="text-xs mt-1">사원을 추가해주세요.</p>
      </div>

      <!-- Data Table -->
      <table v-else class="w-full text-sm">
        <thead class="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-200 dark:border-slate-700">
          <tr>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">사번</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">이름</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">직위</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">부서</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">상태</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">입사일</th>
            <th class="text-left px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">이메일</th>
            <th class="text-right px-4 py-3 font-semibold text-slate-600 dark:text-slate-400">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          <tr
            v-for="emp in hrStore.employees"
            :key="emp.id"
            class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors"
          >
            <td class="px-4 py-3 font-medium text-slate-900 dark:text-white">{{ emp.employeeNo }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300 font-medium">{{ emp.employeeName || '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ emp.position || '-' }}</td>
            <td class="px-4 py-3 text-slate-700 dark:text-slate-300">{{ emp.departmentName || '-' }}</td>
            <td class="px-4 py-3">
              <span
                class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                :class="statusBadgeClass(emp.status)"
              >
                {{ emp.status }}
              </span>
            </td>
            <td class="px-4 py-3 text-slate-600 dark:text-slate-400">{{ emp.hireDate || '-' }}</td>
            <td class="px-4 py-3 text-slate-600 dark:text-slate-400 truncate max-w-[200px]">{{ emp.email || '-' }}</td>
            <td class="px-4 py-3 text-right">
              <div class="flex items-center justify-end gap-2">
                <button
                  @click="openEditModal(emp)"
                  class="px-2 py-1 text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 rounded transition-colors cursor-pointer border-none"
                >
                  수정
                </button>
                <button
                  @click="confirmDelete(emp)"
                  class="px-2 py-1 text-xs font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 rounded transition-colors cursor-pointer border-none"
                >
                  삭제
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div
        v-if="hrStore.employees.length > 0"
        class="flex items-center justify-between px-4 py-3 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/60"
      >
        <span class="text-xs text-slate-500 dark:text-slate-400">
          총 {{ hrStore.totalCount }}명 ({{ currentPage }}/{{ totalPages }}페이지)
        </span>
        <div class="flex items-center gap-1">
          <button
            :disabled="currentPage <= 1"
            @click="goToPage(currentPage - 1)"
            class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer border-none"
          >
            <ChevronLeft class="w-4 h-4" />
          </button>
          <template v-for="p in totalPages" :key="p">
            <button
              v-if="p === 1 || p === totalPages || Math.abs(p - currentPage) <= 2"
              @click="goToPage(p)"
              class="px-2.5 py-1 text-xs rounded transition-colors cursor-pointer border-none"
              :class="p === currentPage ? 'bg-indigo-600 text-white' : 'hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400'"
            >
              {{ p }}
            </button>
            <span
              v-else-if="p === currentPage - 3 || p === currentPage + 3"
              class="px-1 text-xs text-slate-400"
            >...</span>
          </template>
          <button
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
            class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer border-none"
          >
            <ChevronRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <Teleport to="body">
      <div
        v-if="showFormModal"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
        @click.self="showFormModal = false"
      >
        <div class="bg-white dark:bg-slate-900 rounded-xl shadow-2xl border border-slate-200 dark:border-slate-800 w-full max-w-lg mx-4 max-h-[80vh] overflow-y-auto">
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800">
            <h2 class="text-lg font-bold text-slate-900 dark:text-white">
              {{ editingEmployee ? '사원 수정' : '사원 추가' }}
            </h2>
            <button
              @click="showFormModal = false"
              class="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border-none text-slate-500"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">사번</label>
              <input
                v-model="formData.employeeNo"
                type="text"
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                :placeholder="editingEmployee ? formData.employeeNo : '미입력 시 자동 발급'"
                :readonly="!!editingEmployee"
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">이름</label>
              <input
                v-model="formData.employeeName"
                type="text"
                readonly
                placeholder="이메일을 선택하면 자동으로 입력됩니다."
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-slate-100 dark:bg-slate-800/60 text-slate-600 dark:text-slate-400 cursor-not-allowed focus:outline-none"
              />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">직위</label>
                <select
                  v-model="formData.position"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                >
                  <option value="">선택</option>
                  <option
                    v-for="opt in positionOptions"
                    :key="opt.code"
                    :value="opt.code"
                  >{{ opt.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">직책</label>
                <select
                  v-model="formData.jobTitle"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                >
                  <option value="">선택</option>
                  <option
                    v-for="opt in jobTitleOptions"
                    :key="opt.code"
                    :value="opt.code"
                  >{{ opt.label }}</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">부서</label>
              <select
                v-model="formData.departmentId"
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
              >
                <option value="">부서 선택</option>
                <option
                  v-for="dept in deptOptions"
                  :key="dept.id"
                  :value="dept.id"
                >
                  <span v-for="i in dept.depth" :key="i">&nbsp;&nbsp;&nbsp;</span>{{ dept.name }}
                </option>
              </select>
            </div>

            <div class="relative">
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">이메일</label>
              <div class="relative">
                <input
                  v-model="searchEmailQuery"
                  type="text"
                  placeholder="이메일 또는 이름 검색..."
                  @focus="showEmailDropdown = true"
                  @blur="handleEmailBlur"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
                <!-- Autocomplete Dropdown Panel -->
                <div
                  v-if="showEmailDropdown && filteredUsers.length > 0"
                  class="absolute z-[100] left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg shadow-lg"
                >
                  <div
                    v-for="user in filteredUsers"
                    :key="user.id"
                    @mousedown="selectUser(user)"
                    class="px-4 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-indigo-50 dark:hover:bg-slate-700/60 cursor-pointer flex items-center justify-between"
                  >
                    <span>{{ user.email }}</span>
                    <span class="text-xs text-slate-400 dark:text-slate-500 font-medium">{{ user.fullName }}</span>
                  </div>
                </div>
                <div
                  v-else-if="showEmailDropdown && filteredUsers.length === 0"
                  class="absolute z-[100] left-0 right-0 mt-1 px-4 py-2 text-xs text-slate-400 dark:text-slate-500 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg shadow-lg"
                >
                  일치하는 사용자가 없습니다
                </div>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">전화번호</label>
                <input
                  v-model="formData.phone"
                  type="text"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">생년월일</label>
                <input
                  v-model="formData.birthDate"
                  type="date"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">입사일</label>
                <input
                  v-model="formData.hireDate"
                  type="date"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">고용 형태</label>
                <select
                  v-model="formData.employmentType"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                >
                  <option value="full_time">정규직</option>
                  <option value="part_time">파트타임</option>
                  <option value="contract">계약직</option>
                  <option value="intern">인턴</option>
                </select>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">연간 연차 일수</label>
                <input
                  v-model.number="formData.annualLeaveDays"
                  type="number"
                  step="0.5"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">상태</label>
                <select
                  v-model="formData.status"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                >
                  <option value="active">재직</option>
                  <option value="leave">휴직</option>
                  <option value="resigned">퇴사</option>
                </select>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">비상 연락망 이름</label>
                <input
                  v-model="formData.emergencyContact"
                  type="text"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">비상 연락망 전화번호</label>
                <input
                  v-model="formData.emergencyPhone"
                  type="text"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">급여 계좌 은행명</label>
                <input
                  v-model="formData.bankName"
                  type="text"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">급여 계좌 번호</label>
                <input
                  v-model="formData.bankAccount"
                  type="text"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
            </div>

            <div v-if="formData.status === 'resigned'">
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">퇴사일자</label>
              <input
                v-model="formData.resignationDate"
                type="date"
                class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
              />
            </div>

            <div v-if="formData.status === 'leave'" class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">휴직일자</label>
                <input
                  v-model="formData.leaveStartDate"
                  type="date"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">복직예정일</label>
                <input
                  v-model="formData.leaveEndDate"
                  type="date"
                  class="w-full px-3 py-2 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
                />
              </div>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">메모</label>
              <textarea
                v-model="formData.memo"
                rows="2"
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
              @click="saveEmployee"
              :disabled="saving"
              class="px-4 py-2 text-sm font-medium bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white rounded-lg transition-colors flex items-center gap-2 cursor-pointer border-none"
            >
              <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
              {{ editingEmployee ? '수정' : '등록' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
