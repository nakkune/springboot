import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'

// ── Existing types ──

export interface Department {
  id: string
  parentId: string | null
  name: string
  code: string
  managerId: string | null
  sortOrder: number
  isActive: boolean
  children?: Department[]
}

export interface Employee {
  id: string
  userId: string | null
  employeeName?: string
  employeeNo: string
  departmentId: string
  departmentName: string
  position: string
  jobTitle: string
  employmentType: string
  hireDate: string
  resignationDate: string | null
  status: string
  phone: string
  email: string
  emergencyContact: string
  emergencyPhone: string
  bankName: string
  bankAccount: string
  annualLeaveDays: number
  leaveStartDate?: string | null
  leaveEndDate?: string | null
  birthDate?: string | null
  memo: string
  createdBy: string
  createdAt: string
}

// ── New types ──

export interface Attendance {
  id: string
  employeeId: string
  employeeName: string
  employeeNo: string
  workDate: string
  checkIn: string | null
  checkOut: string | null
  workHours: number
  overtimeHours: number
  status: string
  memo: string
}

export interface LeaveRequest {
  id: string
  employeeId: string
  employeeName: string
  employeeNo: string
  leaveType: string
  startDate: string
  endDate: string
  totalDays: number
  reason: string
  status: string
  approverId: string | null
  approverName: string | null
  rejectReason: string | null
}

export interface LeaveBalance {
  id: string
  employeeId: string
  employeeName: string
  employeeNo: string
  year: number
  totalDays: number
  usedDays: number
  remainingDays: number
}

export interface Payroll {
  id: string
  employeeId: string
  employeeName: string
  employeeNo: string
  departmentName: string
  payYear: number
  payMonth: number
  payDate: string
  basePay: number
  positionPay: number
  overtimePay: number
  bonusPay: number
  mealAllowance: number
  transportation: number
  incomeTax: number
  localTax: number
  nationalPension: number
  healthInsurance: number
  employmentInsurance: number
  longtermCare: number
  grossPay: number
  totalDeduction: number
  netPay: number
  status: string
  memo: string
}

export interface CodeDto {
  id: string
  codeGroup: string
  codeGroupName: string
  code: string
  label: string
  sortOrder: number
  isActive: boolean
}

export interface CodeGroupDto {
  codeGroup: string
  codeGroupName: string
}

export interface PerformanceReview {
  id: string
  employeeId: string
  employeeName: string
  employeeNo: string
  reviewerId: string
  reviewerName: string
  reviewYear: number
  reviewPeriod: string
  ratings: Record<string, number>
  totalScore: number | null
  comment: string
  status: string
  submittedAt: string | null
  createdAt: string
  updatedAt: string
}

// ── [신규 추가] 5대 급여 도메인 인터페이스 ──

export interface PayrollLedger {
  id?: string
  title: string
  payYear: number
  payMonth: number
  payDate: string
  payType: string
  startDate: string
  endDate: string
  status?: string
  createdBy?: string
  createdAt?: string
  updatedAt?: string
  totalGross?: number
  totalDeduction?: number
  totalNet?: number
  employeeCount?: number
}

export interface PayrollCode {
  code: string
  name: string
  type: string // allowance, deduction
  isTaxable: boolean
  taxFreeLimit: number
  isSystem?: boolean
  isActive?: boolean
  sortOrder?: number
}

export interface SalaryTemplate {
  employeeId: string
  employeeName?: string
  employeeNo?: string
  departmentName?: string
  basePay: number
  positionPay: number
  mealAllowance: number
  carAllowance: number
  useNationalPension: boolean
  useHealthInsurance: boolean
  useEmploymentInsurance: boolean
  incomeTaxRate: number
  createdAt?: string
  updatedAt?: string
}

export interface PayrollDetail {
  id?: string
  payrollItemId: string
  code: string
  name?: string
  type?: string
  amount: number
  isTaxable: boolean
}

export interface PayrollItem {
  id: string
  ledgerId: string
  employeeId: string
  employeeName: string
  employeeNo: string
  departmentName: string
  position: string
  grossPay: number
  totalDeduction: number
  netPay: number
  bankName: string
  bankAccount: string
  bankOwner: string
  status: string
  memo: string
  details?: PayrollDetail[]
}

function buildTree(flatList: Department[]): Department[] {
  const map = new Map<string, Department>()
  const roots: Department[] = []

  flatList.forEach((dept) => {
    map.set(dept.id, { ...dept, children: [] })
  })

  flatList.forEach((dept) => {
    const node = map.get(dept.id)!
    if (dept.parentId && map.has(dept.parentId)) {
      const parent = map.get(dept.parentId)!
      parent.children = parent.children || []
      parent.children.push(node)
    } else if (!dept.parentId) {
      roots.push(node)
    }
  })

  return roots.sort((a, b) => a.sortOrder - b.sortOrder)
}

export const useHrStore = defineStore('hr', () => {
  const employees = ref<Employee[]>([])
  const departments = ref<Department[]>([])
  const loading = ref(false)
  const totalCount = ref(0)
  const currentEmployee = ref<Employee | null>(null)

  // ── Attendance state ──
  const attendanceRecords = ref<Attendance[]>([])

  // ── Leave state ──
  const leaveRequests = ref<LeaveRequest[]>([])
  const leaveBalance = ref<LeaveBalance | null>(null)

  // ── Payroll state ──
  const payrolls = ref<Payroll[]>([])
  const payrollTotal = ref(0)

  // ── Performance Review state ──
  const reviews = ref<PerformanceReview[]>([])
  const reviewTotal = ref(0)

  // ── [신규 추가] 5대 급여 도메인 State ──
  const ledgers = ref<PayrollLedger[]>([])
  const activeLedger = ref<PayrollLedger | null>(null)
  const ledgerItems = ref<PayrollItem[]>([])
  const payrollCodes = ref<PayrollCode[]>([])
  const salaryTemplates = ref<SalaryTemplate[]>([])

  // ── Department actions ──
  async function fetchDepartments() {
    loading.value = true
    try {
      const data = await api.get('/erp/hr/departments')
      departments.value = data as unknown as Department[]
    } finally {
      loading.value = false
    }
  }

  async function fetchDepartmentTree(): Promise<Department[]> {
    const data = await api.get('/erp/hr/departments/tree')
    return data as unknown as Department[]
  }

  async function createDepartment(dto: Partial<Department>) {
    const data = await api.post('/erp/hr/departments', dto)
    return data
  }

  async function updateDepartment(id: string, dto: Partial<Department>) {
    const data = await api.put(`/erp/hr/departments/${id}`, dto)
    return data
  }

  async function deleteDepartment(id: string) {
    await api.delete(`/erp/hr/departments/${id}`)
  }

  async function fetchEmployeeByUserId(userId: string) {
    try {
      const data = await api.get(`/erp/hr/employees/user/${userId}`)
      currentEmployee.value = data as unknown as Employee
      return data
    } catch (e) {
      console.error('직원 프로필 조회 실패:', e)
      currentEmployee.value = null
    }
  }

  // ── Employee actions ──
  async function fetchEmployees(params: {
    departmentId?: string
    status?: string
    search?: string
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const query = new URLSearchParams()
      if (params.departmentId) query.set('departmentId', params.departmentId)
      if (params.status) query.set('status', params.status)
      if (params.search) query.set('search', params.search)
      query.set('page', String(params.page ?? 1))
      query.set('size', String(params.size ?? 20))

      const result = await api.get(`/erp/hr/employees?${query.toString()}`) as any
      employees.value = result.items as Employee[]
      totalCount.value = result.total as number
    } finally {
      loading.value = false
    }
  }

  async function createEmployee(dto: Partial<Employee>) {
    const data = await api.post('/erp/hr/employees', dto)
    return data
  }

  async function updateEmployee(id: string, dto: Partial<Employee>) {
    const data = await api.put(`/erp/hr/employees/${id}`, dto)
    return data
  }

  async function updateEmployeeStatus(id: string, status: string) {
    await api.patch(`/erp/hr/employees/${id}/status`, { status })
  }

  async function deleteEmployee(id: string) {
    await api.delete(`/erp/hr/employees/${id}`)
  }

  // ── Attendance actions ──
  async function fetchAttendance(employeeId?: string, fromDate?: string, toDate?: string, status?: string) {
    loading.value = true
    try {
      const params = new URLSearchParams()
      if (employeeId) params.set('employeeId', employeeId)
      if (fromDate) params.set('fromDate', fromDate)
      if (toDate) params.set('toDate', toDate)
      if (status) params.set('status', status)
      const data = await api.get(`/erp/hr/attendance?${params.toString()}`)
      attendanceRecords.value = data as unknown as Attendance[]
    } finally {
      loading.value = false
    }
  }

  async function createAttendance(dto: Partial<Attendance>) {
    const data = await api.post('/erp/hr/attendance', dto)
    return data
  }

  async function checkIn(employeeId: string) {
    const data = await api.post('/erp/hr/attendance/check-in', { employeeId })
    return data
  }

  async function checkOut(employeeId: string) {
    const data = await api.post('/erp/hr/attendance/check-out', { employeeId })
    return data
  }

  async function updateAttendance(id: string, dto: Partial<Attendance>) {
    const data = await api.put(`/erp/hr/attendance/${id}`, dto)
    return data
  }

  // ── Leave actions ──
  async function fetchLeaves(params: { employeeId?: string; managerId?: string; status?: string }) {
    loading.value = true
    try {
      const query = new URLSearchParams()
      if (params.employeeId) query.set('employeeId', params.employeeId)
      if (params.managerId) query.set('managerId', params.managerId)
      if (params.status) query.set('status', params.status)
      const data = await api.get(`/erp/hr/leaves?${query.toString()}`)
      leaveRequests.value = data as unknown as LeaveRequest[]
    } finally {
      loading.value = false
    }
  }

  async function createLeave(dto: Partial<LeaveRequest>) {
    const data = await api.post('/erp/hr/leaves', dto)
    return data
  }

  async function approveLeave(id: string, approverId: string) {
    await api.post(`/erp/hr/leaves/${id}/approve`, { approverId })
  }

  async function rejectLeave(id: string, approverId: string, rejectReason: string) {
    await api.post(`/erp/hr/leaves/${id}/reject`, { approverId, rejectReason })
  }

  async function cancelLeave(id: string) {
    await api.post(`/erp/hr/leaves/${id}/cancel`)
  }

  async function fetchLeaveBalance(employeeId: string, year?: number) {
    const params = new URLSearchParams({ employeeId, year: String(year ?? new Date().getFullYear()) })
    const data = await api.get(`/erp/hr/leaves/balance?${params.toString()}`)
    leaveBalance.value = data as unknown as LeaveBalance
    return data
  }

  // ── Payroll actions ──
  async function fetchPayrolls(params: {
    payYear?: number
    payMonth?: number
    status?: string
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const query = new URLSearchParams()
      if (params.payYear) query.set('payYear', String(params.payYear))
      if (params.payMonth) query.set('payMonth', String(params.payMonth))
      if (params.status) query.set('status', params.status)
      query.set('page', String(params.page ?? 1))
      query.set('size', String(params.size ?? 20))
      const result = await api.get(`/erp/hr/payrolls?${query.toString()}`) as any
      payrolls.value = result.items as Payroll[]
      payrollTotal.value = result.total as number
    } finally {
      loading.value = false
    }
  }

  async function fetchEmployeePayrolls(employeeId: string) {
    const data = await api.get(`/erp/hr/payrolls/employee?employeeId=${employeeId}`)
    return data
  }

  async function createPayroll(dto: Partial<Payroll>) {
    const data = await api.post('/erp/hr/payrolls', dto)
    return data
  }

  async function bulkCreatePayroll(payYear: number, payMonth: number, payDate: string) {
    const data = await api.post('/erp/hr/payrolls/bulk-create', { payYear, payMonth, payDate })
    return data
  }

  async function updatePayroll(id: string, dto: Partial<Payroll>) {
    const data = await api.put(`/erp/hr/payrolls/${id}`, dto)
    return data
  }

  async function confirmPayroll(id: string, confirmedBy: string) {
    await api.post(`/erp/hr/payrolls/${id}/confirm`, { confirmedBy })
  }

  async function markPayrollPaid(id: string, confirmedBy: string) {
    await api.post(`/erp/hr/payrolls/${id}/pay`, { confirmedBy })
  }

  async function deletePayroll(id: string) {
    await api.delete(`/erp/hr/payrolls/${id}`)
  }

  // ── [신규 추가] 5대 급여대장 중심 API Actions ──

  async function fetchLedgers(payYear?: number, payMonth?: number, status?: string) {
    loading.value = true
    try {
      const q = new URLSearchParams()
      if (payYear) q.set('payYear', String(payYear))
      if (payMonth) q.set('payMonth', String(payMonth))
      if (status) q.set('status', status)
      const data = await api.get(`/erp/hr/payrolls/ledgers?${q.toString()}`)
      ledgers.value = data as unknown as PayrollLedger[]
    } finally {
      loading.value = false
    }
  }

  async function fetchLedger(id: string) {
    const data = await api.get(`/erp/hr/payrolls/ledgers/${id}`)
    activeLedger.value = data as unknown as PayrollLedger
    return data as unknown as PayrollLedger
  }

  async function createLedger(dto: Partial<PayrollLedger>) {
    const data = await api.post(`/erp/hr/payrolls/ledgers`, dto)
    return data as unknown as PayrollLedger
  }

  async function deleteLedger(id: string) {
    await api.delete(`/erp/hr/payrolls/ledgers/${id}`)
  }

  async function confirmLedger(id: string) {
    await api.post(`/erp/hr/payrolls/ledgers/${id}/confirm`, {})
  }

  async function payLedger(id: string) {
    await api.post(`/erp/hr/payrolls/ledgers/${id}/pay`, {})
  }

  async function fetchLedgerItems(ledgerId: string) {
    loading.value = true
    try {
      const data = await api.get(`/erp/hr/payrolls/ledgers/${ledgerId}/items`)
      ledgerItems.value = data as unknown as PayrollItem[]
    } finally {
      loading.value = false
    }
  }

  async function updateLedgerItemDetails(itemId: string, dto: Partial<PayrollItem>) {
    const data = await api.put(`/erp/hr/payrolls/ledgers/items/${itemId}`, dto)
    return data as unknown as PayrollItem
  }

  async function calculateLedger(ledgerId: string) {
    await api.post(`/erp/hr/payrolls/ledgers/${ledgerId}/calculate`, {})
  }

  async function fetchPayrollCodes() {
    const data = await api.get('/erp/hr/payrolls/codes')
    payrollCodes.value = data as unknown as PayrollCode[]
    return data as unknown as PayrollCode[]
  }

  async function savePayrollCode(dto: Partial<PayrollCode>) {
    const data = await api.post('/erp/hr/payrolls/codes', dto)
    return data as unknown as PayrollCode
  }

  async function fetchSalaryTemplates() {
    loading.value = true
    try {
      const data = await api.get(`/erp/hr/payrolls/templates`)
      salaryTemplates.value = data as unknown as SalaryTemplate[]
    } finally {
      loading.value = false
    }
  }

  async function fetchSalaryTemplate(employeeId: string) {
    const data = await api.get(`/erp/hr/payrolls/templates/${employeeId}`)
    return data as unknown as SalaryTemplate
  }

  async function saveSalaryTemplate(dto: Partial<SalaryTemplate>) {
    const data = await api.post('/erp/hr/payrolls/templates', dto)
    return data as unknown as SalaryTemplate
  }

  // ── Performance Review actions ──
  async function fetchReviews(params: {
    employeeId?: string
    reviewYear?: number
    reviewPeriod?: string
    status?: string
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const query = new URLSearchParams()
      if (params.employeeId) query.set('employeeId', params.employeeId)
      if (params.reviewYear) query.set('reviewYear', String(params.reviewYear))
      if (params.reviewPeriod) query.set('reviewPeriod', params.reviewPeriod)
      if (params.status) query.set('status', params.status)
      query.set('page', String(params.page ?? 1))
      query.set('size', String(params.size ?? 20))
      const result = await api.get(`/erp/hr/reviews?${query.toString()}`) as any
      reviews.value = result.items as PerformanceReview[]
      reviewTotal.value = result.total as number
    } finally {
      loading.value = false
    }
  }

  async function fetchEmployeeReviews(employeeId: string) {
    const data = await api.get(`/erp/hr/reviews/employee?employeeId=${employeeId}`)
    return data
  }

  async function createReview(dto: Partial<PerformanceReview>) {
    const data = await api.post('/erp/hr/reviews', dto)
    return data
  }

  async function updateReview(id: string, dto: Partial<PerformanceReview>) {
    const data = await api.put(`/erp/hr/reviews/${id}`, dto)
    return data
  }

  async function submitReview(id: string) {
    await api.post(`/erp/hr/reviews/${id}/submit`)
  }

  async function acknowledgeReview(id: string) {
    await api.post(`/erp/hr/reviews/${id}/acknowledge`)
  }

  async function deleteReview(id: string) {
    await api.delete(`/erp/hr/reviews/${id}`)
  }

  // ── Code (기준정보) actions ──
  async function fetchCodeGroups(): Promise<CodeGroupDto[]> {
    const data = await api.get('/erp/hr/codes/groups')
    return data as unknown as CodeGroupDto[]
  }

  async function fetchCodes(group: string) {
    const data = await api.get(`/erp/hr/codes?group=${group}`)
    return data as unknown as CodeDto[]
  }

  async function createCode(dto: Partial<CodeDto>) {
    const data = await api.post('/erp/hr/codes', dto)
    return data
  }

  async function updateCode(id: string, dto: Partial<CodeDto>) {
    const data = await api.put(`/erp/hr/codes/${id}`, dto)
    return data
  }

  async function deleteCode(id: string) {
    await api.delete(`/erp/hr/codes/${id}`)
  }

  // ── Computed ──
  const activeEmployees = computed(() =>
    employees.value.filter((e) => e.status === 'active')
  )

  const departmentTree = computed(() => buildTree(departments.value))

  const pendingLeaves = computed(() =>
    leaveRequests.value.filter((l) => l.status === 'pending')
  )

  return {
    // state
    employees, departments, loading, totalCount, currentEmployee,
    attendanceRecords, leaveRequests, leaveBalance,
    payrolls, payrollTotal,
    reviews, reviewTotal,
    // [신규] 5대 급여대장 state
    ledgers, activeLedger, ledgerItems, payrollCodes, salaryTemplates,
    // computed
    activeEmployees, departmentTree, pendingLeaves,
    // department
    fetchDepartments, fetchDepartmentTree,
    createDepartment, updateDepartment, deleteDepartment,
    // code (기준정보)
    fetchCodeGroups, fetchCodes, createCode, updateCode, deleteCode,
    // employee
    fetchEmployees, createEmployee, updateEmployee, updateEmployeeStatus, deleteEmployee, fetchEmployeeByUserId,
    // attendance
    fetchAttendance, createAttendance, checkIn, checkOut, updateAttendance,
    // leave
    fetchLeaves, createLeave, approveLeave, rejectLeave, cancelLeave, fetchLeaveBalance,
    // payroll (레거시 및 신규 5대 매핑 공존)
    fetchPayrolls, fetchEmployeePayrolls, createPayroll,
    bulkCreatePayroll, updatePayroll, confirmPayroll, markPayrollPaid, deletePayroll,
    // [신규] 5대 액션 목록
    fetchLedgers, fetchLedger, createLedger, deleteLedger, confirmLedger, payLedger,
    fetchLedgerItems, updateLedgerItemDetails, calculateLedger,
    fetchPayrollCodes, savePayrollCode,
    fetchSalaryTemplates, fetchSalaryTemplate, saveSalaryTemplate,
    // performance review
    fetchReviews, fetchEmployeeReviews,
    createReview, updateReview,
    submitReview, acknowledgeReview, deleteReview,
  }
})
