<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSalesStore, type Quotation, type QuotationItem } from '@/stores/erp/useSalesStore'
import { 
  ArrowLeft, Save, Printer, Plus, Trash2, HelpCircle, FileText
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const salesStore = useSalesStore()
import { useHrStore } from '@/stores/erp/useHrStore'
const hrStore = useHrStore()
const statusCodes = ref<{ code: string; label: string }[]>([])

const id = route.params.id as string
const isNew = id === 'new'

// 견적서 데이터 초기화 모델
const quote = ref<Quotation>({
  title: '',
  quoteDate: new Date().toISOString().split('T')[0],
  validDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30일 유효
  supplierRegNo: '120-88-12345',
  supplierName: '(주)앤티그래비티 코리아',
  supplierCeo: '홍길동',
  supplierAddress: '서울특별시 강남구 테헤란로 123, 7층',
  supplierBizType: '서비스, 도소매',
  supplierBizItem: '소프트웨어 개발, 정보처리',
  customerName: '',
  customerCeo: '',
  totalSupplyValue: 0,
  totalTaxValue: 0,
  totalAmount: 0,
  remarks: '1. 공급 조건: 납품일 기준 7일 이내 현금 결제\n2. 부가가치세 별도(혹은 포함) 금액입니다.',
  status: 'draft',
  items: []
})

onMounted(async () => {
  try {
    const codes = await hrStore.fetchCodes('QUOTATION_STATUS')
    statusCodes.value = codes.map(c => ({ code: c.code, label: c.label }))
  } catch (e) {
    console.error('Failed to fetch quotation status codes:', e)
    statusCodes.value = [
      { code: 'draft', label: '작성 중 (Draft)' },
      { code: 'sent', label: '발송 완료 (Sent)' },
      { code: 'approved', label: '계약 승인 (Approved)' },
      { code: 'rejected', label: '반려/취소 (Rejected)' }
    ]
  }

  if (!isNew) {
    try {
      const data = await salesStore.fetchQuotation(id)
      if (data) {
        quote.value = JSON.parse(JSON.stringify(data))
      }
    } catch (e) {
      window.alert('견적서 정보를 불러오는 도중 오류가 발생했습니다.')
      router.push('/hr/quotations')
    }
  } else {
    // 신규 작성 시 기본 품목 행 하나 추가
    addItemRow()
  }
})

// 품목 행 추가
function addItemRow() {
  quote.value.items.push({
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
  if (quote.value.items.length <= 1) {
    window.alert('견적서에는 최소 1개 이상의 품목이 존재해야 합니다.')
    return
  }
  quote.value.items.splice(index, 1)
}

// 개별 품목 공급가 및 세액 연산 도우미
function updateItemTotals(item: QuotationItem) {
  const qty = Number(item.qty || 0)
  const price = Number(item.unitPrice || 0)
  item.supplyValue = qty * price
  item.taxValue = Math.floor(item.supplyValue * 0.1) // 원단위 절사
}

// 실시간 총 공급가액
const calcTotalSupply = computed(() => {
  return quote.value.items.reduce((sum, item) => sum + Number(item.supplyValue || 0), 0)
})

// 실시간 총 세액 (부가세)
const calcTotalTax = computed(() => {
  return quote.value.items.reduce((sum, item) => sum + Number(item.taxValue || 0), 0)
})

// 실시간 합계 금액
const calcTotalAmount = computed(() => {
  return calcTotalSupply.value + calcTotalTax.value
})

// 한국 실무용: 총합계 금액을 한글 문자열로 변환 (일금 ~ 원정)
const totalAmountKorean = computed(() => {
  return numToKorean(calcTotalAmount.value)
})

// 숫자 -> 한글 변환 알고리즘 (시니어급 정밀도 설계)
function numToKorean(num: number): string {
  if (num <= 0) return '영'
  const units = ['', '만', '억', '조']
  const nums = ['', '일', '이', '삼', '사', '오', '육', '칠', '팔', '구']
  const smallUnits = ['', '십', '백', '천']
  
  let result = ''
  let numStr = Math.floor(num).toString()
  let len = numStr.length
  
  for (let i = 0; i < len; i += 4) {
    let part = numStr.slice(Math.max(0, len - i - 4), len - i)
    let partResult = ''
    let partLen = part.length
    
    for (let j = 0; j < partLen; j++) {
      let digit = Number(part[partLen - j - 1])
      if (digit > 0) {
        partResult = nums[digit] + smallUnits[j] + partResult
      }
    }
    
    if (partResult !== '') {
      result = partResult + units[i / 4] + result
    }
  }
  
  // 1십 -> 십, 1백 -> 백 등으로 국문 규격 보정
  result = result.replace(/일십/g, '십')
                 .replace(/일백/g, '백')
                 .replace(/일천/g, '천')
  
  return result
}

// 저장 기능 바인딩
async function handleSave() {
  if (!quote.value.title || !quote.value.customerName) {
    window.alert('견적 건명과 공급받는자(거래처) 상호를 반드시 입력해 주세요.')
    return
  }

  // 데이터 합계 최종 동기화
  quote.value.totalSupplyValue = calcTotalSupply.value
  quote.value.totalTaxValue = calcTotalTax.value
  quote.value.totalAmount = calcTotalAmount.value

  try {
    const saved = await salesStore.saveQuotation(quote.value)
    window.alert('견적서가 성공적으로 저장되었습니다.')
    router.push(`/hr/quotations/${saved.id}`)
  } catch (e) {
    console.error('저장 실패:', e)
    window.alert('저장 도중 오류가 발생했습니다.')
  }
}

// 브라우저 기본 인쇄 다이얼로그 호출
function handlePrint() {
  window.print()
}

// 화폐 단원 콤마 포맷
function fmtMoney(val: number): string {
  return new Intl.NumberFormat('ko-KR').format(val)
}
</script>

<template>
  <div class="p-6 max-w-[1100px] mx-auto text-left print:p-0 print:max-w-full">
    <!-- ── 상단 액션바 (인쇄 시 숨김) ── -->
    <div class="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800 mb-6 print:hidden">
      <div class="flex items-center gap-3">
        <div>
          <h1 class="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 tracking-tight">
            <FileText class="w-7 h-7 text-emerald-600" />
            {{ isNew ? '신규 견적서 작성' : '견적서 상세 및 조율' }}
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">대한민국 원화 요율이 완벽하게 연동되는 대화형 견적서 에디터</p>
        </div>
      </div>
      
      <div class="flex items-center gap-2">
        <button 
          v-if="!isNew"
          @click="handlePrint" 
          class="flex items-center gap-1.5 px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-bold cursor-pointer border-none shadow-sm transition-all"
        >
          <Printer class="w-4 h-4" /> 견적서 인쇄 / PDF 저장
        </button>
        <button 
          @click="handleSave" 
          class="flex items-center gap-1.5 px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold cursor-pointer border-none shadow-md transition-all"
        >
          <Save class="w-4 h-4" /> 견적서 저장 (₩)
        </button>
        <button 
          @click="router.push('/hr/quotations')" 
          class="flex items-center gap-1 px-3.5 py-2 bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-900 text-slate-600 dark:text-slate-300 text-xs font-bold rounded-lg cursor-pointer transition-colors shadow-sm"
        >
          <ArrowLeft class="w-4 h-4" />
          견적서 목록
        </button>
      </div>
    </div>

    <!-- ── 편집 및 제어 영역 (인쇄 시 숨김) ── -->
    <div class="bg-slate-50 dark:bg-slate-900/30 p-5 rounded-2xl border border-slate-200/60 dark:border-slate-800/80 mb-6 print:hidden space-y-4">
      <h3 class="text-xs font-black text-slate-800 dark:text-slate-300 uppercase tracking-wide flex items-center gap-1.5">
        ■ 견적서 기본 설정 및 문서 관리
      </h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
        <div>
          <label class="block text-[11px] font-bold text-slate-500 mb-1.5">견적 건명 (제목)</label>
          <input 
            type="text" 
            v-model="quote.title" 
            placeholder="예: 2026년도 하반기 시스템 유지보수 견적의 건"
            class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
          />
        </div>
        <div>
          <label class="block text-[11px] font-bold text-slate-500 mb-1.5">견적 일자</label>
          <input 
            type="date" 
            v-model="quote.quoteDate" 
            class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none" 
          />
        </div>
        <div>
          <label class="block text-[11px] font-bold text-slate-500 mb-1.5">견적서 상태 관리</label>
          <select 
            v-model="quote.status"
            class="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-transparent text-slate-900 dark:text-white focus:outline-none"
          >
            <option v-for="sc in statusCodes" :key="sc.code" :value="sc.code">{{ sc.label }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- ── 실제 견적서 A4 규격 출력용 종이 영역 ── -->
    <div class="bg-white text-slate-950 p-10 border border-slate-300 shadow-xl rounded-2xl font-serif print:border-none print:p-0 print:shadow-none print:rounded-none select-text">
      
      <!-- 견적서 제목 -->
      <div class="text-center py-4 border-b-4 border-slate-900 mb-8">
        <h1 class="text-3xl font-bold tracking-[1em] text-slate-950 pl-[1em]">견적서</h1>
        <p v-if="quote.quotationNo" class="text-[10px] font-mono text-slate-500 tracking-wider mt-2">일련번호: {{ quote.quotationNo }}</p>
      </div>

      <!-- 공급자 & 공급받는자 인적사항 (2단 배치) -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8 text-xs font-sans">
        
        <!-- 좌측: 공급받는자 (수신자) -->
        <div class="space-y-4 flex flex-col justify-end">
          <div class="border-b-2 border-slate-900 pb-2">
            <span class="text-[10px] font-bold text-slate-500">공급받는자 (수신)</span>
          </div>
          <div class="flex items-baseline gap-2">
            <input 
              type="text" 
              placeholder="거래처 상호명을 입력해 주세요"
              v-model="quote.customerName" 
              class="text-lg font-black bg-transparent border-none border-b border-slate-200 outline-none p-0 w-64 print:border-none" 
            />
            <span class="text-xs font-bold text-slate-800">귀하</span>
          </div>
          <div class="space-y-1.5 text-slate-700">
            <div class="flex items-center gap-2">
              <span class="w-16 text-slate-400">참조 (대표):</span>
              <input 
                type="text" 
                v-model="quote.customerCeo" 
                placeholder="대표자 성명" 
                class="bg-transparent border-none outline-none p-0 w-32 border-b border-slate-200 print:border-none text-slate-800" 
              />
            </div>
            <p class="text-slate-500">아래와 같이 견적서를 제출합니다.</p>
            <div class="flex items-center gap-2">
              <span class="w-16 text-slate-400">견적일자:</span>
              <span class="font-bold text-slate-900 font-mono">{{ quote.quoteDate }}</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="w-16 text-slate-400">유효기간:</span>
              <input 
                type="date" 
                v-model="quote.validDate" 
                class="bg-transparent border-none outline-none p-0 w-32 font-mono text-slate-800 print:hidden" 
              />
              <span class="font-bold text-slate-900 font-mono hidden print:inline">{{ quote.validDate }} 까지</span>
            </div>
          </div>
        </div>

        <!-- 우측: 공급자 (발송자) -->
        <div class="relative">
          <div class="border-b-2 border-slate-900 pb-2 mb-2 text-right">
            <span class="text-[10px] font-bold text-slate-500">공급자 (발신)</span>
          </div>
          <table class="w-full border-collapse border border-slate-900 text-left text-[10px]">
            <tbody>
              <tr>
                <th class="border border-slate-900 bg-slate-50 px-2 py-1.5 font-bold text-center w-16">등록번호</th>
                <td colspan="3" class="border border-slate-900 px-2 py-1.5 font-mono font-bold">
                  <input type="text" v-model="quote.supplierRegNo" class="w-full bg-transparent border-none outline-none p-0 font-bold print:border-none text-slate-900" />
                </td>
              </tr>
              <tr>
                <th class="border border-slate-900 bg-slate-50 px-2 py-1.5 font-bold text-center">상 호</th>
                <td class="border border-slate-900 px-2 py-1.5 font-bold">
                  <input type="text" v-model="quote.supplierName" class="w-full bg-transparent border-none outline-none p-0 font-bold print:border-none text-slate-900" />
                </td>
                <th class="border border-slate-900 bg-slate-50 px-2 py-1.5 font-bold text-center w-12">대 표</th>
                <td class="border border-slate-900 px-2 py-1.5">
                  <input type="text" v-model="quote.supplierCeo" class="w-full bg-transparent border-none outline-none p-0 print:border-none text-slate-900" />
                </td>
              </tr>
              <tr>
                <th class="border border-slate-900 bg-slate-50 px-2 py-1.5 font-bold text-center">주 소</th>
                <td colspan="3" class="border border-slate-900 px-2 py-1.5">
                  <input type="text" v-model="quote.supplierAddress" class="w-full bg-transparent border-none outline-none p-0 print:border-none text-slate-900" />
                </td>
              </tr>
              <tr>
                <th class="border border-slate-900 bg-slate-50 px-2 py-1.5 font-bold text-center">업 태</th>
                <td class="border border-slate-900 px-2 py-1.5">
                  <input type="text" v-model="quote.supplierBizType" class="w-full bg-transparent border-none outline-none p-0 print:border-none text-slate-900" />
                </td>
                <th class="border border-slate-900 bg-slate-50 px-2 py-1.5 font-bold text-center">업 종</th>
                <td class="border border-slate-900 px-2 py-1.5">
                  <input type="text" v-model="quote.supplierBizItem" class="w-full bg-transparent border-none outline-none p-0 print:border-none text-slate-900" />
                </td>
              </tr>
            </tbody>
          </table>

          <!-- ── 시니어급 UI/UX: 대표이사 SVG 인감도장 마크 (마치 인쇄한 듯한 고급감!) ── -->
          <div class="absolute right-6 bottom-4 w-12 h-12 rounded-full border-2 border-red-500/80 flex items-center justify-center font-bold text-[8px] text-red-500/80 leading-none rotate-12 select-none pointer-events-none z-10 bg-white/10 backdrop-blur-[0.5px]">
            <div class="text-center font-serif">
              앤티<br/>대표<br/>이사
            </div>
          </div>
        </div>
      </div>

      <!-- 합계금액 (일금 ~ 원정) 국문 바 -->
      <div class="border-2 border-slate-900 bg-slate-50 p-3 rounded-lg flex justify-between items-center mb-6 font-sans text-xs">
        <span class="font-extrabold text-slate-800">■ 합계금액 (공급가액 + 부가세)</span>
        <div class="flex items-center gap-4">
          <span class="font-black text-slate-950 tracking-wider">일금 {{ totalAmountKorean }} 원정</span>
          <span class="font-mono font-black text-base text-slate-950">₩ {{ fmtMoney(calcTotalAmount) }} (VAT포함)</span>
        </div>
      </div>

      <!-- 견적 품목 상세 그리드 테이블 -->
      <div class="border border-slate-900 rounded-xl overflow-hidden mb-6 font-sans text-xs text-left">
        <table class="w-full border-collapse">
          <thead class="bg-slate-50 border-b border-slate-900 text-slate-700">
            <tr class="text-center">
              <th class="border-r border-slate-900 px-2 py-2 font-bold w-12 print:hidden">삭제</th>
              <th class="border-r border-slate-900 px-3 py-2 font-bold text-left">품명 / 항목</th>
              <th class="border-r border-slate-900 px-2 py-2 font-bold w-20">규격</th>
              <th class="border-r border-slate-900 px-2 py-2 font-bold w-14">수량</th>
              <th class="border-r border-slate-900 px-3 py-2 font-bold text-right w-28">단가</th>
              <th class="border-r border-slate-900 px-3 py-2 font-bold text-right w-28">공급가액</th>
              <th class="border-r border-slate-900 px-3 py-2 font-bold text-right w-24">세액(10%)</th>
              <th class="px-3 py-2 font-bold w-28">비고</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-300">
            <tr v-for="(item, idx) in quote.items" :key="idx" class="hover:bg-slate-50/40">
              <!-- 삭제 버튼 (인쇄 시 자동 숨김) -->
              <td class="border-r border-slate-900 px-2 py-1.5 text-center print:hidden">
                <button 
                  @click="removeItemRow(idx)"
                  class="p-1 hover:bg-rose-100 rounded text-rose-600 transition-colors border-none bg-transparent cursor-pointer"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </td>
              <!-- 품명 -->
              <td class="border-r border-slate-900 px-2 py-1.5">
                <input 
                  type="text" 
                  v-model="item.itemName" 
                  placeholder="품목명을 입력하세요"
                  class="w-full bg-transparent border-none outline-none p-0 text-slate-900 font-bold focus:bg-slate-50 print:bg-transparent" 
                />
              </td>
              <!-- 규격 -->
              <td class="border-r border-slate-900 px-2 py-1.5 text-center">
                <input 
                  type="text" 
                  v-model="item.spec" 
                  placeholder="규격"
                  class="w-full bg-transparent border-none outline-none p-0 text-center text-slate-700 print:bg-transparent" 
                />
              </td>
              <!-- 수량 -->
              <td class="border-r border-slate-900 px-2 py-1.5 text-center font-mono">
                <input 
                  type="number" 
                  v-model.number="item.qty" 
                  @input="updateItemTotals(item)"
                  class="w-full bg-transparent border-none outline-none p-0 text-center font-bold text-slate-900 print:bg-transparent" 
                />
              </td>
              <!-- 단가 -->
              <td class="border-r border-slate-900 px-2 py-1.5 text-right font-mono">
                <input 
                  type="number" 
                  v-model.number="item.unitPrice" 
                  @input="updateItemTotals(item)"
                  placeholder="0"
                  class="w-full bg-transparent border-none outline-none p-0 text-right font-bold text-slate-900 print:bg-transparent" 
                />
              </td>
              <!-- 공급가액 -->
              <td class="border-r border-slate-900 px-3 py-1.5 text-right font-mono font-semibold text-slate-900">
                {{ fmtMoney(item.supplyValue) }}
              </td>
              <!-- 세액 -->
              <td class="border-r border-slate-900 px-3 py-1.5 text-right font-mono text-slate-600">
                {{ fmtMoney(item.taxValue) }}
              </td>
              <!-- 비고 -->
              <td class="px-2 py-1.5">
                <input 
                  type="text" 
                  v-model="item.remarks" 
                  placeholder="비고"
                  class="w-full bg-transparent border-none outline-none p-0 text-slate-650 print:bg-transparent" 
                />
              </td>
            </tr>

            <!-- ── 행 추가 버튼 영역 (인쇄 시 자동 숨김) ── -->
            <tr class="print:hidden">
              <td colspan="8" class="text-center py-2 bg-slate-50 hover:bg-slate-100 transition-colors">
                <button 
                  @click="addItemRow"
                  class="flex items-center justify-center gap-1.5 w-full py-1 text-slate-500 hover:text-emerald-600 font-bold border-none bg-transparent cursor-pointer"
                >
                  <Plus class="w-4 h-4" /> 견적 품목 추가 (행 추가)
                </button>
              </td>
            </tr>

            <!-- 합계 로우 (공급가, 세액) -->
            <tr class="bg-slate-50 font-bold text-slate-900">
              <td class="border-r border-slate-900 px-2 py-2 text-center print:hidden">계</td>
              <td :colspan="3" class="border-r border-slate-900 px-3 py-2 text-left print:pl-4">소계 합계 (₩)</td>
              <td class="border-r border-slate-900 px-3 py-2 text-right font-mono"></td>
              <td class="border-r border-slate-900 px-3 py-2 text-right font-mono text-emerald-700">
                {{ fmtMoney(calcTotalSupply) }}
              </td>
              <td class="border-r border-slate-900 px-3 py-2 text-right font-mono text-rose-600">
                {{ fmtMoney(calcTotalTax) }}
              </td>
              <td class="px-2 py-2"></td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 특이사항 및 메모 (한국 실무 규정) -->
      <div class="border border-slate-400 p-4 rounded-xl bg-slate-50/50 text-left font-sans text-xs mb-8">
        <h4 class="font-extrabold text-slate-800 mb-2">■ 특이사항 및 결제 조건</h4>
        <textarea 
          v-model="quote.remarks"
          rows="4"
          placeholder="특이사항이나 결제 조건을 입력하세요"
          class="w-full bg-transparent border-none outline-none p-0 text-slate-700 leading-relaxed resize-none print:bg-transparent"
        ></textarea>
      </div>

      <!-- 하단 대표 감사 문구 -->
      <div class="text-center py-6 font-sans">
        <p class="text-xs font-bold tracking-[0.2em] text-slate-650 mb-2">신뢰를 바탕으로 최선의 품질과 서비스를 약속드립니다.</p>
        <h3 class="text-sm font-black tracking-[0.4em] text-slate-900">{{ quote.supplierName }} 대표이사 {{ quote.supplierCeo }}</h3>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* 10년 차 시니어 퍼블리싱: 인쇄 전용 CSS 매체 쿼리 적용 */
@media print {
  body {
    background-color: white !important;
    color: black !important;
  }
  .print\:hidden {
    display: none !important;
  }
  /* 인쇄 시 폼 인풋창의 보더와 배경색을 제거하여 완전한 출력 양식 서류화 */
  input, textarea, select {
    border: none !important;
    background: transparent !important;
    padding: 0 !important;
    box-shadow: none !important;
  }
  /* 웹화면 전용 음영이나 보더 래퍼 제외 */
  .p-6 {
    padding: 0 !important;
  }
}
</style>
