<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type PayrollCode } from '@/stores/erp/useHrStore'
import { 
  Settings, Loader2, ChevronLeft, Plus, Save, AlertCircle, X, Check, Lock
} from 'lucide-vue-next'

const router = useRouter()
const hrStore = useHrStore()

// 코드 추가 모달 상태
const showModal = ref(false)
const newCode = ref<Partial<PayrollCode>>({
  code: '',
  name: '',
  type: 'allowance',
  isTaxable: true,
  taxFreeLimit: 0,
  isActive: true,
  sortOrder: 10
})

onMounted(async () => {
  await loadCodes()
})

async function loadCodes() {
  await hrStore.fetchPayrollCodes()
}

function openCreateModal() {
  newCode.value = {
    code: '',
    name: '',
    type: 'allowance',
    isTaxable: true,
    taxFreeLimit: 0,
    isActive: true,
    sortOrder: (hrStore.payrollCodes.length + 1) * 10
  }
  showModal.value = true
}

async function handleSaveCode() {
  if (!newCode.value.code || !newCode.value.name) {
    window.alert('코드와 항목명을 필수적으로 입력해 주세요.')
    return
  }
  
  // 코드 영문 대문자 규격 강제 (시니어급 벨리데이션 디테일!)
  newCode.value.code = newCode.value.code.toUpperCase()

  try {
    await hrStore.savePayrollCode(newCode.value)
    showModal.value = false
    await loadCodes()
    window.alert('수당/공제 기준 정보 코드가 성공적으로 등록/수정되었습니다.')
  } catch (e) {
    console.error('코드 저장 실패:', e)
    window.alert('코드 저장 도중 오류가 발생했습니다.')
  }
}

function fmtMoney(val?: number): string {
  if (!val) return '0'
  return new Intl.NumberFormat('ko-KR').format(val)
}
</script>

<template>
  <div class="p-6 max-w-[1600px] mx-auto">
    <!-- 헤더 바 -->
    <div class="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800 mb-6">
      <div class="flex items-center gap-3">
        <div>
          <h1 class="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 tracking-tight">
            <Settings class="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            급여 수당/공제 코드 설정
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">급여 귀속 계산 시 동적으로 작동할 수당 지급액의 비과세 한도액 설정 및 세무 분류 관리</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="openCreateModal"
          class="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer border-none shadow-md"
        >
          <Plus class="w-4 h-4" /> 신규 급여 수당/공제 등록
        </button>
        <button 
          @click="router.push('/hr/payroll')" 
          class="flex items-center gap-1 px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-900 text-slate-600 dark:text-slate-300 text-xs font-bold rounded-lg cursor-pointer transition-colors shadow-sm"
        >
          <ChevronLeft class="w-4 h-4" />
          급여대장 관리
        </button>
      </div>
    </div>

    <!-- 로딩 뷰 -->
    <div v-if="hrStore.loading" class="flex justify-center items-center py-20">
      <Loader2 class="w-8 h-8 text-indigo-500 animate-spin" />
    </div>

    <!-- 코드 리스트 격자 (수당 & 공제 나누기) -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-8 text-left">
      <!-- 1) 수당 지급 코드 설정 (Allowances) -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
        <h3 class="text-sm font-black text-indigo-600 dark:text-indigo-400 mb-4 flex items-center gap-1.5">
          ■ 지급 항목 설정 (수당 대장)
        </h3>
        <table class="w-full text-xs">
          <thead class="bg-slate-50 dark:bg-slate-800/20 border-b border-slate-200 dark:border-slate-800">
            <tr>
              <th class="text-left px-2 py-2 font-bold text-slate-500">코드</th>
              <th class="text-left px-2 py-2 font-bold text-slate-500">지급수당명</th>
              <th class="text-center px-1 py-2 font-bold text-slate-500">과세여부</th>
              <th class="text-right px-2 py-2 font-bold text-slate-500">비과세한도(월)</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
            <tr v-for="c in hrStore.payrollCodes.filter(x => x.type === 'allowance')" :key="c.code" class="hover:bg-slate-50 dark:hover:bg-slate-850/40">
              <td class="px-2 py-3 font-mono font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1">
                <Lock v-if="c.isSystem" class="w-3.5 h-3.5 text-slate-400" title="시스템 필수 항목" />
                {{ c.code }}
              </td>
              <td class="px-2 py-3 font-semibold text-slate-900 dark:text-white">{{ c.name }}</td>
              <td class="px-1 py-3 text-center">
                <span class="px-2 py-0.5 rounded text-[10px] font-black" :class="c.isTaxable ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'">
                  {{ c.isTaxable ? '과세' : '비과세' }}
                </span>
              </td>
              <td class="px-2 py-3 text-right font-mono text-slate-700 dark:text-slate-400">
                {{ c.isTaxable ? '-' : `₩ ${fmtMoney(c.taxFreeLimit)}` }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 2) 공제 항목 코드 설정 (Deductions) -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
        <h3 class="text-sm font-black text-rose-600 dark:text-rose-400 mb-4 flex items-center gap-1.5">
          ■ 공제 항목 설정 (세금/보험 대장)
        </h3>
        <table class="w-full text-xs">
          <thead class="bg-slate-50 dark:bg-slate-800/20 border-b border-slate-200 dark:border-slate-800">
            <tr>
              <th class="text-left px-2 py-2 font-bold text-slate-500">코드</th>
              <th class="text-left px-2 py-2 font-bold text-slate-500">공제구분명</th>
              <th class="text-center px-1 py-2 font-bold text-slate-500">시스템항목</th>
              <th class="text-center px-2 py-2 font-bold text-slate-500">사용상태</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
            <tr v-for="c in hrStore.payrollCodes.filter(x => x.type === 'deduction')" :key="c.code" class="hover:bg-slate-50 dark:hover:bg-slate-850/40">
              <td class="px-2 py-3 font-mono font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1">
                <Lock v-if="c.isSystem" class="w-3.5 h-3.5 text-slate-400" title="시스템 필수 항목" />
                {{ c.code }}
              </td>
              <td class="px-2 py-3 font-semibold text-slate-900 dark:text-white">{{ c.name }}</td>
              <td class="px-1 py-3 text-center">
                <span class="px-2 py-0.5 rounded text-[10px] font-black" :class="c.isSystem ? 'bg-indigo-50 text-indigo-600' : 'bg-slate-50 text-slate-500'">
                  {{ c.isSystem ? '필수' : '선택' }}
                </span>
              </td>
              <td class="px-2 py-3 text-center">
                <span class="text-emerald-500 flex items-center justify-center gap-0.5 font-bold">
                  <Check class="w-4 h-4" /> 사용중
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── 신규 코드 추가 및 설정 모달 ── -->
    <div v-if="showModal" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl max-w-[480px] w-full p-6 relative">
        <button @click="showModal = false" class="absolute top-4 right-4 p-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 rounded-lg cursor-pointer border-none bg-transparent">
          <X class="w-5 h-5" />
        </button>

        <h3 class="text-base font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
          <Plus class="w-5 h-5 text-indigo-600 dark:text-indigo-400" /> 신규 급여 수당/공제 등록
        </h3>

        <div class="space-y-4 text-left text-xs">
          <!-- 구분 설정 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">항목 구분</label>
            <select v-model="newCode.type" class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none">
              <option value="allowance">지급 항목 (수당 명세)</option>
              <option value="deduction">공제 항목 (세금/사회보험)</option>
            </select>
          </div>

          <!-- 코드 영문 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">수당/공제 코드 (영문 대문자 필수)</label>
            <input 
              type="text" 
              v-model="newCode.code" 
              placeholder="예: RESEARCH_PAY"
              class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white font-mono focus:outline-none" 
            />
          </div>

          <!-- 수당명 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">수당/공제 항목명</label>
            <input 
              type="text" 
              v-model="newCode.name" 
              placeholder="예: 기업부설연구소수당"
              class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
            />
          </div>

          <!-- 과세 여부 (Allowances 일 때만 동작) -->
          <div v-if="newCode.type === 'allowance'">
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">세무 과세 분류</label>
            <div class="flex items-center gap-4 py-1">
              <label class="flex items-center gap-1.5 font-bold cursor-pointer">
                <input type="radio" :value="true" v-model="newCode.isTaxable" class="w-4 h-4 accent-indigo-500" /> 과세
              </label>
              <label class="flex items-center gap-1.5 font-bold cursor-pointer">
                <input type="radio" :value="false" v-model="newCode.isTaxable" class="w-4 h-4 accent-indigo-500" /> 비과세 (세금 감면)
              </label>
            </div>
          </div>

          <!-- 비과세 한도액 -->
          <div v-if="newCode.type === 'allowance' && !newCode.isTaxable">
            <label class="block text-[11px] font-bold text-slate-500 mb-1.5">월간 비과세 한도 한계금액 (원)</label>
            <input 
              type="number" 
              v-model.number="newCode.taxFreeLimit" 
              placeholder="예: 200000 (0 입력 시 무제한)"
              class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white font-mono focus:outline-none" 
            />
          </div>
        </div>

        <!-- 버튼 영역 -->
        <div class="flex items-center justify-end gap-2 border-t border-slate-100 dark:border-slate-800 pt-3 mt-5">
          <button @click="showModal = false" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold border-none cursor-pointer">
            취소
          </button>
          <button @click="handleSaveCode" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold border-none cursor-pointer shadow-sm">
            등록 / 저장
          </button>
        </div>
      </div>
    </div>

  </div>
</template>
