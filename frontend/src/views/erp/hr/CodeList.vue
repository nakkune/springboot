<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useHrStore, type CodeDto, type CodeGroupDto } from '@/stores/erp/useHrStore'
import { Plus, Loader2, Pencil, Trash2, Check, X } from 'lucide-vue-next'

const hrStore = useHrStore()

const codeGroups = ref<CodeGroupDto[]>([])
const selectedGroup = ref('')
const codes = ref<CodeDto[]>([])
const loading = ref(false)
const loadingGroups = ref(false)

const groupLabels: Record<string, string> = {
  POSITION: '직위', JOB_TITLE: '직책', EMPLOYMENT_TYPE: '고용형태',
  LEAVE_TYPE: '휴가유형', REVIEW_PERIOD: '평가기간', EMPLOYEE_STATUS: '사원상태',
  QUOTATION_STATUS: '견적서상태', TAX_INVOICE_STATUS: '세금계산서상태', TAX_INVOICE_PURPOSE: '세금계산서목적'
}
const groupOrder = [
  'POSITION', 'JOB_TITLE', 'EMPLOYMENT_TYPE', 'LEAVE_TYPE', 'REVIEW_PERIOD', 'EMPLOYEE_STATUS',
  'QUOTATION_STATUS', 'TAX_INVOICE_STATUS', 'TAX_INVOICE_PURPOSE'
]

function getGroupDisplayName(g: CodeGroupDto): string {
  return g.codeGroupName || groupLabels[g.codeGroup] || g.codeGroup
}

const showModal = ref(false)
const isEditing = ref(false)
const editId = ref('')
const form = ref({ codeGroup: '', codeGroupName: '', code: '', label: '', sortOrder: 0, isActive: true })

const showGroupModal = ref(false)
const newGroupId = ref('')
const newGroupDisplayName = ref('')

onMounted(async () => {
  await loadGroups()
})

async function loadGroups() {
  loadingGroups.value = true
  try {
    const raw = await hrStore.fetchCodeGroups()
    codeGroups.value = raw.sort((a, b) => {
      const ai = groupOrder.indexOf(a.codeGroup); const bi = groupOrder.indexOf(b.codeGroup)
      return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi)
    })
    if (codeGroups.value.length > 0) {
      selectedGroup.value = codeGroups.value[0].codeGroup
      await loadCodes()
    }
  } catch (e: any) {
    console.error('Failed to load groups:', e)
    codeGroups.value = []
  } finally {
    loadingGroups.value = false
  }
}

async function loadCodes() {
  if (!selectedGroup.value) return
  loading.value = true
  try {
    codes.value = await hrStore.fetchCodes(selectedGroup.value)
  } catch (e: any) {
    console.error('Failed to load codes:', e)
    codes.value = []
  } finally {
    loading.value = false
  }
}

watch(selectedGroup, () => { loadCodes() })

function openAdd() {
  isEditing.value = false
  editId.value = ''
  const groupInfo = codeGroups.value.find(g => g.codeGroup === selectedGroup.value)
  form.value = {
    codeGroup: selectedGroup.value || '',
    codeGroupName: groupInfo?.codeGroupName || '',
    code: '',
    label: '',
    sortOrder: codes.value.length + 1,
    isActive: true
  }
  showModal.value = true
}

function openEdit(item: CodeDto) {
  isEditing.value = true
  editId.value = item.id
  form.value = {
    codeGroup: item.codeGroup,
    codeGroupName: item.codeGroupName,
    code: item.code,
    label: item.label,
    sortOrder: item.sortOrder,
    isActive: item.isActive
  }
  showModal.value = true
}

async function save() {
  if (!form.value.code.trim() || !form.value.label.trim() || !form.value.codeGroup.trim()) return
  const dto: Partial<CodeDto> = {
    codeGroup: form.value.codeGroup.trim(),
    codeGroupName: form.value.codeGroupName.trim(),
    code: form.value.code.trim(),
    label: form.value.label.trim(),
    sortOrder: form.value.sortOrder,
    isActive: form.value.isActive
  }
  try {
    if (isEditing.value && editId.value) {
      await hrStore.updateCode(editId.value, dto)
    } else {
      await hrStore.createCode(dto)
    }
  } catch (e: any) {
    alert('저장 실패: ' + (e?.response?.data?.error || e?.message || '알 수 없는 오류'))
    return
  }
  showModal.value = false
  // 새 그룹이 생성되었을 수 있으므로 그룹 목록 다시 로드
  const prevGroup = selectedGroup.value
  codeGroups.value = await hrStore.fetchCodeGroups()
  if (prevGroup && codeGroups.value.some(g => g.codeGroup === prevGroup)) {
    selectedGroup.value = prevGroup
  } else if (codeGroups.value.length > 0) {
    selectedGroup.value = codeGroups.value[0].codeGroup
  }
  await loadCodes()
}

async function confirmDelete(id: string) {
  if (!window.confirm('정말 삭제하시겠습니까?')) return
  try {
    await hrStore.deleteCode(id)
  } catch (e: any) {
    alert('삭제 실패: ' + (e?.response?.data?.error || e?.message || '알 수 없는 오류'))
    return
  }
  const prevGroup = selectedGroup.value
  codeGroups.value = await hrStore.fetchCodeGroups()
  if (prevGroup && codeGroups.value.some(g => g.codeGroup === prevGroup)) {
    selectedGroup.value = prevGroup
  } else if (codeGroups.value.length > 0) {
    selectedGroup.value = codeGroups.value[0].codeGroup
  }
  await loadCodes()
}

function openAddGroup() {
  newGroupId.value = ''
  newGroupDisplayName.value = ''
  showGroupModal.value = true
}

async function saveGroup() {
  const groupKey = newGroupId.value.trim().toUpperCase().replace(/\s+/g, '_')
  const displayName = newGroupDisplayName.value.trim()
  if (!groupKey) return
  if (codeGroups.value.some(g => g.codeGroup === groupKey)) {
    selectedGroup.value = groupKey
    showGroupModal.value = false
    return
  }
  try {
    await hrStore.createCode({
      codeGroup: groupKey,
      codeGroupName: displayName || groupKey,
      code: groupKey.toLowerCase(),
      label: displayName || groupKey,
      sortOrder: 1,
      isActive: true
    })
  } catch (e: any) {
    alert('그룹 추가 실패: ' + (e?.response?.data?.error || e?.message || '알 수 없는 오류'))
    return
  }
  showGroupModal.value = false
  codeGroups.value = await hrStore.fetchCodeGroups()
  selectedGroup.value = groupKey
  await loadCodes()
}
</script>

<template>
  <div class="p-6 h-full flex flex-col">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-xl font-bold text-slate-800 dark:text-white">기준정보 관리</h1>
      <button
        @click="openAdd"
        class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium transition-colors border-none cursor-pointer"
      >
        <Plus class="w-4 h-4" />
        코드 추가
      </button>
    </div>

    <div class="flex-1 flex gap-4 min-h-0">
      <!-- Left: Group List -->
      <div class="w-48 shrink-0 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-y-auto">
        <div v-if="loadingGroups" class="flex items-center justify-center py-8">
          <Loader2 class="w-5 h-5 animate-spin text-indigo-500" />
        </div>
        <template v-else>
          <div class="px-3 py-2 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider border-b border-slate-100 dark:border-slate-700 flex items-center justify-between">
            <span>코드 그룹</span>
            <button
              @click.stop="openAddGroup"
              class="p-0.5 rounded hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors border-none cursor-pointer"
              title="그룹 추가"
            >
              <Plus class="w-3.5 h-3.5" />
            </button>
          </div>
          <div
            v-for="group in codeGroups"
            :key="group.codeGroup"
            @click="selectedGroup = group.codeGroup"
            class="px-3 py-2 text-sm cursor-pointer transition-colors border-b border-slate-50 dark:border-slate-700/50"
            :class="selectedGroup === group.codeGroup
              ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 font-medium'
              : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700/50'"
          >
            <span class="block leading-tight">{{ getGroupDisplayName(group) }}</span>
            <span class="block text-xs text-slate-400 dark:text-slate-500 mt-0.5">{{ group.codeGroup }}</span>
          </div>
          <div v-if="codeGroups.length === 0" class="px-3 py-4 text-sm text-slate-400 text-center">
            등록된 그룹이 없습니다.<br/>우측 상단 [+ 코드 추가] 버튼으로 등록하세요.
          </div>
        </template>
      </div>

      <!-- Right: Code Table -->
      <div class="flex-1 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-auto">
        <div v-if="!selectedGroup" class="flex items-center justify-center h-full text-slate-400">
          왼쪽에서 코드 그룹을 선택하세요
        </div>
        <div v-else-if="loading" class="flex items-center justify-center h-full">
          <Loader2 class="w-6 h-6 animate-spin text-indigo-500" />
        </div>
        <div v-else-if="codes.length === 0" class="flex items-center justify-center h-full text-slate-400">
          등록된 코드가 없습니다. [+ 코드 추가] 버튼으로 등록하세요.
        </div>
        <table v-else class="w-full text-sm">
          <thead class="bg-slate-50 dark:bg-slate-700/50 text-slate-600 dark:text-slate-300 sticky top-0">
            <tr>
              <th class="text-left px-4 py-3 font-medium w-16">순서</th>
              <th class="text-left px-4 py-3 font-medium w-28">코드</th>
              <th class="text-left px-4 py-3 font-medium">표시명</th>
              <th class="text-center px-4 py-3 font-medium w-20">사용</th>
              <th class="text-center px-4 py-3 font-medium w-28">관리</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-slate-700">
            <tr
              v-for="item in codes"
              :key="item.id"
              class="hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors"
            >
              <td class="px-4 py-3 text-slate-500">{{ item.sortOrder }}</td>
              <td class="px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">{{ item.code }}</td>
              <td class="px-4 py-3 text-slate-800 dark:text-slate-200">{{ item.label }}</td>
              <td class="px-4 py-3 text-center">
                <span
                  class="inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full"
                  :class="item.isActive
                    ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                    : 'bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-400'"
                >
                  <Check v-if="item.isActive" class="w-3 h-3" />
                  <X v-else class="w-3 h-3" />
                  {{ item.isActive ? 'ON' : 'OFF' }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <button
                    @click="openEdit(item)"
                    class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors border-none cursor-pointer"
                    title="수정"
                  >
                    <Pencil class="w-4 h-4" />
                  </button>
                  <button
                    @click="confirmDelete(item.id)"
                    class="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-slate-400 hover:text-red-600 dark:hover:text-red-400 transition-colors border-none cursor-pointer"
                    title="삭제"
                  >
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      @click.self="showModal = false"
    >
      <div class="bg-white dark:bg-slate-800 rounded-xl shadow-2xl w-full max-w-md p-6 border border-slate-200 dark:border-slate-700">
        <h2 class="text-lg font-semibold text-slate-800 dark:text-white mb-4">
          {{ isEditing ? '코드 수정' : '코드 추가' }}
        </h2>
        <div class="flex flex-col gap-4">
          <div>
            <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">코드 그룹</label>
            <div class="relative">
              <input
                v-model="form.codeGroup"
                list="codeGroupOptions"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                placeholder="그룹명 입력 또는 선택"
              />
              <datalist id="codeGroupOptions">
                <option v-for="g in codeGroups" :key="g.codeGroup" :value="g.codeGroup" :label="getGroupDisplayName(g)" />
              </datalist>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">그룹 표시명</label>
            <input
              v-model="form.codeGroupName"
              class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="예: 직위"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">코드</label>
            <input
              v-model="form.code"
              class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="예: full_time"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">표시명</label>
            <input
              v-model="form.label"
              class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="예: 정규직"
            />
          </div>
          <div class="flex gap-4">
            <div class="flex-1">
              <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">정렬순서</label>
              <input
                v-model.number="form.sortOrder"
                type="number"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              />
            </div>
            <div class="flex items-end pb-1">
              <label class="flex items-center gap-2 cursor-pointer">
                <span class="text-sm font-medium text-slate-600 dark:text-slate-300">사용</span>
                <button
                  type="button"
                  @click="form.isActive = !form.isActive"
                  class="relative inline-flex h-5 w-9 items-center rounded-full transition-colors border-none cursor-pointer"
                  :class="form.isActive ? 'bg-indigo-600' : 'bg-slate-300 dark:bg-slate-600'"
                >
                  <span
                    class="inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform"
                    :class="form.isActive ? 'translate-x-4.5' : 'translate-x-1'"
                  />
                </button>
              </label>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <button
            @click="showModal = false"
            class="px-4 py-2 rounded-lg text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 transition-colors border-none cursor-pointer"
          >
            취소
          </button>
          <button
            @click="save"
            class="px-4 py-2 rounded-lg text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 transition-colors border-none cursor-pointer"
            :disabled="!form.codeGroup.trim() || !form.code.trim() || !form.label.trim()"
          >
            {{ isEditing ? '수정' : '추가' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Group Add Modal -->
    <div
      v-if="showGroupModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      @click.self="showGroupModal = false"
    >
      <div class="bg-white dark:bg-slate-800 rounded-xl shadow-2xl w-full max-w-sm p-6 border border-slate-200 dark:border-slate-700">
        <h2 class="text-lg font-semibold text-slate-800 dark:text-white mb-4">코드 그룹 추가</h2>
        <div class="flex flex-col gap-3">
          <div>
            <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">그룹 식별자</label>
            <input
              v-model="newGroupId"
              class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="예: DEPARTMENT"
              @keyup.enter="saveGroup"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">그룹 표시명</label>
            <input
              v-model="newGroupDisplayName"
              class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="예: 부서"
              @keyup.enter="saveGroup"
            />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <button
            @click="showGroupModal = false"
            class="px-4 py-2 rounded-lg text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 transition-colors border-none cursor-pointer"
          >
            취소
          </button>
          <button
            @click="saveGroup"
            class="px-4 py-2 rounded-lg text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 transition-colors border-none cursor-pointer"
            :disabled="!newGroupId.trim()"
          >
            추가
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
