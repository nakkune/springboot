<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useHrStore, type Department, type Employee } from '@/stores/erp/useHrStore'
import { Plus, ChevronRight, ChevronDown, Building2, Trash2, Loader2, Save } from 'lucide-vue-next'

const hrStore = useHrStore()

const tree = ref<Department[]>([])
const employees = ref<Employee[]>([])
const loading = ref(false)
const expandedIds = ref<Set<string>>(new Set())

// 직책(JOB_TITLE) 공통코드 → 라벨 변환용 맵
const jobTitleMap = ref<Record<string, string>>({})

const selectedDept = ref<Department | null>(null)
const isAdding = ref(false)
const isAddingChild = ref(false)
const form = ref({
  name: '',
  code: '',
  parentId: null as string | null,
  sortOrder: 0,
  isActive: true,
  managerId: null as string | null
})

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const result = await hrStore.fetchDepartmentTree()
    tree.value = result
    await hrStore.fetchEmployees({ size: 1000 })

    // 직책 공통코드를 로드하여 code → label 변환 맵 구성
    try {
      const codes = await hrStore.fetchCodes('JOB_TITLE')
      const map: Record<string, string> = {}
      for (const c of codes) { map[c.code] = c.label }
      jobTitleMap.value = map
    } catch {
      jobTitleMap.value = {}
    }
  } finally {
    loading.value = false
  }
}

// 직책 코드를 한글 라벨로 변환하는 헬퍼 함수
function jobTitleLabel(code: string | undefined): string {
  if (!code) return '-'
  return jobTitleMap.value[code] || code
}

function toggleExpand(id: string) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) { next.delete(id) } else { next.add(id) }
  expandedIds.value = next
}

function selectDept(dept: Department) {
  selectedDept.value = dept
  isAdding.value = false
  isAddingChild.value = false
  form.value = {
    name: dept.name,
    code: dept.code || '',
    parentId: dept.parentId,
    sortOrder: dept.sortOrder,
    isActive: dept.isActive,
    managerId: dept.managerId
  }
}

function startAddRoot() {
  isAdding.value = true
  isAddingChild.value = false
  selectedDept.value = null
  form.value = { name: '', code: '', parentId: null, sortOrder: 0, isActive: true, managerId: null }
}

function startAddChild(dept: Department) {
  isAdding.value = true
  isAddingChild.value = true
  selectedDept.value = dept
  form.value = { name: '', code: '', parentId: dept.id, sortOrder: 0, isActive: true, managerId: null }
}

function cancelForm() {
  isAdding.value = false
  selectedDept.value = null
}

async function saveForm() {
  if (!form.value.name.trim()) return
  const dto: Partial<Department> = {
    name: form.value.name.trim(),
    code: form.value.code.trim() || undefined,
    parentId: form.value.parentId,
    sortOrder: form.value.sortOrder,
    isActive: form.value.isActive,
    managerId: form.value.managerId || undefined
  }
  if (selectedDept.value && !isAdding.value) {
    await hrStore.updateDepartment(selectedDept.value.id, dto)
  } else {
    await hrStore.createDepartment(dto)
  }
  await loadData()
  cancelForm()
}

async function confirmDelete(id: string) {
  if (window.confirm('정말 이 부서를 삭제하시겠습니까?\n하위 부서가 있으면 삭제할 수 없습니다.')) {
    try {
      await hrStore.deleteDepartment(id)
      await loadData()
      selectedDept.value = null
    } catch {
      window.alert('하위 부서가 존재하여 삭제할 수 없습니다.')
    }
  }
}

const flatDepts = computed(() => {
  const result: Department[] = []
  function walk(list: Department[]) {
    for (const d of list) {
      result.push(d)
      if (d.children) walk(d.children)
    }
  }
  walk(tree.value)
  return result
})
</script>

<template>
  <div class="p-6 h-full flex flex-col">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-xl font-bold text-slate-800 dark:text-white">부서 관리</h1>
      <button
        @click="startAddRoot"
        class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium transition-colors border-none cursor-pointer"
      >
        <Plus class="w-4 h-4" />
        최상위 부서 추가
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center flex-1">
      <Loader2 class="w-6 h-6 animate-spin text-indigo-500" />
    </div>

    <div v-else class="flex-1 flex gap-4 min-h-0">
      <!-- Left: Tree -->
      <div class="w-64 shrink-0 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-y-auto">
        <div class="px-3 py-2 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider border-b border-slate-100 dark:border-slate-700 flex items-center gap-1.5">
          <Building2 class="w-3.5 h-3.5" />
          조직도
        </div>
        <div v-if="tree.length === 0" class="px-3 py-4 text-sm text-slate-400 text-center">
          등록된 부서가 없습니다
        </div>
        <div v-for="dept in tree" :key="dept.id" class="py-1 px-1">
          <!-- Root level -->
          <div
            class="flex items-center group px-2 py-1.5 rounded-lg cursor-pointer transition-colors text-sm"
            :class="selectedDept?.id === dept.id && !isAdding
              ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 font-medium'
              : 'hover:bg-slate-50 dark:hover:bg-slate-700/50 text-slate-700 dark:text-slate-300'"
            @click="selectDept(dept)"
          >
            <button
              @click.stop="toggleExpand(dept.id)"
              class="p-0.5 mr-1 border-none cursor-pointer bg-transparent text-slate-400 hover:text-slate-600"
            >
              <ChevronDown v-if="dept.children?.length && expandedIds.has(dept.id)" class="w-3.5 h-3.5" />
              <ChevronRight v-else-if="dept.children?.length" class="w-3.5 h-3.5" />
              <span v-else class="w-3.5 h-3.5 inline-block" />
            </button>
            <span class="flex-1 truncate">{{ dept.name }}</span>
            <button
              @click.stop="startAddChild(dept)"
              class="opacity-0 group-hover:opacity-100 p-0.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 border-none cursor-pointer bg-transparent text-slate-400 hover:text-slate-600"
              title="하위 부서 추가"
            >
              <Plus class="w-3.5 h-3.5" />
            </button>
          </div>
          <!-- Children -->
          <template v-if="dept.children?.length && expandedIds.has(dept.id)">
            <div
              v-for="child in dept.children"
              :key="child.id"
              class="ml-5 border-l border-slate-200 dark:border-slate-700 pl-2"
            >
              <div
                class="flex items-center group px-2 py-1.5 rounded-lg cursor-pointer transition-colors text-sm"
                :class="selectedDept?.id === child.id && !isAdding
                  ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 font-medium'
                  : 'hover:bg-slate-50 dark:hover:bg-slate-700/50 text-slate-700 dark:text-slate-300'"
                @click="selectDept(child)"
              >
                <button
                  @click.stop="toggleExpand(child.id)"
                  class="p-0.5 mr-1 border-none cursor-pointer bg-transparent text-slate-400 hover:text-slate-600"
                >
                  <ChevronDown v-if="child.children?.length && expandedIds.has(child.id)" class="w-3.5 h-3.5" />
                  <ChevronRight v-else-if="child.children?.length" class="w-3.5 h-3.5" />
                  <span v-else class="w-3.5 h-3.5 inline-block" />
                </button>
                <span class="flex-1 truncate">{{ child.name }}</span>
                <button
                  @click.stop="startAddChild(child)"
                  class="opacity-0 group-hover:opacity-100 p-0.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 border-none cursor-pointer bg-transparent text-slate-400 hover:text-slate-600"
                  title="하위 부서 추가"
                >
                  <Plus class="w-3.5 h-3.5" />
                </button>
              </div>
              <!-- Grandchildren (2 deep) -->
              <template v-if="child.children?.length && expandedIds.has(child.id)">
                <div
                  v-for="grand in child.children"
                  :key="grand.id"
                  class="ml-5 border-l border-slate-200 dark:border-slate-700 pl-2"
                >
                  <div
                    class="flex items-center group px-2 py-1.5 rounded-lg cursor-pointer transition-colors text-sm"
                    :class="selectedDept?.id === grand.id && !isAdding
                      ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 font-medium'
                      : 'hover:bg-slate-50 dark:hover:bg-slate-700/50 text-slate-700 dark:text-slate-300'"
                    @click="selectDept(grand)"
                  >
                    <span class="w-3.5 h-3.5 inline-block mr-1" />
                    <span class="flex-1 truncate">{{ grand.name }}</span>
                    <button
                      @click.stop="startAddChild(grand)"
                      class="opacity-0 group-hover:opacity-100 p-0.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 border-none cursor-pointer bg-transparent text-slate-400 hover:text-slate-600"
                      title="하위 부서 추가"
                    >
                      <Plus class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>

      <!-- Right: Edit Panel -->
      <div class="flex-1 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
        <div v-if="!selectedDept && !isAdding" class="flex items-center justify-center h-full text-slate-400">
          왼쪽에서 부서를 선택하거나 [+ 최상위 부서 추가] 버튼을 클릭하세요
        </div>
        <template v-else>
          <h2 class="text-lg font-semibold text-slate-800 dark:text-white mb-4">
            {{ isAdding ? '부서 추가' : '부서 편집' }}
          </h2>
          <div class="flex flex-col gap-4 max-w-md">
            <div>
              <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">부서명</label>
              <input
                v-model="form.name"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                placeholder="부서명 입력"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">코드</label>
              <input
                v-model="form.code"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                placeholder="예: DEV-FRONT-01"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">상위부서</label>
              <select
                v-model="form.parentId"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option :value="null">없음 (최상위)</option>
                <option
                  v-for="d in flatDepts"
                  :key="d.id"
                  :value="d.id"
                  :disabled="d.id === selectedDept?.id"
                >
                  {{ d.name }}
                </option>
              </select>
            </div>
            <div class="flex gap-4">
              <div class="flex-1">
                <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">정렬순서</label>
                <input
                  v-model.number="form.sortOrder"
                  type="number"
                  class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
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
            <div>
              <label class="block text-sm font-medium text-slate-600 dark:text-slate-300 mb-1">부서장</label>
              <select
                v-model="form.managerId"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option :value="null">없음</option>
                <option
                  v-for="emp in hrStore.employees"
                  :key="emp.id"
                  :value="emp.id"
                >
                  {{ emp.employeeName || emp.employeeNo }} - {{ jobTitleLabel(emp.jobTitle) }}
                </option>
              </select>
            </div>
          </div>
          <div class="flex items-center gap-2 mt-6">
            <button
              @click="saveForm"
              class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 transition-colors border-none cursor-pointer"
              :disabled="!form.name.trim()"
            >
              <Save class="w-4 h-4" />
              저장
            </button>
            <button
              @click="cancelForm"
              class="px-4 py-2 rounded-lg text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 transition-colors border-none cursor-pointer"
            >
              취소
            </button>
            <button
              v-if="selectedDept && !isAdding"
              @click="confirmDelete(selectedDept.id)"
              class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors border border-red-200 dark:border-red-800 bg-white dark:bg-slate-800 ml-auto border-none cursor-pointer"
            >
              <Trash2 class="w-4 h-4" />
              삭제
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
