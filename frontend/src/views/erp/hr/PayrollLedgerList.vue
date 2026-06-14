<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type PayrollLedger } from '@/stores/erp/useHrStore'
import { 
  FileText, Plus, Calculator, Settings, UserCheck, Calendar, DollarSign,
  TrendingUp, ArrowRight, Trash2, CheckCircle, Clock, AlertCircle, Loader2, X
} from 'lucide-vue-next'

const router = useRouter()
const hrStore = useHrStore()

// 상단 조회 필터
const filterYear = ref(new Date().getFullYear())
const filterMonth = ref<number | null>(null)
const filterStatus = ref('')

// 대장 생성 모달 상태
const showCreateModal = ref(false)
const newLedger = ref({
  title: '',
  payYear: new Date().getFullYear(),
  payMonth: new Date().getMonth() + 1,
  payDate: '',
  payType: '정기급여',
  startDate: '',
  endDate: ''
})

onMounted(async () => {
  await loadLedgers()
})

async function loadLedgers() {
  await hrStore.fetchLedgers(filterYear.value, filterMonth.value || undefined, filterStatus.value || undefined)
}

function handleFilterChange() {
  loadLedgers()
}

// 귀속연월 변경 시 대장명 및 귀속기간 자동 추천 (시니어급 UX 디테일!)
function onPeriodChange() {
  const y = newLedger.value.payYear
  const m = newLedger.value.payMonth
  newLedger.value.title = `${y}년 ${String(m).padStart(2, '0')}월분 ${newLedger.value.payType}`
  
  // 귀속기간 자동 세팅 (해당 월의 1일 ~ 말일)
  const lastDay = new Date(y, m, 0).getDate()
  newLedger.value.startDate = `${y}-${String(m).padStart(2, '0')}-01`
  newLedger.value.endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  
  // 지급일 추천 (기본 25일)
  newLedger.value.payDate = `${y}-${String(m).padStart(2, '0')}-25`
}

function openCreateModal() {
  showCreateModal.value = true
  onPeriodChange()
}

async function handleCreateLedger() {
  if (!newLedger.value.title.trim()) {
    window.alert('급여대장 타이틀을 입력해 주세요.')
    return
  }
  
  try {
    const created = await hrStore.createLedger(newLedger.value)
    showCreateModal.value = false
    window.alert('새로운 급여대장이 성공적으로 생성되었으며, 재직 임직원들의 기본 계약 급여가 연동 주입되었습니다.')
    
    // 생성된 대장의 상세 조정 및 실시간 계산 시트 화면으로 이동
    if (created && created.id) {
      router.push(`/erp/hr/payrolls/ledgers/${created.id}`)
    } else {
      await loadLedgers()
    }
  } catch (e) {
    console.error('대장 생성 실패:', e)
    window.alert('급여대장 생성 도중 오류가 발생했습니다.')
  }
}

async function handleDeleteLedger(id: string) {
  if (!window.confirm('이 급여대장을 삭제하시겠습니까? 대장 내 모든 사원의 개별 지급 내역도 함께 소멸됩니다.')) return
  try {
    await hrStore.deleteLedger(id)
    await loadLedgers()
    window.alert('급여대장이 정상적으로 삭제되었습니다.')
  } catch (e) {
    console.error('대장 삭제 실패:', e)
  }
}

// 대장 상세 화면으로 이동
function enterLedger(id: string) {
  router.push(`/erp/hr/payrolls/ledgers/${id}`)
}

function fmtMoney(val?: number): string {
  if (!val) return '0'
  return new Intl.NumberFormat('ko-KR').format(val)
}

function statusBadge(status?: string): string {
  switch (status) {
    case 'draft': return 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
    case 'confirmed': return 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950/40 dark:text-indigo-400 border border-indigo-200/30'
    case 'paid': return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-200/30'
    default: return 'bg-slate-100 text-slate-600'
  }
}

function getStatusText(status?: string): string {
  switch (status) {
    case 'draft': return '작성 중'
    case 'confirmed': return '최종 확정'
    case 'paid': return '지급 완료'
    default: return status || ''
  }
}
</script>

<template>
  <div class="p-6 max-w-[1600px] mx-auto">
    <!-- 한국 ERP 스타일 대장 헤더 -->
    <div class="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800 mb-6">
      <div>
        <h1 class="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 tracking-tight">
          <TrendingUp class="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
          급여대장 작성/관리
        </h1>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">대한민국 세법 및 사회보험 기준 귀속월별 동적 급여 관리 홈</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="router.push('/erp/hr/payrolls/templates')"
          class="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold transition-all cursor-pointer border border-slate-200 dark:border-slate-700"
        >
          <UserCheck class="w-4 h-4 text-emerald-500" /> 기본계약 급여 템플릿 설정
        </button>
        <button
          @click="router.push('/erp/hr/payrolls/codes')"
          class="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold transition-all cursor-pointer border border-slate-200 dark:border-slate-700"
        >
          <Settings class="w-4 h-4 text-indigo-500" /> 급여 수당/공제 코드 설정
        </button>
        <button
          @click="openCreateModal"
          class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-bold transition-colors cursor-pointer border-none shadow-md"
        >
          <Plus class="w-4 h-4" /> 신규 급여대장 작성
        </button>
      </div>
    </div>

    <!-- 상단 간이 필터 바 -->
    <div class="bg-slate-100 dark:bg-slate-800/60 rounded-xl p-4 mb-6 border border-slate-200/60 dark:border-slate-800 flex items-center justify-between flex-wrap gap-4">
      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-slate-600 dark:text-slate-400">조회연도</label>
          <input
            v-model.number="filterYear"
            type="number"
            @change="handleFilterChange"
            class="px-3 py-1.5 w-24 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-slate-600 dark:text-slate-400">귀속월</label>
          <select v-model="filterMonth" @change="handleFilterChange" class="px-3 py-1.5 w-28 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none">
            <option :value="null">전체</option>
            <option v-for="m in 12" :key="m" :value="m">{{ String(m).padStart(2, '0') }}월</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-slate-600 dark:text-slate-400">진행단계</label>
          <select v-model="filterStatus" @change="handleFilterChange" class="px-3 py-1.5 w-32 text-sm border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none">
            <option value="">전체 상태</option>
            <option value="draft">작성 중</option>
            <option value="confirmed">최종 확정</option>
            <option value="paid">지급 완료</option>
          </select>
        </div>
      </div>
      <div class="text-[10px] text-slate-400 font-semibold">
        ※ 신규 급여대장 작성 시, 당시 재직 중인 사원의 인사 급여 템플릿(연봉 계약)을 복제하여 초기 대장을 벌크 계산합니다.
      </div>
    </div>

    <!-- 로딩 상태 뷰 -->
    <div v-if="hrStore.loading" class="flex flex-col items-center justify-center py-32">
      <Loader2 class="w-10 h-10 text-indigo-500 animate-spin mb-3" />
      <span class="text-sm font-bold text-slate-500">한국 표준 급여대장 목록을 로드하는 중...</span>
    </div>

    <!-- 비어있을 때 뷰 -->
    <div v-else-if="hrStore.ledgers.length === 0" class="flex flex-col items-center justify-center py-28 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm">
      <FileText class="w-16 h-16 text-slate-300 dark:text-slate-700 mb-4" />
      <h3 class="text-base font-bold text-slate-800 dark:text-slate-200">생성된 급여대장이 없습니다.</h3>
      <p class="text-xs text-slate-400 mt-1 max-w-md text-center">매월 급여 계산을 위해 우측 상단의 "신규 급여대장 작성" 버튼을 클릭하여 당월 급여 작성 프로세스를 시작해 주세요.</p>
      <button @click="openCreateModal" class="mt-4 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold border-none cursor-pointer shadow-md transition-all">
        첫 번째 급여대장 생성하기
      </button>
    </div>

    <!-- 급여대장 격자 리스트 뷰 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div 
        v-for="ledger in hrStore.ledgers" 
        :key="ledger.id"
        class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800/80 rounded-2xl shadow-sm hover:shadow-md hover:border-indigo-200 dark:hover:border-indigo-900/60 p-5 transition-all flex flex-col justify-between"
      >
        <div>
          <!-- 카드 헤더 (상태 뱃지 및 타이틀) -->
          <div class="flex items-center justify-between mb-4">
            <span class="px-2.5 py-0.5 rounded-full text-[10px] font-black tracking-wide" :class="statusBadge(ledger.status)">
              {{ getStatusText(ledger.status) }}
            </span>
            <div class="flex items-center gap-1">
              <span class="px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 text-[10px] rounded font-bold">
                {{ ledger.payType }}
              </span>
              <button 
                v-if="ledger.status === 'draft'"
                @click.stop="handleDeleteLedger(ledger.id!)"
                class="p-1 hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-950/30 text-slate-400 rounded cursor-pointer border-none bg-transparent transition-colors"
                title="급여대장 삭제"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>

          <!-- 대장 핵심 정보 -->
          <h3 class="text-base font-black text-slate-900 dark:text-white tracking-tight mb-2">
            {{ ledger.title }}
          </h3>
          
          <div class="space-y-1.5 text-xs text-slate-600 dark:text-slate-400 font-medium mb-5">
            <p class="flex items-center gap-1.5">
              <Calendar class="w-3.5 h-3.5 text-slate-400" />
              급여귀속 : {{ ledger.startDate }} ~ {{ ledger.endDate }}
            </p>
            <p class="flex items-center gap-1.5">
              <Clock class="w-3.5 h-3.5 text-slate-400" />
              급여지급일 : {{ ledger.payDate }}
            </p>
            <p class="flex items-center gap-1.5">
              <UserCheck class="w-3.5 h-3.5 text-slate-400" />
              대상 사원 : {{ ledger.employeeCount || 0 }} 명
            </p>
          </div>
        </div>

        <!-- 실무용 통계 대시보드 요약 대장 (지급총액) -->
        <div class="bg-slate-50 dark:bg-slate-800/40 rounded-xl p-3.5 border border-slate-200/50 dark:border-slate-800/60 mb-5 flex justify-between items-center text-xs">
          <div>
            <span class="text-slate-400 block text-[10px]">차인지급액 합계(실수령)</span>
            <span class="font-black text-slate-900 dark:text-white text-sm font-mono">
              ₩ {{ fmtMoney(ledger.totalNet) }}
            </span>
          </div>
          <div class="text-right">
            <span class="text-slate-400 block text-[10px]">지급총액(세전)</span>
            <span class="font-bold text-slate-600 dark:text-slate-400 font-mono">
              ₩ {{ fmtMoney(ledger.totalGross) }}
            </span>
          </div>
        </div>

        <!-- 대장 입장 액션 버튼 -->
        <button
          @click="enterLedger(ledger.id!)"
          class="w-full flex items-center justify-center gap-1.5 py-2.5 bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-950/40 dark:hover:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400 rounded-xl text-xs font-bold transition-all border border-indigo-200/40 dark:border-indigo-800/60 cursor-pointer"
        >
          급여대장 입력 및 계산 <ArrowRight class="w-3.5 h-3.5" />
        </button>
      </div>
    </div>

    <!-- ── 신규 급여대장 작성/생성 팝업 모달 ── -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl max-w-[500px] w-full p-6 relative">
        <button @click="showCreateModal = false" class="absolute top-4 right-4 p-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 rounded-lg cursor-pointer border-none bg-transparent">
          <X class="w-5 h-5" />
        </button>

        <h3 class="text-base font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
          <Plus class="w-5 h-5 text-indigo-600 dark:text-indigo-400" /> 신규 급여대장 작성
        </h3>

        <div class="space-y-4 text-left text-xs">
          <!-- 귀속연도 및 월 설정 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">귀속연도</label>
              <input 
                type="number" 
                v-model.number="newLedger.payYear" 
                @change="onPeriodChange"
                class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
              />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">귀속월</label>
              <select 
                v-model.number="newLedger.payMonth" 
                @change="onPeriodChange"
                class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none"
              >
                <option v-for="m in 12" :key="m" :value="m">{{ m }}월</option>
              </select>
            </div>
          </div>

          <!-- 급여 구분 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">급여구분 (지급유형)</label>
            <select 
              v-model="newLedger.payType" 
              @change="onPeriodChange"
              class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none"
            >
              <option value="정기급여">정기급여 (기본 연봉 계약 기준)</option>
              <option value="상여">상여 (정기/임시 보너스)</option>
              <option value="성과급">성과급 (특별 성과상여)</option>
              <option value="연차수당">연차수당 (연차 보상 정산)</option>
            </select>
          </div>

          <!-- 급여대장 명칭 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">급여대장 타이틀</label>
            <input 
              type="text" 
              v-model="newLedger.title" 
              placeholder="예: 2026년 06월분 정기급여"
              class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
            />
          </div>

          <!-- 귀속기간 설정 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">귀속 시작일</label>
              <input 
                type="date" 
                v-model="newLedger.startDate" 
                class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
              />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">귀속 종료일</label>
              <input 
                type="date" 
                v-model="newLedger.endDate" 
                class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
              />
            </div>
          </div>

          <!-- 지급일자 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">지급일자 (지급예정일)</label>
            <input 
              type="date" 
              v-model="newLedger.payDate" 
              class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
            />
          </div>

          <div class="bg-indigo-50 dark:bg-indigo-950/20 p-3 rounded-lg border border-indigo-100 dark:border-indigo-900/60 leading-relaxed text-indigo-800 dark:text-indigo-300 text-[10px]">
            <strong>💡 실무 연동 팁:</strong> 급여대장 생성과 동시에 현재 재직 중인 모든 사원들의 은행 급여 지급계좌와 연봉계약 템플릿(기본 수당/공제)이 자동으로 복제 생성됩니다.
          </div>
        </div>

        <!-- 버튼 영역 -->
        <div class="flex items-center justify-end gap-2 border-t border-slate-100 dark:border-slate-800 pt-3 mt-5">
          <button @click="showCreateModal = false" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold border-none cursor-pointer">
            취소
          </button>
          <button @click="handleCreateLedger" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold border-none cursor-pointer shadow-sm">
            급여대장 작성 개시
          </button>
        </div>
      </div>
    </div>

  </div>
</template>
