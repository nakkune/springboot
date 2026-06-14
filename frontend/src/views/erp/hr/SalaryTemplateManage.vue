<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHrStore, type SalaryTemplate } from '@/stores/erp/useHrStore'
import { 
  UserCheck, Loader2, ChevronLeft, Save, Building, User, Settings, AlertCircle, Edit
} from 'lucide-vue-next'

const router = useRouter()
const hrStore = useHrStore()

// 에디터 상태
const activeTemplate = ref<SalaryTemplate | null>(null)

onMounted(async () => {
  await loadTemplates()
})

async function loadTemplates() {
  await hrStore.fetchSalaryTemplates()
  if (hrStore.salaryTemplates.length > 0) {
    selectTemplate(hrStore.salaryTemplates[0])
  }
}

function selectTemplate(t: SalaryTemplate) {
  activeTemplate.value = JSON.parse(JSON.stringify(t))
}

async function handleSaveTemplate() {
  if (!activeTemplate.value) return
  
  try {
    await hrStore.saveSalaryTemplate(activeTemplate.value)
    await loadTemplates()
    window.alert('해당 임직원의 인사 기본 계약 급여 템플릿이 성공적으로 저장 및 마이그레이션되었습니다.')
  } catch (e) {
    console.error('템플릿 저장 실패:', e)
    window.alert('저장 도중 오류가 발생했습니다.')
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
            <UserCheck class="w-7 h-7 text-emerald-600 dark:text-emerald-400" />
            기본계약 급여 템플릿 설정
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">임직원 입사 계약 조건에 따른 고정 기본급 및 사회보험 가입 상태 마스터 설정</p>
        </div>
      </div>
      <button 
        @click="router.push('/hr/payroll')" 
        class="flex items-center gap-1 px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-900 text-slate-600 dark:text-slate-300 text-xs font-bold rounded-lg cursor-pointer transition-colors shadow-sm"
      >
        <ChevronLeft class="w-4 h-4" />
        급여대장 관리
      </button>
    </div>

    <!-- 2단 배치 레이아웃 -->
    <div class="flex flex-col lg:flex-row gap-6 items-stretch">
      
      <!-- 1) 좌측: 사원 템플릿 리스트 (width 60%) -->
      <div class="w-full lg:w-[60%] bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm flex flex-col justify-between text-left">
        <div>
          <div class="bg-slate-50 dark:bg-slate-800/40 px-4 py-3 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <span class="text-xs font-bold text-slate-700 dark:text-slate-300">연봉 계약 임직원 명단</span>
            <span class="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold">클릭 시 템플릿 에디터 활성화</span>
          </div>

          <div v-if="hrStore.loading" class="flex justify-center items-center py-20">
            <Loader2 class="w-8 h-8 text-emerald-500 animate-spin" />
          </div>
          <table v-else class="w-full text-xs">
            <thead class="bg-slate-50/50 dark:bg-slate-800/10 border-b border-slate-200 dark:border-slate-800">
              <tr>
                <th class="text-left px-3 py-2.5 font-bold text-slate-500">사번/성명</th>
                <th class="text-left px-3 py-2.5 font-bold text-slate-500">부서</th>
                <th class="text-right px-3 py-2.5 font-bold text-slate-500">계약 기본급</th>
                <th class="text-right px-3 py-2.5 font-bold text-slate-500">고정 비과세 식대</th>
                <th class="text-center px-2 py-2.5 font-bold text-slate-500">4대보험 가입</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
              <tr 
                v-for="t in hrStore.salaryTemplates" 
                :key="t.employeeId"
                @click="selectTemplate(t)"
                class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-all cursor-pointer"
                :class="activeTemplate?.employeeId === t.employeeId ? 'bg-emerald-50/40 dark:bg-emerald-950/20 font-bold border-l-4 border-emerald-600 dark:border-emerald-400' : ''"
              >
                <td class="px-3 py-3 text-slate-900 dark:text-white flex items-center gap-1.5">
                  <User class="w-3.5 h-3.5 text-slate-400" />
                  <div>
                    <p class="font-bold">{{ t.employeeName }}</p>
                    <span class="text-[9px] text-slate-400 font-mono">{{ t.employeeNo }}</span>
                  </div>
                </td>
                <td class="px-3 py-3 text-slate-600 dark:text-slate-400 font-medium">{{ t.departmentName || '-' }}</td>
                <td class="px-3 py-3 text-right text-slate-900 dark:text-white font-mono font-bold">₩ {{ fmtMoney(t.basePay) }}</td>
                <td class="px-3 py-3 text-right text-slate-600 dark:text-slate-400 font-mono">₩ {{ fmtMoney(t.mealAllowance) }}</td>
                <td class="px-2 py-3 text-center">
                  <span class="inline-flex gap-1 text-[9px] font-bold text-emerald-600 dark:text-emerald-400">
                    <span v-if="t.useNationalPension" title="국민">국</span>
                    <span v-if="t.useHealthInsurance" title="건강">건</span>
                    <span v-if="t.useEmploymentInsurance" title="고용">고</span>
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="px-3 py-2 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/30 text-xxs text-slate-400 font-medium">
          ※ 재직 중인 사원의 연봉 조건이 정상 조회되고 있습니다.
        </div>
      </div>

      <!-- 2) 우측: 계약 에디터 (width 40%) -->
      <div class="w-full lg:w-[40%] bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm flex flex-col justify-between text-left">
        <div v-if="activeTemplate" class="space-y-4">
          <div class="border-b border-slate-200 dark:border-slate-800 pb-3 mb-4">
            <h3 class="text-sm font-black text-slate-950 dark:text-white flex items-center gap-1.5">
              <Edit class="w-4 h-4 text-emerald-500" /> [{{ activeTemplate.employeeName }}] 연봉계약 조건 조정
            </h3>
            <span class="text-[10px] text-slate-400 font-semibold mt-1 block">소속: {{ activeTemplate.departmentName || '-' }} | 사번: {{ activeTemplate.employeeNo }}</span>
          </div>

          <!-- 계약 기본급 입력 -->
          <div class="space-y-3.5 text-xs">
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">연봉계약 기본급 (월, 원)</label>
              <input 
                type="number" 
                v-model.number="activeTemplate.basePay" 
                class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white font-bold font-mono focus:outline-none" 
              />
            </div>

            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">고정 직책수당 (월, 원)</label>
              <input 
                type="number" 
                v-model.number="activeTemplate.positionPay" 
                class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white font-bold font-mono focus:outline-none" 
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-[11px] font-bold text-slate-500 mb-1.5">고정 비과세 식대 (월, 원)</label>
                <input 
                  type="number" 
                  v-model.number="activeTemplate.mealAllowance" 
                  class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white font-mono focus:outline-none" 
                />
              </div>
              <div>
                <label class="block text-[11px] font-bold text-slate-500 mb-1.5">고정 차량유지보조금 (월, 원)</label>
                <input 
                  type="number" 
                  v-model.number="activeTemplate.carAllowance" 
                  class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white font-mono focus:outline-none" 
                />
              </div>
            </div>

            <!-- 사회보험 가입 여부 스위치 -->
            <div class="bg-slate-50 dark:bg-slate-800/40 p-4 rounded-xl border border-slate-200/50 dark:border-slate-800/60 space-y-3">
              <h4 class="text-[10px] font-black text-slate-600 dark:text-slate-400 uppercase tracking-wider">법정 4대보험 공제 여부</h4>
              <div class="flex items-center justify-between">
                <span class="font-semibold">국민연금 납부</span>
                <input type="checkbox" v-model="activeTemplate.useNationalPension" class="w-4 h-4 accent-emerald-500" />
              </div>
              <div class="flex items-center justify-between">
                <span class="font-semibold">건강보험 (요양포함) 납부</span>
                <input type="checkbox" v-model="activeTemplate.useHealthInsurance" class="w-4 h-4 accent-emerald-500" />
              </div>
              <div class="flex items-center justify-between">
                <span class="font-semibold">고용보험 납부</span>
                <input type="checkbox" v-model="activeTemplate.useEmploymentInsurance" class="w-4 h-4 accent-emerald-500" />
              </div>
            </div>

            <!-- 소득세율 징수 비율 셀렉터 -->
            <div>
              <label class="block text-[11px] font-bold text-slate-500 mb-1.5">간이소득세 원천징수 징수 비율 선택</label>
              <select v-model.number="activeTemplate.incomeTaxRate" class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none">
                <option :value="80">국세청 기준세율의 80% (매달 적게 떼고 연말정산 시 추가 납부)</option>
                <option :value="100">국세청 기준세율의 100% (대한민국 기본 표준 요율)</option>
                <option :value="120">국세청 기준세율의 120% (매달 넉넉히 떼고 연말정산 시 환급 극대화)</option>
              </select>
            </div>
          </div>

          <div class="flex items-center justify-end pt-4 border-t border-slate-100 dark:border-slate-800 mt-5">
            <button 
              @click="handleSaveTemplate" 
              class="flex items-center gap-1.5 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold border-none cursor-pointer shadow-md transition-all"
            >
              <Save class="w-4 h-4" /> 템플릿 변경사항 저장 (₩)
            </button>
          </div>
        </div>

        <div v-else class="flex flex-col items-center justify-center py-40 text-slate-400">
          <AlertCircle class="w-12 h-12 text-slate-300 dark:text-slate-800 mb-3" />
          <p class="text-xs font-bold">임직원 명단에서 연봉 계약을 조율할 사원을 선택해 주세요.</p>
        </div>
      </div>

    </div>
  </div>
</template>
