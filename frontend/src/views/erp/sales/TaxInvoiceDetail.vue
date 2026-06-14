<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSalesStore, type TaxInvoice, type TaxInvoiceItem } from '@/stores/erp/useSalesStore'
import { 
  ArrowLeft, Save, Printer, Plus, Trash2, Receipt
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const salesStore = useSalesStore()
import { useHrStore } from '@/stores/erp/useHrStore'
const hrStore = useHrStore()
const statusCodes = ref<{ code: string; label: string }[]>([])
const purposeCodes = ref<{ code: string; label: string }[]>([])

const id = route.params.id as string
const isNew = id === 'new'

// 세금계산서 초기 상태
const invoice = ref<TaxInvoice>({
  writeDate: new Date().toISOString().split('T')[0],
  supplierRegNo: '120-88-12345',
  supplierSubNo: '',
  supplierName: '(주)앤티그래비티 코리아',
  supplierCeo: '홍길동',
  supplierAddress: '서울특별시 강남구 테헤란로 123, 7층',
  supplierBizType: '서비스, 도소매',
  supplierBizItem: '소프트웨어 개발, 정보처리',
  supplierEmail: 'admin@antigravity.co.kr',
  customerRegNo: '',
  customerSubNo: '',
  customerName: '',
  customerCeo: '',
  customerAddress: '',
  customerBizType: '',
  customerBizItem: '',
  customerEmail1: '',
  totalSupplyValue: 0,
  totalTaxValue: 0,
  totalAmount: 0,
  purpose: 'charge', // 'charge': 청구, 'receipt': 영수
  status: 'draft',
  items: []
})

onMounted(async () => {
  try {
    const codes = await hrStore.fetchCodes('TAX_INVOICE_STATUS')
    statusCodes.value = codes.map(c => ({ code: c.code, label: c.label }))
  } catch (e) {
    console.error('Failed to fetch tax invoice status codes:', e)
    statusCodes.value = [
      { code: 'draft', label: '작성 중 (Draft)' },
      { code: 'issued', label: '국세청 승인 발행 (Issued)' }
    ]
  }

  try {
    const codes = await hrStore.fetchCodes('TAX_INVOICE_PURPOSE')
    purposeCodes.value = codes.map(c => ({ code: c.code, label: c.label }))
  } catch (e) {
    console.error('Failed to fetch tax invoice purpose codes:', e)
    purposeCodes.value = [
      { code: 'charge', label: '청구 (대금 받기 전)' },
      { code: 'receipt', label: '영수 (대금 영수 완료)' }
    ]
  }

  if (!isNew) {
    try {
      const data = await salesStore.fetchTaxInvoice(id)
      if (data) {
        invoice.value = JSON.parse(JSON.stringify(data))
      }
    } catch (e) {
      window.alert('세금계산서 정보를 불러오는 도중 오류가 발생했습니다.')
      router.push('/hr/tax-invoices')
    }
  } else {
    addItemRow()
  }
})

// 품목 행 추가
function addItemRow() {
  const today = new Date()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const date = String(today.getDate()).padStart(2, '0')
  
  invoice.value.items.push({
    itemDate: `${month}-${date}`,
    itemName: '',
    spec: '',
    qty: 1,
    unitPrice: 0,
    supplyValue: 0,
    taxValue: 0,
    remarks: ''
  })
}

// 품목 행 삭제
function removeItemRow(index: number) {
  if (invoice.value.items.length <= 1) {
    window.alert('세금계산서에는 최소 1개 이상의 품목이 존재해야 합니다.')
    return
  }
  invoice.value.items.splice(index, 1)
}

// 개별 품목 금액 연산
function updateItemTotals(item: TaxInvoiceItem) {
  const qty = Number(item.qty || 0)
  const price = Number(item.unitPrice || 0)
  item.supplyValue = qty * price
  item.taxValue = Math.floor(item.supplyValue * 0.1)
}

// 실시간 총합 연산
const calcTotalSupply = computed(() => {
  return invoice.value.items.reduce((sum, item) => sum + Number(item.supplyValue || 0), 0)
})

const calcTotalTax = computed(() => {
  return invoice.value.items.reduce((sum, item) => sum + Number(item.taxValue || 0), 0)
})

const calcTotalAmount = computed(() => {
  return calcTotalSupply.value + calcTotalTax.value
})

// 국세청 자릿수 표기용 어레이 변환기 (공급가액 11칸, 세액 10칸 표준 격자 레이아웃 이식)
const supplyValueDigits = computed(() => {
  return getDigitsArray(calcTotalSupply.value, 11)
})

const taxValueDigits = computed(() => {
  return getDigitsArray(calcTotalTax.value, 10)
})

function getDigitsArray(value: number, length: number): string[] {
  const str = Math.floor(value).toString()
  const digits = new Array(length).fill('')
  
  const startIdx = length - str.length
  if (startIdx < 0) {
    // 자릿수 초과 시 뒤에서부터 채움
    for (let i = 0; i < length; i++) {
      digits[i] = str[str.length - length + i]
    }
  } else {
    for (let i = 0; i < str.length; i++) {
      digits[startIdx + i] = str[i]
    }
  }
  return digits
}

// 저장
async function handleSave() {
  if (!invoice.value.customerRegNo || !invoice.value.customerName) {
    window.alert('공급받는자 사업자등록번호와 상호를 반드시 입력해 주세요.')
    return
  }

  invoice.value.totalSupplyValue = calcTotalSupply.value
  invoice.value.totalTaxValue = calcTotalTax.value
  invoice.value.totalAmount = calcTotalAmount.value

  try {
    const saved = await salesStore.saveTaxInvoice(invoice.value)
    window.alert('세금계산서가 성공적으로 저장 및 국세청 승인 발행되었습니다.')
    router.push(`/hr/tax-invoices/${saved.id}`)
  } catch (e) {
    console.error('세금계산서 저장 실패:', e)
    window.alert('세금계산서 저장 도중 오류가 발생했습니다.')
  }
}

function handlePrint() {
  window.print()
}

function fmtMoney(val: number): string {
  return new Intl.NumberFormat('ko-KR').format(val)
}
</script>

<template>
  <div class="p-6 max-w-[1150px] mx-auto text-left print:p-0 print:max-w-full">
    <!-- ── 상단 액션 바 (인쇄 숨김) ── -->
    <div class="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800 mb-6 print:hidden">
      <div class="flex items-center gap-3">
        <div>
          <h1 class="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 tracking-tight">
            <Receipt class="w-7 h-7 text-indigo-650" />
            {{ isNew ? '신규 세금계산서 작성' : '세금계산서 상세' }}
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">대한민국 국세청 표준 전자세금계산서 적색 양식 및 실무 검증 규칙 탑재</p>
        </div>
      </div>
      
      <div class="flex items-center gap-2">
        <button 
          v-if="!isNew"
          @click="handlePrint" 
          class="flex items-center gap-1.5 px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold cursor-pointer border-none shadow-sm transition-all"
        >
          <Printer class="w-4 h-4" /> 계산서 인쇄 / PDF 저장
        </button>
        <button 
          @click="handleSave" 
          class="flex items-center gap-1.5 px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold cursor-pointer border-none shadow-md transition-all"
        >
          <Save class="w-4 h-4" /> 세금계산서 발행 (₩)
        </button>
        <button 
          @click="router.push('/hr/tax-invoices')" 
          class="flex items-center gap-1 px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-900 text-slate-600 dark:text-slate-300 text-xs font-bold rounded-lg cursor-pointer transition-colors shadow-sm"
        >
          <ArrowLeft class="w-4 h-4" />
          계산서 목록
        </button>
      </div>
    </div>

    <!-- ── 제어 패널 (인쇄 숨김) ── -->
    <div class="bg-slate-50 dark:bg-slate-900/30 p-5 rounded-2xl border border-slate-200/60 dark:border-slate-800/80 mb-6 print:hidden space-y-4">
      <h3 class="text-xs font-black text-slate-800 dark:text-slate-300 uppercase tracking-wide">
        ■ 세금계산서 설정 및 교부 구분
      </h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
        <div>
          <label class="block text-[11px] font-bold text-slate-500 mb-1.5">작성 일자 (세무 신고 기준)</label>
          <input 
            type="date" 
            v-model="invoice.writeDate" 
            class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
          />
        </div>
        <div>
          <label class="block text-[11px] font-bold text-slate-500 mb-1.5">영수 / 청구 선택</label>
          <div class="flex items-center gap-4 py-2">
            <label v-for="p in purposeCodes" :key="p.code" class="flex items-center gap-1.5 font-bold cursor-pointer">
              <input type="radio" :value="p.code" v-model="invoice.purpose" class="w-4 h-4 accent-indigo-650" /> {{ p.label }}
            </label>
          </div>
        </div>
        <div>
          <label class="block text-[11px] font-bold text-slate-500 mb-1.5">세금계산서 상태</label>
          <select 
            v-model="invoice.status"
            class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none"
          >
            <option v-for="sc in statusCodes" :key="sc.code" :value="sc.code">{{ sc.label }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- ── 국세청 표준 세금계산서 A4 종이 폼 (10년 차 고급 퍼블리싱 기법!) ── -->
    <div class="hometax-form bg-white text-rose-950 p-6 border-2 border-red-500 rounded-2xl shadow-xl print:border-none print:p-0 print:shadow-none print:rounded-none">
      
      <!-- 최상단 헤더 타이틀 및 승인 번호 -->
      <div class="flex justify-between items-center border-b border-red-500 pb-3 mb-4 font-sans text-xs">
        <div class="text-left">
          <span class="text-[10px] text-red-500 font-bold block">책 번호: 권 호</span>
          <span v-if="invoice.issueId" class="text-[10px] font-mono font-bold text-slate-900 block mt-1">국세청 승인번호: {{ invoice.issueId }}</span>
        </div>
        <h2 class="text-2xl font-black text-red-600 tracking-[0.4em] uppercase">세금계산서</h2>
        <div class="text-right text-[10px] text-red-500 font-bold">
          (공급자 보관용 / 적색)
        </div>
      </div>

      <!-- 인적사항 격자 레이아웃 (공급자 vs 공급받는자) -->
      <div class="grid grid-cols-2 border border-red-500 mb-4 font-sans text-[10px] text-left">
        
        <!-- 1) 공급자 섹션 (좌측) -->
        <div class="border-r border-red-550 flex">
          <div class="bg-red-50 text-red-650 font-bold text-center flex items-center justify-center w-8 border-r border-red-550 leading-tight">
            공<br/>급<br/>자
          </div>
          <div class="flex-1 divide-y divide-red-500">
            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">등록번호</div>
              <div class="flex-1 px-2 py-1.5 font-mono font-bold text-slate-900 flex items-center">
                <input type="text" v-model="invoice.supplierRegNo" class="w-full bg-transparent border-none outline-none p-0 font-bold text-slate-900 print:text-black" />
              </div>
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-12 border-l border-r border-red-500 flex items-center justify-center">종사업장</div>
              <div class="px-1 py-1.5 w-10 flex items-center">
                <input type="text" v-model="invoice.supplierSubNo" placeholder="0000" class="w-full bg-transparent border-none outline-none p-0 text-center font-mono text-slate-900 print:text-black" />
              </div>
            </div>
            
            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">상호<br/>(법인명)</div>
              <div class="flex-1 px-2 py-1.5 font-bold text-slate-900 flex items-center">
                <input type="text" v-model="invoice.supplierName" class="w-full bg-transparent border-none outline-none p-0 font-bold text-slate-900 print:text-black" />
              </div>
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-10 border-l border-r border-red-500 flex items-center justify-center">성명<br/>(대표)</div>
              <div class="px-2 py-1.5 w-20 flex items-center relative">
                <input type="text" v-model="invoice.supplierCeo" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
                <!-- 붉은 대표 도장 마크 -->
                <div class="absolute right-1 w-6 h-6 rounded-full border border-red-500/80 flex items-center justify-center text-[5px] text-red-500/80 leading-none rotate-12 pointer-events-none bg-white/20 select-none">
                  (인)
                </div>
              </div>
            </div>

            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">사업장<br/>주소</div>
              <div class="flex-1 px-2 py-1.5 flex items-center">
                <input type="text" v-model="invoice.supplierAddress" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
            </div>

            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">업태</div>
              <div class="w-28 px-2 py-1.5 flex items-center">
                <input type="text" v-model="invoice.supplierBizType" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-10 border-l border-r border-red-500 flex items-center justify-center">업종</div>
              <div class="flex-1 px-2 py-1.5 flex items-center">
                <input type="text" v-model="invoice.supplierBizItem" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
            </div>
          </div>
        </div>

        <!-- 2) 공급받는자 섹션 (우측) -->
        <div class="flex">
          <div class="bg-red-50 text-red-650 font-bold text-center flex items-center justify-center w-8 border-r border-red-550 leading-tight">
            공<br/>급<br/>받<br/>는<br/>자
          </div>
          <div class="flex-1 divide-y divide-red-500">
            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">등록번호</div>
              <div class="flex-1 px-2 py-1.5 font-mono font-bold text-slate-900 flex items-center">
                <input 
                  type="text" 
                  v-model="invoice.customerRegNo" 
                  placeholder="사업자번호 또는 주민번호"
                  class="w-full bg-transparent border-none outline-none p-0 font-bold text-slate-900 print:text-black" 
                />
              </div>
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-12 border-l border-r border-red-500 flex items-center justify-center">종사업장</div>
              <div class="px-1 py-1.5 w-10 flex items-center">
                <input type="text" v-model="invoice.customerSubNo" placeholder="0000" class="w-full bg-transparent border-none outline-none p-0 text-center font-mono text-slate-900 print:text-black" />
              </div>
            </div>

            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">상호<br/>(법인명)</div>
              <div class="flex-1 px-2 py-1.5 font-bold text-slate-900 flex items-center">
                <input 
                  type="text" 
                  v-model="invoice.customerName" 
                  placeholder="상호명 입력"
                  class="w-full bg-transparent border-none outline-none p-0 font-bold text-slate-900 print:text-black" 
                />
              </div>
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-10 border-l border-r border-red-500 flex items-center justify-center">성명<br/>(대표)</div>
              <div class="px-2 py-1.5 w-20 flex items-center">
                <input type="text" v-model="invoice.customerCeo" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
            </div>

            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">사업장<br/>주소</div>
              <div class="flex-1 px-2 py-1.5 flex items-center">
                <input type="text" v-model="invoice.customerAddress" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
            </div>

            <div class="flex">
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-16 border-r border-red-500 flex items-center justify-center">업태</div>
              <div class="w-28 px-2 py-1.5 flex items-center">
                <input type="text" v-model="invoice.customerBizType" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
              <div class="bg-red-50 text-red-650 font-bold text-center px-1 py-2 w-10 border-l border-r border-red-500 flex items-center justify-center">업종</div>
              <div class="flex-1 px-2 py-1.5 flex items-center">
                <input type="text" v-model="invoice.customerBizItem" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- 중간 요약 격자: 작성년월일, 공급가액 및 세액 자릿수 그리드 (홈택스 표준) -->
      <div class="border border-red-500 mb-4 font-sans text-[9px] text-left">
        <div class="flex divide-x divide-red-500 border-b border-red-500">
          <div class="bg-red-50 text-red-650 font-bold text-center py-2 w-28 flex items-center justify-center">작성 일자</div>
          
          <!-- 공급가액 자릿수 타이틀 -->
          <div class="flex-1 flex flex-col">
            <div class="bg-red-50 text-red-650 font-bold text-center py-1 border-b border-red-500">공 급 가 액</div>
            <div class="grid grid-cols-11 text-center font-bold text-[8px] bg-red-50/20 py-0.5 text-slate-650 divide-x divide-red-500">
              <div>백억</div><div>십억</div><div>억</div><div>천만</div><div>백만</div><div>십만</div><div>만</div><div>천</div><div>백</div><div>십</div><div>일</div>
            </div>
          </div>

          <!-- 세액 자릿수 타이틀 -->
          <div class="w-[38%] flex flex-col">
            <div class="bg-red-50 text-red-650 font-bold text-center py-1 border-b border-red-500">세 액 (VAT)</div>
            <div class="grid grid-cols-10 text-center font-bold text-[8px] bg-red-50/20 py-0.5 text-slate-650 divide-x divide-red-500">
              <div>십억</div><div>억</div><div>천만</div><div>백만</div><div>십만</div><div>만</div><div>천</div><div>백</div><div>십</div><div>일</div>
            </div>
          </div>
        </div>

        <!-- 실시간 자릿수 데이터 연동 로우 -->
        <div class="flex divide-x divide-red-500 font-mono font-bold text-center text-xs text-slate-900">
          <!-- 작성일자 -->
          <div class="w-28 py-2.5 flex items-center justify-center">
            <input 
              type="date" 
              v-model="invoice.writeDate" 
              class="w-full bg-transparent border-none outline-none p-0 text-center font-bold text-slate-900 print:text-black" 
            />
          </div>

          <!-- 공급가액 격자 입력 데이터 -->
          <div class="flex-1 grid grid-cols-11 divide-x divide-red-500 items-center">
            <div v-for="(d, i) in supplyValueDigits" :key="`s-${i}`" class="py-2">{{ d }}</div>
          </div>

          <!-- 세액 격자 입력 데이터 -->
          <div class="w-[38%] grid grid-cols-10 divide-x divide-red-500 items-center">
            <div v-for="(d, i) in taxValueDigits" :key="`t-${i}`" class="py-2 text-rose-600 print:text-black">{{ d }}</div>
          </div>
        </div>
      </div>

      <!-- 이메일 정보 (홈택스 표준) -->
      <div class="grid grid-cols-2 border border-red-500 mb-4 font-sans text-[10px] text-left">
        <div class="flex items-center">
          <div class="bg-red-50 text-red-650 font-bold text-center py-2 w-24 border-r border-red-500">공급자 이메일</div>
          <div class="flex-1 px-2">
            <input type="email" v-model="invoice.supplierEmail" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
          </div>
        </div>
        <div class="flex items-center">
          <div class="bg-red-50 text-red-650 font-bold text-center py-2 w-24 border-r border-red-500">공급받는자 이메일</div>
          <div class="flex-1 px-2">
            <input type="email" v-model="invoice.customerEmail1" placeholder="수신 메일 주소" class="w-full bg-transparent border-none outline-none p-0 text-slate-900 print:text-black" />
          </div>
        </div>
      </div>

      <!-- 세금계산서 품목 상세 그리드 테이블 -->
      <div class="border border-red-500 rounded-xl overflow-hidden mb-4 font-sans text-[10px] text-left">
        <table class="w-full border-collapse">
          <thead class="bg-red-50 text-red-650 border-b border-red-500 text-center">
            <tr>
              <th class="border-r border-red-500 py-1.5 font-bold w-10 print:hidden">삭제</th>
              <th class="border-r border-red-500 py-1.5 font-bold w-12">월일</th>
              <th class="border-r border-red-500 py-1.5 font-bold">품목 / 규격</th>
              <th class="border-r border-red-500 py-1.5 font-bold w-14">수량</th>
              <th class="border-r border-red-500 py-1.5 font-bold w-24 text-right">단가</th>
              <th class="border-r border-red-500 py-1.5 font-bold w-28 text-right">공급가액</th>
              <th class="border-r border-red-500 py-1.5 font-bold w-24 text-right">세액</th>
              <th class="py-1.5 font-bold w-28 text-left pl-2">비고</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-red-300">
            <tr v-for="(item, idx) in invoice.items" :key="idx" class="hover:bg-red-50/10">
              <!-- 삭제 (인쇄 시 숨김) -->
              <td class="border-r border-red-500 py-1.5 text-center print:hidden">
                <button 
                  @click="removeItemRow(idx)"
                  class="p-1 hover:bg-rose-100 rounded text-rose-600 transition-colors border-none bg-transparent cursor-pointer"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </td>
              <!-- 월일 -->
              <td class="border-r border-red-500 py-1.5 text-center">
                <input 
                  type="text" 
                  v-model="item.itemDate" 
                  placeholder="06-04"
                  class="w-full bg-transparent border-none outline-none p-0 text-center font-mono text-slate-800" 
                />
              </td>
              <!-- 품목 / 규격 -->
              <td class="border-r border-red-500 py-1.5 px-2 flex items-center gap-1">
                <input 
                  type="text" 
                  v-model="item.itemName" 
                  placeholder="품목명"
                  class="flex-1 bg-transparent border-none outline-none p-0 text-slate-900 font-bold" 
                />
                <input 
                  type="text" 
                  v-model="item.spec" 
                  placeholder="규격"
                  class="w-12 bg-transparent border-none outline-none p-0 text-slate-450 text-[9px]" 
                />
              </td>
              <!-- 수량 -->
              <td class="border-r border-red-500 py-1.5 text-center font-mono">
                <input 
                  type="number" 
                  v-model.number="item.qty" 
                  @input="updateItemTotals(item)"
                  class="w-full bg-transparent border-none outline-none p-0 text-center font-bold text-slate-900" 
                />
              </td>
              <!-- 단가 -->
              <td class="border-r border-red-500 py-1.5 px-2 text-right font-mono">
                <input 
                  type="number" 
                  v-model.number="item.unitPrice" 
                  @input="updateItemTotals(item)"
                  placeholder="0"
                  class="w-full bg-transparent border-none outline-none p-0 text-right font-bold text-slate-900" 
                />
              </td>
              <!-- 공급가액 -->
              <td class="border-r border-red-500 py-1.5 px-2 text-right font-mono text-slate-900 font-semibold">
                {{ fmtMoney(item.supplyValue) }}
              </td>
              <!-- 세액 -->
              <td class="border-r border-red-500 py-1.5 px-2 text-right font-mono text-rose-600">
                {{ fmtMoney(item.taxValue) }}
              </td>
              <!-- 비고 -->
              <td class="py-1.5 pl-2 pr-1">
                <input 
                  type="text" 
                  v-model="item.remarks" 
                  placeholder="비고"
                  class="w-full bg-transparent border-none outline-none p-0 text-slate-500" 
                />
              </td>
            </tr>

            <!-- 행 추가 (인쇄 숨김) -->
            <tr class="print:hidden">
              <td colspan="8" class="text-center py-1.5 bg-red-50/10 hover:bg-red-50/20 transition-colors">
                <button 
                  @click="addItemRow"
                  class="flex items-center justify-center gap-1.5 w-full py-0.5 text-red-500 hover:text-red-700 font-bold border-none bg-transparent cursor-pointer"
                >
                  <Plus class="w-4 h-4" /> 계산서 품목 추가 (행 추가)
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 하단 영수/청구 구분 및 합계금액 란 -->
      <div class="border border-red-500 font-sans text-[10px] text-left grid grid-cols-12 divide-x divide-red-500">
        <div class="col-span-3 flex flex-col justify-between">
          <span class="bg-red-50 text-red-650 font-bold text-center py-1 border-b border-red-500">합계금액</span>
          <span class="font-mono font-bold text-slate-900 text-center py-2.5 text-xs">
            ₩ {{ fmtMoney(calcTotalAmount) }}
          </span>
        </div>
        <div class="col-span-3 flex flex-col justify-between">
          <span class="bg-red-50 text-red-650 font-bold text-center py-1 border-b border-red-500">공급가액</span>
          <span class="font-mono font-bold text-slate-900 text-center py-2.5 text-xs">
            ₩ {{ fmtMoney(calcTotalSupply) }}
          </span>
        </div>
        <div class="col-span-3 flex flex-col justify-between">
          <span class="bg-red-50 text-red-650 font-bold text-center py-1 border-b border-red-500">세액 (부가세)</span>
          <span class="font-mono font-bold text-rose-600 text-center py-2.5 text-xs">
            ₩ {{ fmtMoney(calcTotalTax) }}
          </span>
        </div>
        <div class="col-span-3 flex flex-col justify-between">
          <span class="bg-red-50 text-red-650 font-bold text-center py-1 border-b border-red-500">영수 또는 청구</span>
          <span class="font-bold text-slate-950 text-center py-2.5 text-xs uppercase tracking-widest">
            이 금액을 {{ invoice.purpose === 'charge' ? '청구함' : '영수함' }}
          </span>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.hometax-form {
  max-width: 1100px;
  margin: 0 auto;
}

@media print {
  body {
    background-color: white !important;
    color: black !important;
  }
  .print\:hidden {
    display: none !important;
  }
  input, textarea, select {
    border: none !important;
    background: transparent !important;
    padding: 0 !important;
    box-shadow: none !important;
  }
  .p-6 {
    padding: 0 !important;
  }
  .hometax-form {
    border: none !important;
    padding: 0 !important;
    box-shadow: none !important;
  }
}
</style>
