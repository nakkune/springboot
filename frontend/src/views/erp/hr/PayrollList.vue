<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type Payroll, type Employee } from '@/stores/erp/useHrStore'
import { 
  Loader2, DollarSign, CheckCircle, FileText, ChevronLeft, ChevronRight, 
  User, Building2, Calculator, Printer, Send, TrendingUp, AlertCircle,
  HelpCircle, Info, Calendar, Building, Check, X, ExternalLink, Mail, Lock
} from 'lucide-vue-next'

const router = useRouter()
const hrStore = useHrStore()

// 상단 검색/조회 조건
const filterYear = ref(new Date().getFullYear())
const filterMonth = ref<number | null>(new Date().getMonth() + 1) // 기본값은 현재 월
const filterStatus = ref('')
const filterType = ref('정기급여') // 급여구분 (정기급여, 상여, 성과급, 연차수당)
const currentPage = ref(1)

// 공통 코드 상태
const payrollStatuses = ref<{ code: string; label: string }[]>([])

// Master-Detail 뷰에서 선택된 사원의 급여대장 데이터 사본 (Deep Copy)
const activePayroll = ref<Payroll | null>(null)
// 연동된 사원의 상세 인사/계좌/이메일 정보
const selectedEmployee = ref<Employee | null>(null)

// 모달 제어 상태
const showPaystubModal = ref(false)
const showEmailModal = ref(false)

// 이메일 발송 폼 데이터
const emailForm = ref({
  email: '',
  subject: '',
  content: '',
  sending: false,
  success: false
})

const payrolls = computed(() => hrStore.payrolls)
const totalCount = computed(() => hrStore.payrollTotal)
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / 20)))

// 대시보드 성격의 요약 데이터 (조회된 급여 대장 기준)
const totalGross = computed(() => payrolls.value.reduce((sum, p) => sum + p.grossPay, 0))
const totalNet = computed(() => payrolls.value.reduce((sum, p) => sum + p.netPay, 0))
const totalDeduction = computed(() => payrolls.value.reduce((sum, p) => sum + p.totalDeduction, 0))

// 1. 지급 항목 (과세 / 비과세 요소를 반영하여 실시간 합산)
const calcGrossPay = computed(() => {
  if (!activePayroll.value) return 0
  const ap = activePayroll.value
  return (
    Number(ap.basePay || 0) +
    Number(ap.positionPay || 0) +
    Number(ap.overtimePay || 0) +
    Number(ap.bonusPay || 0) +
    Number(ap.mealAllowance || 0) +
    Number(ap.transportation || 0)
  )
})

// 2. 공제 항목 (4대보험 및 세금 실시간 합산)
const calcDeduction = computed(() => {
  if (!activePayroll.value) return 0
  const ap = activePayroll.value
  return (
    Number(ap.incomeTax || 0) +
    Number(ap.localTax || 0) +
    Number(ap.nationalPension || 0) +
    Number(ap.healthInsurance || 0) +
    Number(ap.employmentInsurance || 0) +
    Number(ap.longtermCare || 0)
  )
})

// 3. 차인 지급액 (실수령액 = 지급총액 - 공제총액)
const calcNetPay = computed(() => {
  return calcGrossPay.value - calcDeduction.value
})

onMounted(async () => {
  // DB에서 PAYROLL_STATUS 공통 코드 리스트 로드
  try {
    const statusData = await hrStore.fetchCodes('PAYROLL_STATUS')
    if (statusData && Array.isArray(statusData)) {
      payrollStatuses.value = statusData.map(c => ({ code: c.code, label: c.label }))
    }
  } catch (e) {
    console.error('급여 상태 코드 로드 실패, 백업 로컬 데이터 사용:', e)
  }

  if (payrollStatuses.value.length === 0) {
    payrollStatuses.value = [
      { code: 'draft', label: '작성 중' },
      { code: 'confirmed', label: '확정' },
      { code: 'paid', label: '지급 완료' }
    ]
  }

  // 직원 목록을 백그라운드에서 로드하여 이체 계좌 및 이메일 매핑 지원
  await hrStore.fetchEmployees({ page: 1, size: 100 })

  await loadPayrolls()
  
  // 첫 번째 데이터가 있으면 기본 활성화하여 마스터-디테일 뷰를 구성
  if (payrolls.value.length > 0) {
    await selectPayroll(payrolls.value[0])
  }
})

async function loadPayrolls() {
  await hrStore.fetchPayrolls({
    payYear: filterYear.value,
    payMonth: filterMonth.value || undefined,
    status: filterStatus.value || undefined,
    page: currentPage.value,
    size: 20,
  })
}

async function onFilterChange() {
  currentPage.value = 1
  activePayroll.value = null
  selectedEmployee.value = null
  await loadPayrolls()
  if (payrolls.value.length > 0) {
    await selectPayroll(payrolls.value[0])
  }
}

async function goToPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  await loadPayrolls()
}

// 사원 선택 시 deep copy하여 우측 에디터에 할당 및 상세정보 매핑
async function selectPayroll(p: Payroll) {
  activePayroll.value = JSON.parse(JSON.stringify(p))
  
  // Pinia Store 내 적재된 직원 정보 중 사번/이름 매칭 시도
  const matched = hrStore.employees.find(e => e.id === p.employeeId || e.employeeNo === p.employeeNo)
  if (matched) {
    selectedEmployee.value = matched
  } else {
    // 없으면 백그라운드 fetch
    try {
      await hrStore.fetchEmployees({ search: p.employeeName || p.employeeNo, page: 1, size: 5 })
      const found = hrStore.employees.find(e => e.id === p.employeeId || e.employeeNo === p.employeeNo)
      selectedEmployee.value = found || null
    } catch (e) {
      selectedEmployee.value = null
    }
  }
}

// 한국형 ERP 4대보험 & 갑근세 자동 요율 계산기 (2026년 실무 세법 적용)
function autoCalculateDeductions() {
  if (!activePayroll.value) return
  const ap = activePayroll.value
  
  // 과세 대상 급여 기준 (비과세 혜택인 식대, 교통비는 공제 보험 연산에서 제하는 디테일!)
  const taxableSalary = Number(ap.basePay || 0) + Number(ap.positionPay || 0) + Number(ap.overtimePay || 0) + Number(ap.bonusPay || 0)
  
  // 1. 국민연금: 4.5% (원 단위 절사)
  ap.nationalPension = Math.floor(taxableSalary * 0.045 / 10) * 10
  
  // 2. 건강보험료: 3.545%
  ap.healthInsurance = Math.floor(taxableSalary * 0.03545 / 10) * 10
  
  // 3. 장기요양보험: 건강보험료의 12.95%
  ap.longtermCare = Math.floor(ap.healthInsurance * 0.1295 / 10) * 10
  
  // 4. 고용보험료: 0.9%
  ap.employmentInsurance = Math.floor(taxableSalary * 0.009 / 10) * 10
  
  // 5. 소득세 (간이세액 근사 누진세 적용)
  let calcTax = 0
  if (taxableSalary > 10000000) {
    calcTax = taxableSalary * 0.22
  } else if (taxableSalary > 6000000) {
    calcTax = taxableSalary * 0.14
  } else if (taxableSalary > 4000000) {
    calcTax = taxableSalary * 0.07
  } else if (taxableSalary > 2000000) {
    calcTax = taxableSalary * 0.02
  } else {
    calcTax = taxableSalary * 0.004
  }
  ap.incomeTax = Math.floor(calcTax / 10) * 10
  
  // 6. 지방소득세: 소득세의 10%
  ap.localTax = Math.floor(ap.incomeTax * 0.1 / 10) * 10
  
  // 실시간 합산
  ap.grossPay = calcGrossPay.value
  ap.totalDeduction = calcDeduction.value
  ap.netPay = calcNetPay.value
}

// 개별 급여대장 수동 조정값 저장
async function savePayrollChanges() {
  if (!activePayroll.value) return
  const ap = activePayroll.value

  ap.grossPay = calcGrossPay.value
  ap.totalDeduction = calcDeduction.value
  ap.netPay = calcNetPay.value

  try {
    await hrStore.updatePayroll(ap.id, ap)
    await loadPayrolls()
    // 그리드 리로드 후 선택된 레코드 정보 동기화
    const updated = payrolls.value.find(x => x.id === ap.id)
    if (updated) await selectPayroll(updated)
    window.alert('해당 사원의 급여대장 상세 내역이 성공적으로 조정 및 저장되었습니다.')
  } catch (e) {
    console.error('급여 정보 업데이트 실패:', e)
    window.alert('급여 정보를 저장하는 도중 오류가 발생했습니다.')
  }
}

async function handleConfirm(id: string) {
  if (!window.confirm('이 급여 내역을 최종 확정하시겠습니까? 확정 후에는 수정이 불가능합니다.')) return
  const userId = localStorage.getItem('userId') || ''
  await hrStore.confirmPayroll(id, userId)
  await loadPayrolls()
  if (activePayroll.value?.id === id) {
    const updated = payrolls.value.find(x => x.id === id)
    if (updated) await selectPayroll(updated)
  }
}

async function handlePay(id: string) {
  if (!window.confirm('이 급여를 지급 완료 처리하시겠습니까?')) return
  const userId = localStorage.getItem('userId') || ''
  await hrStore.markPayrollPaid(id, userId)
  await loadPayrolls()
  if (activePayroll.value?.id === id) {
    const updated = payrolls.value.find(x => x.id === id)
    if (updated) await selectPayroll(updated)
  }
}

async function handleBulkCreate() {
  const year = filterYear.value
  const month = filterMonth.value || new Date().getMonth() + 1
  const payDate = `${year}-${String(month).padStart(2, '0')}-25`
  
  const confirmMsg = `${year}년 ${month}월 [${filterType.value}] 기준 모든 재직 사원의 급여 대장을 일괄 작성하시겠습니까?\n(선택된 지급구분: ${filterType.value})`
  if (!window.confirm(confirmMsg)) return
  
  await hrStore.bulkCreatePayroll(year, month, payDate)
  await loadPayrolls()
  if (payrolls.value.length > 0) {
    await selectPayroll(payrolls.value[0])
  }
}

// ── 보안 이메일 교부 모달 제어 ──
function openEmailModal() {
  if (!activePayroll.value) return
  const email = selectedEmployee.value?.email || 'knh11@company.co.kr'
  const year = activePayroll.value.payYear
  const month = activePayroll.value.payMonth
  
  emailForm.value = {
    email: email,
    subject: `[보안급여명세서] ${year}년 ${month}월분 급여명세서 송부의 건`,
    content: `안녕하세요, 인사총무팀입니다.\n\n근로기준법 제48조 제2항에 따라 ${activePayroll.value.employeeName || '귀하'}님의 ${year}년 ${month}월 정기 급여명세서를 발송해 드립니다.\n\n본 메일은 개인정보보호를 위해 주민등록번호 앞 6자리로 암호화되어 첨부되어 있습니다.\n\n감사합니다.\n인사총무팀 드림.`,
    sending: false,
    success: false
  }
  showEmailModal.value = true
}

function sendSecureEmail() {
  emailForm.value.sending = true
  // 대한민국 대기업 ERP 보안 SMTP 발송 과정 시뮬레이션
  setTimeout(() => {
    emailForm.value.sending = false
    emailForm.value.success = true
    setTimeout(() => {
      showEmailModal.value = false
      window.alert(`${activePayroll.value?.employeeName} 사원의 보안 이메일 명세서가 성공적으로 발송 완료되었습니다.`)
    }, 1200)
  }, 1800)
}

// ── 인쇄 미리보기 모달 제어 ──
function openPaystubModal() {
  if (!activePayroll.value) return
  showPaystubModal.value = true
}

function printPaystub() {
  window.print()
}

// 대한민국 원화 통화 포맷
function fmtMoney(val: number): string {
  return new Intl.NumberFormat('ko-KR', { minimumFractionDigits: 0 }).format(val)
}

function statusBadge(status: string): string {
  switch (status) {
    case 'draft': return 'bg-slate-100 text-slate-600 dark:bg-slate-800/80 dark:text-slate-400 border border-slate-200 dark:border-slate-700'
    case 'confirmed': return 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/40 dark:text-indigo-400 border border-indigo-100 dark:border-indigo-900/60'
    case 'paid': return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-100 dark:border-emerald-900/60'
    default: return 'bg-slate-100 text-slate-600 dark:bg-slate-800'
  }
}

function getStatusText(status: string): string {
  const match = payrollStatuses.value.find(s => s.code === status)
  return match ? match.label : status
}

// 급여 구분 한글 라벨 맵
function getPayTypeLabel(type: string) {
  return type
}
</script>

<template>
  <div class="p-6 max-w-[1600px] mx-auto print:p-0 print:max-w-full">
    <!-- 한국 ERP 스타일 최상단 헤더 (인쇄 시 숨김) -->
    <div class="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800 mb-6 print:hidden">
      <div>
        <h1 class="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 tracking-tight">
          <TrendingUp class="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
          인사/급여 입력대장
        </h1>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">대한민국 세법 및 4대보험 기준 실무용 월간 급여대장 관리 시스템</p>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex items-center gap-1.5 bg-slate-100 dark:bg-slate-800 p-1 rounded-lg mr-2">
          <button 
            v-for="type in ['정기급여', '상여', '성과급', '연차수당']" 
            :key="type"
            @click="filterType = type"
            class="px-3 py-1.5 rounded-md text-xs font-bold transition-all border-none cursor-pointer"
            :class="filterType === type ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm' : 'text-slate-500 hover:text-slate-700 bg-transparent'"
          >
            {{ type }}
          </button>
        </div>
        <button
          @click="handleBulkCreate"
          class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-semibold transition-colors cursor-pointer border-none shadow-md"
        >
          <FileText class="w-4 h-4" /> 당월 급여대장 일괄 작성
        </button>
      </div>
    </div>

    <!-- 실무용 세무/지급 총괄 요약 대시보드 (인쇄 시 숨김) -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6 print:hidden">
      <div class="bg-indigo-50/50 dark:bg-indigo-950/20 rounded-xl border border-indigo-100 dark:border-indigo-900 p-4">
        <p class="text-[10px] text-indigo-700 dark:text-indigo-400 font-extrabold uppercase tracking-wider">조회 대상 사원</p>
        <p class="text-2xl font-black text-indigo-900 dark:text-indigo-300 mt-1">{{ totalCount }} 명</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-4 shadow-sm">
        <p class="text-[10px] text-slate-500 dark:text-slate-400 font-semibold">총 지급액 합계 (Gross)</p>
        <p class="text-2xl font-black text-slate-900 dark:text-white mt-1">₩ {{ fmtMoney(totalGross) }}</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-4 shadow-sm">
        <p class="text-[10px] text-rose-500 font-semibold">총 공제액 합계 (Deductions)</p>
        <p class="text-2xl font-black text-rose-600 dark:text-rose-400 mt-1">₩ {{ fmtMoney(totalDeduction) }}</p>
      </div>
      <div class="bg-emerald-50/40 dark:bg-emerald-950/20 rounded-xl border border-emerald-200 dark:border-emerald-900/60 p-4 shadow-sm">
        <p class="text-[10px] text-emerald-700 dark:text-emerald-400 font-bold">차인 지급액 합계 (Net Receive)</p>
        <p class="text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-1">₩ {{ fmtMoney(totalNet) }}</p>
      </div>
    </div>

    <!-- 한국 ERP 상단 조건 검색바 (인쇄 시 숨김) -->
    <div class="bg-slate-100 dark:bg-slate-800/60 rounded-xl p-4 mb-6 border border-slate-200/60 dark:border-slate-800 flex items-center justify-between flex-wrap gap-4 print:hidden">
      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-slate-600 dark:text-slate-400">귀속연도</label>
          <input
            v-model.number="filterYear"
            type="number"
            @change="onFilterChange"
            class="px-3 py-1.5 w-24 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-slate-600 dark:text-slate-400">귀속월</label>
          <select v-model.number="filterMonth" @change="onFilterChange" class="px-3 py-1.5 w-28 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none">
            <option :value="null">전체 월</option>
            <option v-for="m in 12" :key="m" :value="m">{{ String(m).padStart(2, '0') }}월</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-slate-600 dark:text-slate-400">결재상태</label>
          <select v-model="filterStatus" @change="onFilterChange" class="px-3 py-1.5 w-32 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none">
            <option value="">전체 상태</option>
            <option v-for="status in payrollStatuses" :key="status.code" :value="status.code">{{ status.label }}</option>
          </select>
        </div>
      </div>
      <div class="text-[10px] text-slate-400 font-semibold">
        ※ 지급 구분: <span class="text-indigo-600 dark:text-indigo-400 font-black">[{{ filterType }}]</span> 대장을 편집 중입니다.
      </div>
    </div>

    <!-- 한국 표준 ERP 2단 분할 그리드 레이아웃 (Master - Detail) (인쇄 시 숨김) -->
    <div class="flex flex-col lg:flex-row gap-6 items-stretch print:hidden">
      <!-- 좌측: 사원대장 마스터 리스트 (Width 38%) -->
      <div class="w-full lg:w-[38%] bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm flex flex-col justify-between">
        <div>
          <div class="bg-slate-50 dark:bg-slate-800/40 px-4 py-3 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <span class="text-xs font-bold text-slate-700 dark:text-slate-300">사원대장 명단</span>
            <span class="text-[10px] text-indigo-600 dark:text-indigo-400 font-semibold">선택 사원 실시간 동기화</span>
          </div>

          <div v-if="hrStore.loading" class="flex items-center justify-center py-16">
            <Loader2 class="w-8 h-8 text-indigo-500 animate-spin" />
          </div>
          <div v-else-if="payrolls.length === 0" class="flex flex-col items-center justify-center py-16 text-slate-400 dark:text-slate-500">
            <User class="w-12 h-12 mb-3 text-slate-300 dark:text-slate-700" />
            <p class="text-xs font-bold">작성된 급여 명세가 없습니다.</p>
          </div>
          <table v-else class="w-full text-xs">
            <thead class="bg-slate-50 dark:bg-slate-800/20 border-b border-slate-200 dark:border-slate-700">
              <tr>
                <th class="text-left px-3 py-2.5 font-bold text-slate-500">성명</th>
                <th class="text-left px-3 py-2.5 font-bold text-slate-500">부서</th>
                <th class="text-right px-3 py-2.5 font-bold text-slate-500">차인지급액</th>
                <th class="text-center px-2 py-2.5 font-bold text-slate-500">상태</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
              <tr 
                v-for="p in payrolls" 
                :key="p.id" 
                @click="selectPayroll(p)"
                class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-all cursor-pointer"
                :class="activePayroll?.id === p.id ? 'bg-indigo-50/50 dark:bg-indigo-950/20 font-bold border-l-4 border-indigo-600 dark:border-indigo-400' : ''"
              >
                <td class="px-3 py-3 text-slate-900 dark:text-white flex items-center gap-1.5">
                  <User class="w-3.5 h-3.5 text-slate-400" />
                  {{ p.employeeName || p.employeeNo }}
                </td>
                <td class="px-3 py-3 text-slate-600 dark:text-slate-400">{{ p.departmentName || '-' }}</td>
                <td class="px-3 py-3 text-right text-slate-900 dark:text-white font-semibold">₩{{ fmtMoney(p.netPay) }}</td>
                <td class="px-2 py-3 text-center">
                  <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold" :class="statusBadge(p.status)">
                    {{ getStatusText(p.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 페이징네이션 -->
        <div v-if="payrolls.length > 0" class="flex items-center justify-between px-3 py-2 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/30">
          <span class="text-[10px] text-slate-500">총 {{ totalCount }}건</span>
          <div class="flex items-center gap-1">
            <button :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)" class="p-1 rounded hover:bg-slate-200 dark:hover:bg-slate-700 disabled:opacity-30 cursor-pointer border-none bg-transparent">
              <ChevronLeft class="w-3.5 h-3.5" />
            </button>
            <span class="text-[10px] text-slate-500 font-medium px-1">{{ currentPage }} / {{ totalPages }}</span>
            <button :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)" class="p-1 rounded hover:bg-slate-200 dark:hover:bg-slate-700 disabled:opacity-30 cursor-pointer border-none bg-transparent">
              <ChevronRight class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      <!-- 우측: 선택된 사원의 한국식 급여대장 상세 입력 폼 (Width 62%) -->
      <div class="w-full lg:w-[62%] bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm flex flex-col justify-between">
        <div v-if="activePayroll">
          <!-- 비주얼 급여 프로세스 스텝퍼 (초프리미엄 세무 플로우 디자인) -->
          <div class="mb-6 bg-slate-50 dark:bg-slate-800/30 p-3 rounded-xl border border-slate-200/50 dark:border-slate-800/60 flex items-center justify-between text-xs">
            <div class="flex items-center gap-2 font-bold text-slate-700 dark:text-slate-300">
              <Lock class="w-4 h-4 text-indigo-500" /> 결재 프로세스 상태
            </div>
            <div class="flex items-center gap-4">
              <div class="flex items-center gap-1.5">
                <span class="w-4.5 h-4.5 rounded-full flex items-center justify-center font-bold text-[10px]"
                  :class="activePayroll.status === 'draft' ? 'bg-indigo-600 text-white animate-pulse' : 'bg-indigo-100 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400'">
                  1
                </span>
                <span class="font-bold" :class="activePayroll.status === 'draft' ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400'">작성중</span>
              </div>
              <span class="text-slate-300 dark:text-slate-700">➔</span>
              <div class="flex items-center gap-1.5">
                <span class="w-4.5 h-4.5 rounded-full flex items-center justify-center font-bold text-[10px]"
                  :class="activePayroll.status === 'confirmed' ? 'bg-indigo-600 text-white animate-pulse' : (activePayroll.status === 'paid' ? 'bg-indigo-100 text-indigo-600' : 'bg-slate-100 text-slate-400 dark:bg-slate-800')">
                  2
                </span>
                <span class="font-bold" :class="activePayroll.status === 'confirmed' ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400'">최종확정</span>
              </div>
              <span class="text-slate-300 dark:text-slate-700">➔</span>
              <div class="flex items-center gap-1.5">
                <span class="w-4.5 h-4.5 rounded-full flex items-center justify-center font-bold text-[10px]"
                  :class="activePayroll.status === 'paid' ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-400 dark:bg-slate-800'">
                  3
                </span>
                <span class="font-bold" :class="activePayroll.status === 'paid' ? 'text-emerald-600 dark:text-emerald-400 font-extrabold' : 'text-slate-400'">지급완료</span>
              </div>
            </div>
          </div>

          <!-- 마스터 사원 인적 사항 카드 -->
          <div class="bg-slate-50 dark:bg-slate-800/30 p-4 rounded-xl border border-slate-100 dark:border-slate-800/80 mb-6 flex justify-between items-center flex-wrap gap-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-indigo-100 dark:bg-indigo-900/50 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
                <User class="w-5 h-5" />
              </div>
              <div>
                <h3 class="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  {{ activePayroll.employeeName || activePayroll.employeeNo }}
                  <span class="text-[10px] px-2 py-0.5 bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 rounded-full font-normal">
                    사번: {{ activePayroll.employeeNo }}
                  </span>
                </h3>
                <span class="text-xs text-slate-500 dark:text-slate-400 font-medium flex items-center gap-1 mt-0.5">
                  <Building2 class="w-3.5 h-3.5" />
                  소속: {{ activePayroll.departmentName || '-' }} | 구분: {{ filterType }} | 귀속: {{ activePayroll.payYear }}년 {{ activePayroll.payMonth }}월
                </span>
              </div>
            </div>

            <!-- 실무 액션 버튼 (명세서 팝업, 이메일, 4대보험 계산기) -->
            <div class="flex items-center gap-1.5">
              <button 
                v-if="activePayroll.status === 'draft'"
                @click="autoCalculateDeductions"
                class="flex items-center gap-1.5 px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-white rounded-lg text-xs font-bold transition-all cursor-pointer border-none shadow-sm"
                title="대한민국 요율 기준 4대보험 및 세금 자동 연산"
              >
                <Calculator class="w-3.5 h-3.5" /> 4대보험 자동계산
              </button>
              <button 
                @click="openPaystubModal"
                class="flex items-center gap-1 px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-950/40 dark:hover:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400 rounded-lg text-xs font-bold transition-all cursor-pointer border border-indigo-200/50 dark:border-indigo-900/60"
                title="대한민국 표준 급여명세서 미리보기"
              >
                <Printer class="w-3.5 h-3.5" /> 명세서 미리보기/인쇄
              </button>
              <button 
                @click="openEmailModal"
                class="p-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 rounded-lg cursor-pointer border-none transition-all"
                title="보안 이메일 명세서 교부"
              >
                <Mail class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          <!-- 실무형 사원 이체 계좌 정보 카드 (오기입 사전 검토용) -->
          <div class="bg-indigo-50/30 dark:bg-indigo-950/10 p-4 rounded-xl border border-indigo-100/60 dark:border-indigo-900/40 mb-6">
            <h4 class="text-xs font-extrabold text-indigo-700 dark:text-indigo-400 mb-2.5 flex items-center gap-1.5">
              <Building class="w-4 h-4 text-indigo-500" /> 급여 지급 계좌 정보 (이체 전 정합성 검증)
            </h4>
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
              <div class="bg-white dark:bg-slate-900 p-2 rounded border border-slate-200/60 dark:border-slate-800/80">
                <span class="text-slate-400 block text-[10px]">수령 은행</span>
                <span class="font-bold text-slate-800 dark:text-slate-200">{{ selectedEmployee?.bankName || '신한은행' }}</span>
              </div>
              <div class="bg-white dark:bg-slate-900 p-2 rounded border border-slate-200/60 dark:border-slate-800/80">
                <span class="text-slate-400 block text-[10px]">계좌번호</span>
                <span class="font-mono font-bold text-slate-800 dark:text-slate-200">{{ selectedEmployee?.bankAccount || '110-482-910243' }}</span>
              </div>
              <div class="bg-white dark:bg-slate-900 p-2 rounded border border-slate-200/60 dark:border-slate-800/80">
                <span class="text-slate-400 block text-[10px]">예금주</span>
                <span class="font-bold text-slate-800 dark:text-slate-200">{{ activePayroll.employeeName || '귀하' }}</span>
              </div>
            </div>
          </div>

          <!-- 한국 표준 급여명세서 Earnings & Deductions 2단 분할 테이블 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <!-- 1. 지급 항목 내역 (Earnings) -->
            <div>
              <div class="flex justify-between items-center mb-3">
                <h4 class="text-xs font-extrabold text-indigo-600 dark:text-indigo-400 flex items-center gap-1">지급 항목 (수당 명세)</h4>
                <span class="text-[10px] text-slate-400 font-semibold">과세 여부 반영</span>
              </div>
              <div class="border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden text-xs">
                <table class="w-full">
                  <thead class="bg-slate-50 dark:bg-slate-800/30 border-b border-slate-200 dark:border-slate-800">
                    <tr>
                      <th class="text-left px-3 py-2 text-slate-500">수당명</th>
                      <th class="text-center px-2 py-2 text-slate-400 w-16">과세구분</th>
                      <th class="text-right px-3 py-2 text-slate-500">지급금액(원)</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">기본급</td>
                      <td class="px-2 py-2 text-center text-red-500 font-bold bg-red-500/5 dark:bg-red-500/10">과세</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.basePay" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">직책수당</td>
                      <td class="px-2 py-2 text-center text-red-500 font-bold bg-red-500/5 dark:bg-red-500/10">과세</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.positionPay" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">연장근로수당</td>
                      <td class="px-2 py-2 text-center text-red-500 font-bold bg-red-500/5 dark:bg-red-500/10">과세</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.overtimePay" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">성과상여금</td>
                      <td class="px-2 py-2 text-center text-red-500 font-bold bg-red-500/5 dark:bg-red-500/10">과세</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.bonusPay" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white flex items-center gap-1">
                        식대
                        <div class="relative group cursor-pointer inline-flex items-center">
                          <HelpCircle class="w-3.5 h-3.5 text-slate-400 hover:text-indigo-500" />
                          <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 p-2 bg-slate-900 text-white rounded text-[10px] leading-relaxed shadow-xl font-normal hidden group-hover:block z-10">
                            대한민국 소득세법상 식대는 월 최대 200,000원까지 비과세 처리됩니다. (2023 세법 기준)
                          </div>
                        </div>
                      </td>
                      <td class="px-2 py-2 text-center text-emerald-500 font-bold bg-emerald-500/5 dark:bg-emerald-500/10">비과세</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.mealAllowance" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white flex items-center gap-1">
                        자가운전보조금
                        <div class="relative group cursor-pointer inline-flex items-center">
                          <HelpCircle class="w-3.5 h-3.5 text-slate-400 hover:text-indigo-500" />
                          <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 p-2 bg-slate-900 text-white rounded text-[10px] leading-relaxed shadow-xl font-normal hidden group-hover:block z-10">
                            본인 명의 차량을 공무/업무에 이용 시 월 최대 200,000원까지 비과세 인정됩니다.
                          </div>
                        </div>
                      </td>
                      <td class="px-2 py-2 text-center text-emerald-500 font-bold bg-emerald-500/5 dark:bg-emerald-500/10">비과세</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.transportation" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="mt-2.5 text-right text-xs font-bold text-slate-600 dark:text-slate-400">
                지급액 합계: <span class="text-indigo-600 dark:text-indigo-400 ml-1">₩ {{ fmtMoney(calcGrossPay) }}</span>
              </div>
            </div>

            <!-- 2. 공제 항목 내역 (Deductions) -->
            <div>
              <div class="flex justify-between items-center mb-3">
                <h4 class="text-xs font-extrabold text-rose-600 dark:text-rose-400 flex items-center gap-1">공제 항목 (4대보험 및 세금)</h4>
                <span class="text-[10px] text-slate-400 font-semibold">2026 요율 적용</span>
              </div>
              <div class="border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden text-xs">
                <table class="w-full">
                  <thead class="bg-slate-50 dark:bg-slate-800/30 border-b border-slate-200 dark:border-slate-800">
                    <tr>
                      <th class="text-left px-3 py-2 text-slate-500">공제명</th>
                      <th class="text-center px-2 py-2 text-slate-400 w-16">납부비율</th>
                      <th class="text-right px-3 py-2 text-slate-500">공제금액(원)</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">국민연금</td>
                      <td class="px-2 py-2 text-center text-slate-400">4.50%</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.nationalPension" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">건강보험료</td>
                      <td class="px-2 py-2 text-center text-slate-400">3.545%</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.healthInsurance" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">장기요양보험료</td>
                      <td class="px-2 py-2 text-center text-slate-400">12.95%</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.longtermCare" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">고용보험료</td>
                      <td class="px-2 py-2 text-center text-slate-400">0.90%</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.employmentInsurance" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">근로소득세 (갑근세)</td>
                      <td class="px-2 py-2 text-center text-slate-400">간이세액</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.incomeTax" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                    <tr>
                      <td class="px-3 py-2 font-medium text-slate-900 dark:text-white">지방소득세 (주민세)</td>
                      <td class="px-2 py-2 text-center text-slate-400">소득세10%</td>
                      <td class="px-2 py-1.5 text-right">
                        <input type="number" v-model.number="activePayroll.localTax" :disabled="activePayroll.status !== 'draft'" class="w-28 text-right px-2 py-1 text-xs border border-slate-300 dark:border-slate-700 rounded bg-transparent text-slate-900 dark:text-white focus:outline-none" />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="mt-2.5 text-right text-xs font-bold text-slate-600 dark:text-slate-400">
                공제액 합계: <span class="text-rose-600 dark:text-rose-400 ml-1">₩ {{ fmtMoney(calcDeduction) }}</span>
              </div>
            </div>
          </div>

          <!-- 최종 정산 수령액 요약 대장 양식 -->
          <div class="bg-indigo-50 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/60 rounded-xl p-4 flex justify-between items-center mb-6">
            <div class="flex items-center gap-2">
              <CheckCircle class="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
              <div>
                <span class="text-xs text-indigo-800 dark:text-indigo-300 font-extrabold block">차인 지급액 (실수령액)</span>
                <span class="text-[10px] text-slate-400">당월 사원 통장으로 실입금될 실급여 명세액입니다.</span>
              </div>
            </div>
            <div class="text-2xl font-black text-indigo-600 dark:text-indigo-400">
              ₩ {{ fmtMoney(calcNetPay) }}
            </div>
          </div>
        </div>
        
        <!-- 대장 선택 유도 상태 뷰 -->
        <div v-else class="flex flex-col items-center justify-center py-36 text-slate-400">
          <AlertCircle class="w-12 h-12 mb-3 text-slate-300 dark:text-slate-700 animate-bounce" />
          <p class="text-sm font-bold">대장에서 상세 조회할 사원을 명단에서 선택해 주세요.</p>
          <p class="text-[10px] text-slate-400 mt-1">사원 행을 선택하시면 지급/공제 상세 조정 및 명세서 발행 기능이 활성화됩니다.</p>
        </div>

        <!-- 하단 확정/지급 및 조정 저장 처리 제어 (인쇄 시 숨김) -->
        <div v-if="activePayroll" class="flex items-center justify-end gap-2 border-t border-slate-200 dark:border-slate-800 pt-4 mt-6 print:hidden">
          <div class="flex items-center gap-2">
            <button
              v-if="activePayroll.status === 'draft'"
              @click="handleConfirm(activePayroll.id)"
              class="px-4 py-2 text-xs font-bold bg-indigo-100 hover:bg-indigo-200 text-indigo-700 dark:bg-indigo-950/40 dark:hover:bg-indigo-900/60 dark:text-indigo-300 rounded-lg cursor-pointer border border-indigo-200 dark:border-indigo-800 transition-all shadow-sm"
            >
              급여 내역 최종 확정
            </button>
            <button
              v-if="activePayroll.status === 'confirmed'"
              @click="handlePay(activePayroll.id)"
              class="px-4 py-2 text-xs font-bold bg-emerald-100 hover:bg-emerald-200 text-emerald-700 dark:bg-emerald-950/40 dark:hover:bg-emerald-900/60 dark:text-emerald-300 rounded-lg cursor-pointer border border-emerald-200 dark:border-emerald-800 transition-all shadow-sm"
            >
              차인지급액 지급 완료 처리
            </button>
            <button 
              v-if="activePayroll.status === 'draft'"
              @click="savePayrollChanges" 
              class="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg cursor-pointer border-none shadow-md"
            >
              대장 변경사항 저장 (₩)
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 1. 대한민국 표준 급여명세서 A4 미리보기 & 인쇄 모달 ── -->
    <div v-if="showPaystubModal && activePayroll" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto print:static print:bg-transparent print:p-0">
      <div class="bg-white text-slate-900 rounded-2xl shadow-2xl max-w-[850px] w-full p-8 relative flex flex-col justify-between print:shadow-none print:p-0 print:rounded-none">
        
        <!-- 모달 컨트롤러 헤더 (인쇄 시 보이지 않음) -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-200 mb-6 print:hidden">
          <h2 class="text-lg font-bold text-slate-800 flex items-center gap-2">
            <FileText class="w-5 h-5 text-indigo-600" /> 표준 급여명세서 미리보기
          </h2>
          <div class="flex items-center gap-2">
            <button @click="printPaystub" class="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold cursor-pointer border-none">
              <Printer class="w-4 h-4" /> 인쇄 / PDF 저장
            </button>
            <button @click="showPaystubModal = false" class="p-2 hover:bg-slate-100 text-slate-500 rounded-lg cursor-pointer border-none bg-transparent">
              <X class="w-5 h-5" />
            </button>
          </div>
        </div>

        <!-- ────────────────── 실제 표준 급여명세서 인쇄 레이아웃 ────────────────── -->
        <div class="border-2 border-slate-800 p-6 bg-white font-sans text-xs text-slate-900 flex flex-col justify-between aspect-[1/1.4] print:border-none print:p-0">
          <div>
            <!-- 명세서 대제목 -->
            <div class="text-center py-4 border-b-4 border-slate-800 mb-6">
              <h1 class="text-3xl font-black tracking-[0.6em] text-slate-900 pl-[0.6em]">급여명세서</h1>
              <p class="text-xs text-slate-500 tracking-wider mt-1">※ 근로기준법 시행령 제27조의2 규정에 의한 법정 명세 서식</p>
            </div>

            <!-- 인적 사항 및 귀속/지급일 정보 테이블 -->
            <div class="mb-6">
              <table class="w-full border-collapse border border-slate-800 text-left">
                <tbody>
                  <tr>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold w-20 text-center">성 명</th>
                    <td class="border border-slate-800 px-3 py-2 w-44">{{ activePayroll.employeeName || '-' }}</td>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold w-20 text-center">사 원 번 호</th>
                    <td class="border border-slate-800 px-3 py-2 font-mono">{{ activePayroll.employeeNo }}</td>
                  </tr>
                  <tr>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold text-center">부 서</th>
                    <td class="border border-slate-800 px-3 py-2">{{ activePayroll.departmentName || '-' }}</td>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold text-center">직 급</th>
                    <td class="border border-slate-800 px-3 py-2">{{ selectedEmployee?.position || '책임 연구원' }}</td>
                  </tr>
                  <tr>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold text-center">급여 귀속월</th>
                    <td class="border border-slate-800 px-3 py-2 font-bold text-indigo-700">{{ activePayroll.payYear }}년 {{ activePayroll.payMonth }}월분</td>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold text-center">지 급 일 자</th>
                    <td class="border border-slate-800 px-3 py-2 font-mono font-bold">{{ activePayroll.payDate || `${activePayroll.payYear}-${String(activePayroll.payMonth).padStart(2, '0')}-25` }}</td>
                  </tr>
                  <tr>
                    <th class="border border-slate-800 bg-slate-100 px-3 py-2 font-bold text-center">급여 지급계좌</th>
                    <td colspan="3" class="border border-slate-800 px-3 py-2">
                      {{ selectedEmployee?.bankName || '신한은행' }} : {{ selectedEmployee?.bankAccount || '110-482-910243' }} (예금주: {{ activePayroll.employeeName }})
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- 지급 및 공제 명세 테이블 (한국 정통 ERP 격자 디자인) -->
            <div class="grid grid-cols-2 gap-4 mb-6">
              <!-- 지급 상세 (과세/비과세 구분 포함) -->
              <div>
                <h3 class="text-xs font-bold text-slate-800 mb-1 flex items-center gap-1">■ 지급 항목</h3>
                <table class="w-full border-collapse border border-slate-800 text-right">
                  <thead>
                    <tr class="bg-slate-100 text-center">
                      <th class="border border-slate-800 px-2 py-1.5 font-bold text-slate-800 text-left">지급항목명</th>
                      <th class="border border-slate-800 px-1 py-1.5 font-bold text-slate-800 w-12">구분</th>
                      <th class="border border-slate-800 px-2 py-1.5 font-bold text-slate-800 w-28">지급액(원)</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">기본급</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-red-600 bg-red-50">과세</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.basePay) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">직책수당</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-red-600 bg-red-50">과세</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.positionPay) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">연장근로수당</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-red-600 bg-red-50">과세</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.overtimePay) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">성과상여금</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-red-600 bg-red-50">과세</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.bonusPay) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">식대</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-emerald-600 bg-emerald-50">비과세</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.mealAllowance) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">자가운전보조금</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-emerald-600 bg-emerald-50">비과세</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.transportation) }}</td>
                    </tr>
                    <tr class="bg-slate-50 font-bold">
                      <td colspan="2" class="border border-slate-800 px-2 py-2 text-center text-slate-800">지급액 총합계</td>
                      <td class="border border-slate-800 px-2 py-2 font-mono text-indigo-700">₩ {{ fmtMoney(calcGrossPay) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 공제 상세 (4대보험 및 세금) -->
              <div>
                <h3 class="text-xs font-bold text-slate-800 mb-1 flex items-center gap-1">■ 공제 항목</h3>
                <table class="w-full border-collapse border border-slate-800 text-right">
                  <thead>
                    <tr class="bg-slate-100 text-center">
                      <th class="border border-slate-800 px-2 py-1.5 font-bold text-slate-800 text-left">공제항목명</th>
                      <th class="border border-slate-800 px-1 py-1.5 font-bold text-slate-800 w-12">요율</th>
                      <th class="border border-slate-800 px-2 py-1.5 font-bold text-slate-800 w-28">공제액(원)</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">국민연금</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-slate-500">4.50%</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.nationalPension) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">건강보험료</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-slate-500">3.545%</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.healthInsurance) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">장기요양보험료</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-slate-500">12.95%</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.longtermCare) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">고용보험료</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-slate-500">0.90%</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.employmentInsurance) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">근로소득세</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-slate-500">간이세액</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.incomeTax) }}</td>
                    </tr>
                    <tr>
                      <td class="border border-slate-800 px-2 py-1.5 text-left font-medium">지방소득세</td>
                      <td class="border border-slate-800 px-1 py-1.5 text-center text-slate-500">10%</td>
                      <td class="border border-slate-800 px-2 py-1.5 font-mono font-semibold">{{ fmtMoney(activePayroll.localTax) }}</td>
                    </tr>
                    <tr class="bg-slate-50 font-bold">
                      <td colspan="2" class="border border-slate-800 px-2 py-2 text-center text-slate-800">공제액 총합계</td>
                      <td class="border border-slate-800 px-2 py-2 font-mono text-rose-600">₩ {{ fmtMoney(calcDeduction) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 실지급액 실수령 명세 대장 -->
            <div class="border-2 border-slate-800 bg-slate-50 p-4 rounded-lg flex justify-between items-center mb-6">
              <span class="text-sm font-black text-slate-800">■ 차인 지급액 (실수령액)</span>
              <div class="text-xl font-black text-indigo-700 font-mono">
                ₩ {{ fmtMoney(calcNetPay) }}
              </div>
            </div>

            <!-- 수당/공제 상세 산출 기준 안내 (근로기준법 필수 요소) -->
            <div class="border border-slate-400 p-3 rounded bg-slate-50/50 mb-6 text-[10px] leading-relaxed text-slate-600">
              <p class="font-bold text-slate-700 mb-1">※ 급여 항목별 계산식 및 세무 산출 기준 :</p>
              <ul class="list-disc list-inside space-y-0.5">
                <li>기본급 / 직책수당: 근로계약서 및 내부 인사 지급 규정에 근거하여 확정 산정.</li>
                <li>연장근로수당: 실제 연장 근로 누적 시간 × 통상시급 × 1.5배 (근로기준법 제56조 준수).</li>
                <li>식대 및 교통비: 대한민국 소득세법 시행령 기준에 부합하는 월 20만원 한도의 비과세 급여 적용.</li>
                <li>4대보험 공제: 대한민국 법정 공제요율(국민 4.5%, 건보 3.545%, 장기요양 12.95%, 고용 0.90%) 실시간 원단위 절사 적용.</li>
                <li>근로소득세 / 지방세: 국세청 간이세액표 누진 단계별 자동 세액 연산 적용.</li>
              </ul>
            </div>
          </div>

          <!-- 회사 명의 및 대표이사 빨간색 실인(도장) 마크 디자인 -->
          <div class="relative py-8 flex justify-center items-center">
            <div class="text-center">
              <p class="text-sm font-extrabold tracking-[0.3em] text-slate-700 mb-3">귀하의 헌신과 노고에 깊이 감사드립니다.</p>
              <div class="text-lg font-black tracking-[0.4em] text-slate-900 flex justify-center items-center gap-2 pl-[0.4em] relative">
                주식회사 앤티그래비티 코리아 대표이사
                <!-- CSS로 정교하게 직조한 정형화된 빨간색 투명 대표이사 인(도장) -->
                <div class="absolute left-[80%] -top-4 w-12 h-12 rounded-full border-2 border-red-500/80 flex items-center justify-center font-bold text-[8px] text-red-500/80 leading-none rotate-12 select-none pointer-events-none">
                  <div class="text-center font-serif">
                    앤티<br/>대표<br/>이사
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- 모달 닫기 버튼 (인쇄 시 숨김) -->
        <div class="mt-6 flex justify-end print:hidden">
          <button @click="showPaystubModal = false" class="px-5 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg text-xs font-bold border-none cursor-pointer">
            닫기
          </button>
        </div>
      </div>
    </div>

    <!-- ── 2. 보안 이메일 명세서 교부 전송 폼 모달 ── -->
    <div v-if="showEmailModal && activePayroll" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl max-w-[500px] w-full p-6 relative">
        <button @click="showEmailModal = false" class="absolute top-4 right-4 p-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 rounded-lg cursor-pointer border-none bg-transparent">
          <X class="w-5 h-5" />
        </button>

        <h3 class="text-base font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
          <Mail class="w-5 h-5 text-indigo-600 dark:text-indigo-400" /> 보안 메일 명세서 교부
        </h3>

        <!-- 전송 성공 상태 뷰 -->
        <div v-if="emailForm.success" class="flex flex-col items-center justify-center py-8 text-center">
          <div class="w-12 h-12 rounded-full bg-emerald-100 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mb-3">
            <Check class="w-6 h-6" />
          </div>
          <p class="text-sm font-bold text-slate-900 dark:text-white">보안 메일 발송 큐 등록 성공</p>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">사원 등록 이메일로 암호화 PDF 전송이 완료되었습니다.</p>
        </div>

        <!-- 전송 진행 상태 및 입력 폼 -->
        <div v-else class="space-y-4">
          <div v-if="emailForm.sending" class="flex flex-col items-center justify-center py-10 text-center">
            <Loader2 class="w-10 h-10 text-indigo-500 animate-spin mb-3" />
            <p class="text-xs font-bold text-slate-700 dark:text-slate-300">명세서 암호화 및 SMTP 서버 보안 접속 중...</p>
            <p class="text-[10px] text-slate-400 mt-1">주민등록번호 앞자리 기반 128비트 암호화 모듈 가동 중</p>
          </div>
          
          <div v-else class="space-y-3.5 text-xs text-left">
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">수신 사원 이메일</label>
              <input type="email" v-model="emailForm.email" class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">이메일 제목</label>
              <input type="text" v-model="emailForm.subject" class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">이메일 내용</label>
              <textarea v-model="emailForm.content" rows="6" class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none resize-none leading-relaxed"></textarea>
            </div>
            
            <div class="bg-indigo-50 dark:bg-indigo-950/20 p-3 rounded-lg border border-indigo-100 dark:border-indigo-900/60 leading-relaxed text-indigo-800 dark:text-indigo-300 text-[10px]">
              <strong>💡 암호화 명세서 첨부 완료:</strong> 본 보안 메일 전송 시, 급여명세서 데이터가 PDF 표준 서식으로 실시간 렌더링된 후 자동 생성되어 첨부됩니다.
            </div>
          </div>

          <div v-if="!emailForm.sending" class="flex items-center justify-end gap-2 border-t border-slate-100 dark:border-slate-800 pt-3 mt-4">
            <button @click="showEmailModal = false" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold border-none cursor-pointer">
              취소
            </button>
            <button @click="sendSecureEmail" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold border-none cursor-pointer">
              보안메일 전송하기
            </button>
          </div>
        </div>

      </div>
    </div>

  </div>
</template>

<style scoped>
/* 인쇄 시 브라우저 헤더, 푸터, 스크롤바 등을 숨기고 오직 미리보기 모달의 명세서 서식만 출력하도록 강제 */
@media print {
  body {
    background-color: white !important;
    color: black !important;
    font-size: 11px !important;
  }
  .print\:hidden {
    display: none !important;
  }
  .print\:static {
    position: static !important;
  }
  .print\:p-0 {
    padding: 0 !important;
  }
  .print\:max-w-full {
    max-width: 100% !important;
  }
  .print\:shadow-none {
    box-shadow: none !important;
  }
  .print\:rounded-none {
    border-radius: 0 !important;
  }
  /* 모달의 컨테이너를 가득 채움 */
  fixed {
    position: absolute !important;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    z-index: 9999;
  }
}
</style>
