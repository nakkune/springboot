<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSalesStore, type Quotation } from '@/stores/erp/useSalesStore'
import { 
  FileText, Plus, Loader2, Search, Trash2, Calendar, User, DollarSign, ChevronRight, ArrowLeft
} from 'lucide-vue-next'

const router = useRouter()
const salesStore = useSalesStore()

// 검색 및 필터 상태
const searchKeyword = ref('')
const selectedStatus = ref('')

onMounted(async () => {
  await salesStore.fetchQuotations()
})

// 필터된 견적서 목록
const filteredQuotations = computed(() => {
  return salesStore.quotations.filter(q => {
    const matchSearch = (q.title || '').includes(searchKeyword.value) || 
                        (q.customerName || '').includes(searchKeyword.value) || 
                        (q.quotationNo || '').includes(searchKeyword.value)
    const matchStatus = !selectedStatus.value || q.status === selectedStatus.value
    return matchSearch && matchStatus
  })
})

// 대시보드 합산 정보
const totals = computed(() => {
  let supply = 0
  let tax = 0
  let count = filteredQuotations.value.length

  filteredQuotations.value.forEach(q => {
    supply += q.totalSupplyValue || 0
    tax += q.totalTaxValue || 0
  })

  return {
    supply,
    tax,
    amount: supply + tax,
    count
  }
})

// 신규 견적서 생성 페이지로 이동
function goCreatePage() {
  router.push('/hr/quotations/new')
}

// 견적서 삭제 처리
async function handleDelete(id?: string) {
  if (!id) return
  if (!window.confirm('선택하신 견적서 및 모든 세부 품목 내역을 영구 삭제하시겠습니까?')) return
  
  try {
    await salesStore.deleteQuotation(id)
    window.alert('견적서가 성공적으로 삭제되었습니다.')
  } catch (e) {
    console.error('견적서 삭제 실패:', e)
    window.alert('견적서 삭제 도중 오류가 발생했습니다.')
  }
}

// 화폐 포맷팅
function fmtMoney(val: number): string {
  return new Intl.NumberFormat('ko-KR').format(val)
}

function statusBadge(status: string): string {
  switch (status) {
    case 'draft': return 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
    case 'sent': return 'bg-blue-100 text-blue-700 dark:bg-blue-950/40 dark:text-blue-400'
    case 'approved': return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400'
    case 'rejected': return 'bg-rose-100 text-rose-700 dark:bg-rose-950/30 dark:text-rose-400'
    default: return 'bg-slate-100 text-slate-600'
  }
}

function getStatusText(status: string): string {
  switch (status) {
    case 'draft': return '작성 중'
    case 'sent': return '발송 완료'
    case 'approved': return '계약 승인'
    case 'rejected': return '반려/취소'
    default: return status
  }
}
</script>

<template>
  <div class="p-6 max-w-[1600px] mx-auto text-left">
    <!-- 헤더 바 (양 끝 정렬 설계) -->
    <div class="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800 mb-6">
      <div class="flex items-center gap-3">
        <div>
          <h1 class="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 tracking-tight">
            <FileText class="w-7 h-7 text-emerald-600 dark:text-emerald-400" />
            견적서 관리
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">거래처 제공용 표준 견적서 작성 및 세무 공급금액 자동 연산 관리</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="goCreatePage"
          class="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer border-none shadow-md"
        >
          <Plus class="w-4 h-4" /> 신규 견적서 작성
        </button>
      </div>
    </div>

    <!-- 대시보드 요약 정보 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-4 shadow-sm">
        <p class="text-[10px] text-slate-500 font-semibold flex items-center gap-1">
          <Calendar class="w-3.5 h-3.5 text-slate-400" /> 등록된 총 견적수
        </p>
        <p class="text-xl font-black text-slate-900 dark:text-white mt-1.5">{{ totals.count }} 건</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-4 shadow-sm">
        <p class="text-[10px] text-slate-500 font-semibold flex items-center gap-1">
          <DollarSign class="w-3.5 h-3.5 text-slate-400" /> 견적 공급가액 합계
        </p>
        <p class="text-xl font-black text-slate-900 dark:text-white mt-1.5">₩ {{ fmtMoney(totals.supply) }}</p>
      </div>
      <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-4 shadow-sm">
        <p class="text-[10px] text-rose-500 font-semibold flex items-center gap-1">
          <DollarSign class="w-3.5 h-3.5 text-rose-450" /> 부가가치세 합계 (10%)
        </p>
        <p class="text-xl font-black text-rose-600 dark:text-rose-450 mt-1.5">₩ {{ fmtMoney(totals.tax) }}</p>
      </div>
      <div class="bg-emerald-50/40 dark:bg-emerald-950/20 rounded-xl border border-emerald-200 dark:border-emerald-900/60 p-4 shadow-sm">
        <p class="text-[10px] text-emerald-700 dark:text-emerald-400 font-bold flex items-center gap-1">
          <DollarSign class="w-3.5 h-3.5 text-emerald-550" /> 견적 총액 (합계금액)
        </p>
        <p class="text-xl font-black text-emerald-600 dark:text-emerald-400 mt-1.5">₩ {{ fmtMoney(totals.amount) }}</p>
      </div>
    </div>

    <!-- 필터링 및 검색 바 -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 mb-6 shadow-sm flex flex-col md:flex-row items-center gap-4 justify-between">
      <div class="flex items-center gap-2 w-full md:w-80">
        <div class="relative w-full">
          <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <Search class="w-4 h-4" />
          </span>
          <input 
            v-model="searchKeyword"
            type="text" 
            placeholder="견적 건명, 견적 번호, 거래처 검색..."
            class="w-full pl-9 pr-4 py-2 border border-slate-350 dark:border-slate-700 rounded-lg bg-transparent text-xs text-slate-900 dark:text-white focus:outline-none"
          />
        </div>
      </div>
      
      <div class="flex items-center gap-2 w-full md:w-auto">
        <select 
          v-model="selectedStatus"
          class="px-3 py-2 border border-slate-350 dark:border-slate-700 rounded-lg bg-transparent text-xs text-slate-700 dark:text-slate-300 focus:outline-none"
        >
          <option value="">전체 상태</option>
          <option value="draft">작성 중</option>
          <option value="sent">발송 완료</option>
          <option value="approved">계약 승인</option>
          <option value="rejected">반려/취소</option>
        </select>
      </div>
    </div>

    <!-- 로딩 스피너 -->
    <div v-if="salesStore.loading" class="flex justify-center items-center py-20">
      <Loader2 class="w-10 h-10 text-emerald-500 animate-spin" />
    </div>

    <!-- 데이터 리스트 테이블 -->
    <div v-else class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm">
      <div v-if="filteredQuotations.length === 0" class="flex flex-col items-center justify-center py-20 text-slate-400">
        <FileText class="w-16 h-16 text-slate-300 dark:text-slate-800 mb-3" />
        <p class="text-sm font-bold">등록된 견적서 내역이 존재하지 않습니다.</p>
        <p class="text-xs text-slate-400 mt-1">상단의 '신규 견적서 작성' 버튼을 눌러 첫 견적 내역을 구성해 보세요.</p>
      </div>

      <table v-else class="w-full text-xs">
        <thead class="bg-slate-50 dark:bg-slate-800/30 border-b border-slate-200 dark:border-slate-800">
          <tr>
            <th class="px-4 py-3 text-left font-bold text-slate-500 w-36">견적 번호</th>
            <th class="px-4 py-3 text-left font-bold text-slate-500">견적 건명</th>
            <th class="px-4 py-3 text-left font-bold text-slate-500">공급받는자 (거래처)</th>
            <th class="px-4 py-3 text-left font-bold text-slate-500 w-28">견적 일자</th>
            <th class="px-4 py-3 text-right font-bold text-slate-500 w-36">합계 금액 (세후)</th>
            <th class="px-4 py-3 text-center font-bold text-slate-500 w-24">상태</th>
            <th class="px-4 py-3 text-center font-bold text-slate-500 w-28">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          <tr 
            v-for="q in filteredQuotations" 
            :key="q.id"
            @click="router.push(`/hr/quotations/${q.id}`)"
            class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-all cursor-pointer group"
          >
            <td class="px-4 py-3.5 font-mono font-bold text-slate-900 dark:text-white">{{ q.quotationNo }}</td>
            <td class="px-4 py-3.5 font-semibold text-slate-900 dark:text-white group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">
              {{ q.title }}
            </td>
            <td class="px-4 py-3.5 text-slate-600 dark:text-slate-400 font-medium">
              <span class="flex items-center gap-1.5">
                <User class="w-3.5 h-3.5 text-slate-400" />
                {{ q.customerName }}
              </span>
            </td>
            <td class="px-4 py-3.5 font-mono text-slate-600 dark:text-slate-400">{{ q.quoteDate }}</td>
            <td class="px-4 py-3.5 text-right font-mono font-bold text-slate-900 dark:text-white">
              ₩ {{ fmtMoney(q.totalAmount) }}
            </td>
            <td class="px-4 py-3.5 text-center">
              <span class="px-2 py-0.5 rounded text-[10px] font-black" :class="statusBadge(q.status)">
                {{ getStatusText(q.status) }}
              </span>
            </td>
            <td class="px-4 py-3.5 text-center" @click.stop>
              <div class="flex items-center justify-center gap-1.5">
                <button
                  @click="router.push(`/hr/quotations/${q.id}`)"
                  class="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded font-bold transition-all border-none cursor-pointer flex items-center gap-0.5"
                >
                  상세 <ChevronRight class="w-3.5 h-3.5" />
                </button>
                <button
                  @click="handleDelete(q.id)"
                  class="p-1.5 bg-rose-50 hover:bg-rose-100 dark:bg-rose-955 dark:hover:bg-rose-900/40 text-rose-600 dark:text-rose-400 rounded transition-all border-none cursor-pointer"
                  title="삭제"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
