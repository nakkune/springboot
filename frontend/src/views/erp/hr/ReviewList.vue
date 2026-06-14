<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useHrStore } from '@/stores/erp/useHrStore'

interface RatingItem {
  key: string
  label: string
  value: number
}

const hrStore = useHrStore()

// ── Filters ──
const filterYear = ref(new Date().getFullYear())
const filterPeriod = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = 20

// ── Dialog state ──
const showFormDialog = ref(false)
const showDetailDialog = ref(false)
const showDeleteConfirm = ref(false)
const isEditing = ref(false)
const selectedReviewId = ref<string | null>(null)

// ── Form state ──
const form = ref({
  employeeId: '',
  reviewerId: '',
  reviewYear: new Date().getFullYear(),
  reviewPeriod: 'annual',
  comment: ''
})
const ratingItems = ref<RatingItem[]>([
  { key: 'task_performance', label: '업무 성과', value: 3 },
  { key: 'collaboration', label: '협업 능력', value: 3 },
  { key: 'leadership', label: '리더십', value: 3 },
  { key: 'attitude', label: '태도', value: 3 },
  { key: 'communication', label: '의사소통', value: 3 }
])

const formError = ref('')
const detailReview = ref<any>(null)
const deleteTargetId = ref<string | null>(null)

// ── Computed ──
const totalPages = computed(() => Math.max(1, Math.ceil(hrStore.reviewTotal / pageSize)))

const averageScore = computed(() => {
  const values = ratingItems.value.map(r => r.value)
  const sum = values.reduce((a, b) => a + b, 0)
  return Math.round((sum / values.length) * 100) / 100
})

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    draft: '작성 중',
    submitted: '제출됨',
    acknowledged: '확인됨'
  }
  return map[status] || status
}

const periodLabel = (period: string) => {
  const map: Record<string, string> = {
    annual: '연간',
    half: '반기',
    quarter: '분기'
  }
  return map[period] || period
}

const statusClass = (status: string) => {
  if (status === 'draft') return 'bg-gray-100 text-gray-700'
  if (status === 'submitted') return 'bg-blue-100 text-blue-700'
  if (status === 'acknowledged') return 'bg-green-100 text-green-700'
  return 'bg-gray-100 text-gray-700'
}

// ── Methods ──
async function loadReviews() {
      await hrStore.fetchReviews({
    reviewYear: filterYear.value,
    reviewPeriod: filterPeriod.value || undefined,
    status: filterStatus.value || undefined,
    page: currentPage.value,
    size: pageSize
  })
}

function resetForm() {
  form.value = {
    employeeId: '',
    reviewerId: '',
    reviewYear: new Date().getFullYear(),
    reviewPeriod: 'annual',
    comment: ''
  }
  ratingItems.value = [
    { key: 'task_performance', label: '업무 성과', value: 3 },
    { key: 'collaboration', label: '협업 능력', value: 3 },
    { key: 'leadership', label: '리더십', value: 3 },
    { key: 'attitude', label: '태도', value: 3 },
    { key: 'communication', label: '의사소통', value: 3 }
  ]
  formError.value = ''
  isEditing.value = false
  selectedReviewId.value = null
}

function openCreateDialog() {
  resetForm()
  showFormDialog.value = true
}

async function openEditDialog(review: any) {
  resetForm()
  isEditing.value = true
  selectedReviewId.value = review.id
  form.value = {
    employeeId: review.employeeId,
    reviewerId: review.reviewerId,
    reviewYear: review.reviewYear,
    reviewPeriod: review.reviewPeriod,
    comment: review.comment || ''
  }
  if (review.ratings) {
    ratingItems.value = ratingItems.value.map(item => ({
      ...item,
      value: review.ratings[item.key] ?? item.value
    }))
  }
  showFormDialog.value = true
}

async function saveReview() {
  if (!form.value.employeeId || !form.value.reviewerId) {
    formError.value = '평가 대상과 평가자를 선택해주세요.'
    return
  }

  const ratings: Record<string, number> = {}
  ratingItems.value.forEach(item => {
    ratings[item.key] = item.value
  })

  const dto = {
    employeeId: form.value.employeeId,
    reviewerId: form.value.reviewerId,
    reviewYear: form.value.reviewYear,
    reviewPeriod: form.value.reviewPeriod,
    ratings,
    totalScore: averageScore.value,
    comment: form.value.comment
  }

  try {
    if (isEditing.value && selectedReviewId.value) {
      await hrStore.updateReview(selectedReviewId.value, dto)
    } else {
      await hrStore.createReview(dto)
    }
    showFormDialog.value = false
    await loadReviews()
  } catch (e: any) {
    formError.value = e?.response?.data?.error || '저장 중 오류가 발생했습니다.'
  }
}

async function openDetail(review: any) {
  detailReview.value = review
  showDetailDialog.value = true
}

async function handleSubmit(id: string) {
  try {
    await hrStore.submitReview(id)
    await loadReviews()
  } catch (e: any) {
    alert(e?.response?.data?.error || '제출 중 오류가 발생했습니다.')
  }
}

async function handleAcknowledge(id: string) {
  try {
    await hrStore.acknowledgeReview(id)
    await loadReviews()
  } catch (e: any) {
    alert(e?.response?.data?.error || '확인 중 오류가 발생했습니다.')
  }
}

function confirmDelete(id: string) {
  deleteTargetId.value = id
  showDeleteConfirm.value = true
}

async function executeDelete() {
  if (!deleteTargetId.value) return
  try {
    await hrStore.deleteReview(deleteTargetId.value)
    showDeleteConfirm.value = false
    deleteTargetId.value = null
    await loadReviews()
  } catch (e: any) {
    alert(e?.response?.data?.error || '삭제 중 오류가 발생했습니다.')
  }
}

function changePage(page: number) {
  currentPage.value = page
  loadReviews()
}

function applyFilters() {
  currentPage.value = 1
  loadReviews()
}

// ── Init ──
onMounted(async () => {
  if (!hrStore.employees.length) {
    await hrStore.fetchEmployees({ size: 1000 })
  }
  loadReviews()
})
</script>

<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">인사 평가</h1>
      <button
        class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        @click="openCreateDialog"
      >
        + 새 평가 등록
      </button>
    </div>

    <!-- Filters -->
    <div class="flex flex-wrap gap-3 mb-6 p-4 bg-white rounded-lg shadow-sm border border-gray-200">
      <div>
        <label class="block text-xs font-medium text-gray-500 mb-1">평가년도</label>
        <select
          v-model="filterYear"
          class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          @change="applyFilters"
        >
          <option v-for="y in [2024, 2025, 2026]" :key="y" :value="y">{{ y }}년</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-500 mb-1">평가 기간</label>
        <select
          v-model="filterPeriod"
          class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          @change="applyFilters"
        >
          <option value="">전체</option>
          <option value="annual">연간</option>
          <option value="half">반기</option>
          <option value="quarter">분기</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-500 mb-1">상태</label>
        <select
          v-model="filterStatus"
          class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          @change="applyFilters"
        >
          <option value="">전체</option>
          <option value="draft">작성 중</option>
          <option value="submitted">제출됨</option>
          <option value="acknowledged">확인됨</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="bg-gray-50 border-b border-gray-200">
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">사번</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">직원명</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">평가자</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">평가연도</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">기간</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">총점</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">상태</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">등록일</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-if="hrStore.loading" class="text-center text-gray-500">
            <td colspan="9" class="px-4 py-8">로딩 중...</td>
          </tr>
          <tr v-else-if="hrStore.reviews.length === 0" class="text-center text-gray-500">
            <td colspan="9" class="px-4 py-8">등록된 평가가 없습니다.</td>
          </tr>
          <tr
            v-for="review in hrStore.reviews"
            :key="review.id"
            class="hover:bg-gray-50 cursor-pointer"
            @click="openDetail(review)"
          >
            <td class="px-4 py-3 text-sm text-gray-900">{{ review.employeeNo }}</td>
            <td class="px-4 py-3 text-sm font-medium text-gray-900">{{ review.employeeName }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ review.reviewerName }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ review.reviewYear }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ periodLabel(review.reviewPeriod) }}</td>
            <td class="px-4 py-3 text-sm font-medium text-gray-900">{{ review.totalScore ?? '-' }}</td>
            <td class="px-4 py-3">
              <span
                class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium"
                :class="statusClass(review.status)"
              >
                {{ statusLabel(review.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ review.createdAt?.slice(0, 10) }}</td>
            <td class="px-4 py-3 text-right text-sm" @click.stop>
              <button
                v-if="review.status === 'draft'"
                class="text-blue-600 hover:text-blue-800 mr-2"
                @click="openEditDialog(review)"
              >
                수정
              </button>
              <button
                v-if="review.status === 'draft'"
                class="text-green-600 hover:text-green-800 mr-2"
                @click="handleSubmit(review.id)"
              >
                제출
              </button>
              <button
                v-if="review.status === 'submitted'"
                class="text-purple-600 hover:text-purple-800 mr-2"
                @click="handleAcknowledge(review.id)"
              >
                확인
              </button>
              <button
                v-if="review.status === 'draft'"
                class="text-red-600 hover:text-red-800"
                @click="confirmDelete(review.id)"
              >
                삭제
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-2 mt-4">
      <button
        class="px-3 py-1 text-sm border rounded-lg disabled:opacity-50"
        :disabled="currentPage <= 1"
        @click="changePage(currentPage - 1)"
      >
        이전
      </button>
      <span class="text-sm text-gray-600">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="px-3 py-1 text-sm border rounded-lg disabled:opacity-50"
        :disabled="currentPage >= totalPages"
        @click="changePage(currentPage + 1)"
      >
        다음
      </button>
    </div>

    <!-- Form Dialog -->
    <div v-if="showFormDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-96 overflow-y-auto">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">
            {{ isEditing ? '평가 수정' : '새 평가 등록' }}
          </h2>
        </div>
        <div class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">평가 대상</label>
            <select
              v-model="form.employeeId"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              :disabled="isEditing"
            >
              <option value="">선택하세요</option>
              <option v-for="emp in hrStore.employees" :key="emp.id" :value="emp.id">
                {{ emp.employeeNo }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">평가자</label>
            <select v-model="form.reviewerId" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="">선택하세요</option>
              <option v-for="emp in hrStore.employees" :key="emp.id" :value="emp.id">
                {{ emp.employeeNo }}
              </option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">평가년도</label>
              <select v-model="form.reviewYear" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option :value="2024">2024년</option>
                <option :value="2025">2025년</option>
                <option :value="2026">2026년</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">평가 기간</label>
              <select v-model="form.reviewPeriod" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                <option value="annual">연간</option>
                <option value="half">반기</option>
                <option value="quarter">분기</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">평가 항목</label>
            <div class="space-y-3">
              <div v-for="(item, idx) in ratingItems" :key="item.key" class="flex items-center justify-between">
                <span class="text-sm text-gray-700">{{ item.label }}</span>
                <div class="flex items-center gap-2">
                  <input type="range" min="1" max="5" step="0.5"
                    v-model.number="ratingItems[idx].value"
                    class="w-32 accent-blue-600" />
                  <span class="text-sm font-medium text-gray-900 w-8 text-center">{{ ratingItems[idx].value }}</span>
                </div>
              </div>
            </div>
            <div class="mt-2 text-right text-sm text-gray-500">
              평균 총점: <span class="font-bold text-blue-600">{{ averageScore }}</span> / 5
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">코멘트</label>
            <textarea v-model="form.comment" rows="3"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none"
              placeholder="평가 코멘트를 입력하세요"></textarea>
          </div>
          <p v-if="formError" class="text-sm text-red-600">{{ formError }}</p>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
          <button class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            @click="showFormDialog = false">취소</button>
          <button class="px-4 py-2 text-sm text-white bg-blue-600 rounded-lg hover:bg-blue-700"
            @click="saveReview">{{ isEditing ? '수정' : '등록' }}</button>
        </div>
      </div>
    </div>

    <!-- Detail Dialog -->
    <div v-if="showDetailDialog && detailReview" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">평가 상세</h2>
          <span class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium"
            :class="statusClass(detailReview.status)">
            {{ statusLabel(detailReview.status) }}
          </span>
        </div>
        <div class="px-6 py-4 space-y-3">
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div><span class="text-gray-500">직원:</span> <span class="font-medium">{{ detailReview.employeeName }}</span></div>
            <div><span class="text-gray-500">사번:</span> <span class="font-medium">{{ detailReview.employeeNo }}</span></div>
            <div><span class="text-gray-500">평가자:</span> <span class="font-medium">{{ detailReview.reviewerName }}</span></div>
            <div><span class="text-gray-500">평가연도:</span> <span class="font-medium">{{ detailReview.reviewYear }}년 ({{ periodLabel(detailReview.reviewPeriod) }})</span></div>
          </div>
          <div v-if="detailReview.ratings" class="border-t pt-3">
            <h3 class="text-sm font-medium text-gray-700 mb-2">평가 점수</h3>
            <div class="space-y-2">
              <div v-for="(val, key) in detailReview.ratings" :key="key" class="flex items-center justify-between text-sm">
                <span class="text-gray-600">{{ key }}</span>
                <span class="font-medium">{{ val }}</span>
              </div>
            </div>
            <div class="mt-2 text-right text-sm">
              <span class="text-gray-500">총점:</span>
              <span class="font-bold text-blue-600 ml-1">{{ detailReview.totalScore ?? '-' }}</span>
            </div>
          </div>
          <div v-if="detailReview.comment" class="border-t pt-3">
            <h3 class="text-sm font-medium text-gray-700 mb-1">코멘트</h3>
            <p class="text-sm text-gray-600 whitespace-pre-wrap">{{ detailReview.comment }}</p>
          </div>
          <div v-if="detailReview.submittedAt" class="border-t pt-3 text-xs text-gray-400">
            제출일: {{ detailReview.submittedAt }}
          </div>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end">
          <button class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            @click="showDetailDialog = false">닫기</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-80 mx-4">
        <div class="px-6 py-4">
          <h3 class="text-lg font-semibold text-gray-900">평가 삭제</h3>
          <p class="mt-2 text-sm text-gray-600">정말로 이 평가를 삭제하시겠습니까?</p>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
          <button class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            @click="showDeleteConfirm = false">취소</button>
          <button class="px-4 py-2 text-sm text-white bg-red-600 rounded-lg hover:bg-red-700"
            @click="executeDelete">삭제</button>
        </div>
      </div>
    </div>

    <!-- Detail Dialog -->
    <Teleport to="body">
      <div v-if="showDetailDialog && detailReview" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
        <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
          <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-900">평가 상세</h2>
            <span
              class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium"
              :class="statusClass(detailReview.status)"
            >
              {{ statusLabel(detailReview.status) }}
            </span>
          </div>
          <div class="px-6 py-4 space-y-3">
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div><span class="text-gray-500">직원:</span> <span class="font-medium">{{ detailReview.employeeName }}</span></div>
              <div><span class="text-gray-500">사번:</span> <span class="font-medium">{{ detailReview.employeeNo }}</span></div>
              <div><span class="text-gray-500">평가자:</span> <span class="font-medium">{{ detailReview.reviewerName }}</span></div>
              <div><span class="text-gray-500">평가연도:</span> <span class="font-medium">{{ detailReview.reviewYear }}년 ({{ periodLabel(detailReview.reviewPeriod) }})</span></div>
            </div>

            <div v-if="detailReview.ratings" class="border-t pt-3">
              <h3 class="text-sm font-medium text-gray-700 mb-2">평가 점수</h3>
              <div class="space-y-2">
                <div
                  v-for="(val, key) in detailReview.ratings"
                  :key="key"
                  class="flex items-center justify-between text-sm"
                >
                  <span class="text-gray-600">{{ key }}</span>
                  <span class="font-medium">{{ val }}</span>
                </div>
              </div>
              <div class="mt-2 text-right text-sm">
                <span class="text-gray-500">총점:</span>
                <span class="font-bold text-blue-600 ml-1">{{ detailReview.totalScore ?? '-' }}</span>
              </div>
            </div>

            <div v-if="detailReview.comment" class="border-t pt-3">
              <h3 class="text-sm font-medium text-gray-700 mb-1">코멘트</h3>
              <p class="text-sm text-gray-600 whitespace-pre-wrap">{{ detailReview.comment }}</p>
            </div>

            <div v-if="detailReview.submittedAt" class="border-t pt-3 text-xs text-gray-400">
              제출일: {{ detailReview.submittedAt }}
            </div>
          </div>
          <div class="px-6 py-4 border-t border-gray-200 flex justify-end">
            <button
              class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              @click="showDetailDialog = false"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirm -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
        <div class="bg-white rounded-xl shadow-xl w-80 mx-4">
          <div class="px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">평가 삭제</h3>
            <p class="mt-2 text-sm text-gray-600">정말로 이 평가를 삭제하시겠습니까?</p>
          </div>
          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <button
              class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              @click="showDeleteConfirm = false"
            >
              취소
            </button>
            <button
              class="px-4 py-2 text-sm text-white bg-red-600 rounded-lg hover:bg-red-700"
              @click="executeDelete"
            >
              삭제
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
