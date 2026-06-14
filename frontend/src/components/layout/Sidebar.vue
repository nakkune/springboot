<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'
import { 
  Settings, 
  ChevronDown, 
  ChevronRight, 
  Folder, 
  Trello, 
  Loader2,
  Building2,
  Plus,
  Trash,
  Home,
  Users,
  Briefcase,
  Calendar,
  DollarSign,
  GitBranch,
  BookOpen,
  FileText,
  Receipt
} from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'openSettings', tab?: string): void
}>()

const isOpen = ref(true)
const router = useRouter()
const route = useRoute()
const workspaceStore = useWorkspaceStore()
const userRole = ref('member') // [NEW] 관리자 권한 메뉴 제어용

onMounted(async () => {
  // 컴포넌트 마운트 시 최상위 워크스페이스 목록 로드
  await workspaceStore.fetchWorkspaces()
  userRole.value = localStorage.getItem('userRole') || 'member'
})

const navigateToMain = () => {
  router.push('/main')
}

const navigateToBoard = (boardId: string) => {
  router.push(`/board/${boardId}`)
}

const handleWorkspaceToggle = async (wsId: string) => {
  await workspaceStore.fetchProjects(wsId)
}

const handleProjectToggle = async (wsId: string, projId: string) => {
  await workspaceStore.fetchBoards(wsId, projId)
}

// 10년 차 시니어 설계: 브라우저 기본 다이얼로그를 활용한 빠르고 안전한 CRUD
const handleCreateWorkspace = async () => {
  const name = window.prompt('새 워크스페이스 이름을 입력하세요:')
  if (!name || !name.trim()) return
  await workspaceStore.createWorkspace(name.trim())
}

const handleDeleteWorkspace = async (wsId: string, wsName: string) => {
  if (window.confirm(`정말 '${wsName}' 워크스페이스를 삭제하시겠습니까?\n경고: 하위의 모든 프로젝트와 보드가 함께 삭제됩니다!`)) {
    await workspaceStore.deleteWorkspace(wsId)
  }
}

const handleCreateProject = async (wsId: string) => {
  const name = window.prompt('새 프로젝트 이름을 입력하세요:')
  if (!name || !name.trim()) return
  await workspaceStore.createProject(wsId, name.trim())
}

const handleDeleteProject = async (wsId: string, projId: string, projName: string) => {
  if (window.confirm(`정말 '${projName}' 프로젝트를 삭제하시겠습니까?\n경고: 하위의 모든 보드가 함께 삭제됩니다!`)) {
    await workspaceStore.deleteProject(wsId, projId)
  }
}

const handleCreateBoard = async (wsId: string, projId: string) => {
  const name = window.prompt('새 보드 이름을 입력하세요:')
  if (!name || !name.trim()) return
  await workspaceStore.createBoard(wsId, projId, name.trim())
}

const handleDeleteBoard = async (wsId: string, projId: string, boardId: string, boardName: string) => {
  if (window.confirm(`정말 '${boardName}' 보드를 삭제하시겠습니까?`)) {
    await workspaceStore.deleteBoard(wsId, projId, boardId)
    if (route.params.id === boardId) {
      router.push('/main')
    }
  }
}
</script>

<template>
  <aside 
    class="bg-slate-50 dark:bg-slate-900 border-r border-slate-200/80 dark:border-slate-800 text-slate-600 dark:text-slate-300 transition-all duration-300 flex flex-col relative select-none z-30"
    :class="isOpen ? 'w-64' : 'w-16'"
  >
    <!-- Logo & Toggle -->
    <div class="h-14 flex items-center px-4 border-b border-slate-200/80 dark:border-slate-800 justify-between">
      <div class="flex items-center">
        <div 
          class="w-8 h-8 rounded bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold cursor-pointer shadow-md hover:scale-105 active:scale-95 transition-transform" 
          @click="isOpen = !isOpen"
        >
          M
        </div>
        <span 
          v-if="isOpen" 
          class="ml-3 font-semibold text-lg tracking-tight bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-800 dark:from-white dark:to-slate-400 bg-clip-text text-transparent transition-opacity duration-300"
        >
          MiniERP
        </span>
      </div>
    </div>

    <!-- Nav Items & Tree View -->
    <div class="flex-1 overflow-y-auto py-4 flex flex-col gap-4 px-2 custom-scrollbar">
      
      <!-- 상단 메인 메뉴 -->
      <div class="flex flex-col gap-1">
        <div 
          @click="navigateToMain"
          class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
          :class="route.path === '/main' ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
        >
          <Home class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
          <span v-if="isOpen" class="ml-3">Main</span>
        </div>
        
        <!-- [NEW] 관리자 가입승인 및 권한제어 어드민 콘솔 메뉴 신설 (시니어 E2E 설계) -->
        <div 
          v-if="userRole === 'admin' || userRole === 'moderator'"
          @click="router.push('/admin')"
          :class="route.path === '/admin' ? 'bg-slate-200/60 dark:bg-slate-800 text-indigo-650 dark:text-indigo-300 font-semibold border-l-2 border-indigo-500 pl-2.5' : 'flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white text-indigo-600 dark:text-indigo-400/90'"
        >
          <Building2 class="w-5 h-5 text-indigo-500 dark:text-indigo-400 shrink-0" />
          <span v-if="isOpen" class="ml-3">Admin Console</span>
        </div>

        <!-- Main / ERP 구분선 -->
        <div v-if="isOpen" class="h-px bg-slate-200 dark:bg-slate-800 my-1 mx-2"></div>

        <!-- ERP 섹션 -->
        <div v-if="isOpen" class="flex flex-col gap-1">
          <h3 class="text-xs font-semibold text-slate-450 dark:text-slate-500 uppercase tracking-wider px-3">
            ERP
          </h3>
          <div
            @click="router.push('/hr/employees')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path === '/hr/employees' ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <Users class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">사원</span>
          </div>
          <div
            @click="router.push('/hr/attendance')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path === '/hr/attendance' ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <Loader2 class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">근태</span>
          </div>
          <div
            @click="router.push('/hr/leaves')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path === '/hr/leaves' ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <Calendar class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">휴가</span>
          </div>
          <div
            @click="router.push('/hr/payroll')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path === '/hr/payroll' || route.path.includes('/payrolls') ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <DollarSign class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">급여대장 작성/관리</span>
          </div>
          <div
            @click="router.push('/hr/quotations')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path.startsWith('/hr/quotations') ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <FileText class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">견적서 관리</span>
          </div>
          <div
            @click="router.push('/hr/tax-invoices')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path.startsWith('/hr/tax-invoices') ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <Receipt class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">세금계산서 관리</span>
          </div>
          <!-- 기준정보 sub-header -->
          <div class="h-px bg-slate-200 dark:bg-slate-800 my-1.5 mx-2"></div>
          <h3 class="text-xs font-semibold text-slate-450 dark:text-slate-500 uppercase tracking-wider px-3 mb-0.5">
            기준정보
          </h3>
          <div
            @click="router.push('/hr/departments')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path === '/hr/departments' ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <GitBranch class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">부서 관리</span>
          </div>
          <div
            @click="router.push('/hr/codes')"
            class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200 text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
            :class="route.path === '/hr/codes' ? 'bg-slate-200/60 dark:bg-slate-800 text-slate-950 dark:text-white font-semibold border-l-2 border-indigo-500 pl-2.5' : ''"
          >
            <BookOpen class="w-5 h-5 text-slate-500 dark:text-slate-400 shrink-0" />
            <span class="ml-3">공통 코드</span>
          </div>
        </div>
      </div>

      <!-- 구분선 -->
      <div v-if="isOpen" class="h-px bg-slate-200 dark:bg-slate-800 my-1 mx-2"></div>
      
      <!-- 워크스페이스 계층 구조 트리 -->
      <div v-if="isOpen" class="flex flex-col gap-2">
        <div class="flex items-center justify-between px-3 pr-4 group">
          <h3 class="text-xs font-semibold text-slate-450 dark:text-slate-500 uppercase tracking-wider">
            Workspaces
          </h3>
          <button @click.stop="handleCreateWorkspace" class="opacity-0 group-hover:opacity-100 hover:bg-slate-200 dark:hover:bg-slate-700 p-0.5 rounded transition-all text-slate-400 dark:text-slate-500 hover:text-slate-800 dark:hover:text-white border-none cursor-pointer bg-transparent" title="새 워크스페이스 추가">
            <Plus class="w-3.5 h-3.5" />
          </button>
        </div>

        <!-- 로딩 표시 -->
        <div v-if="workspaceStore.isLoadingWorkspaces" class="flex items-center justify-center py-4">
          <Loader2 class="w-5 h-5 text-indigo-400 animate-spin" />
        </div>

        <div v-else class="flex flex-col gap-1">
          <!-- 1단계: 워크스페이스 루프 -->
          <div v-for="ws in workspaceStore.workspaces" :key="ws.id" class="flex flex-col">
            <div 
              @click="handleWorkspaceToggle(ws.id)"
              class="group flex items-center justify-between px-3 py-1.5 rounded-lg cursor-pointer hover:bg-slate-150/50 dark:hover:bg-slate-800/60 transition-colors text-sm font-medium text-slate-700 dark:text-slate-300 hover:text-slate-950 dark:hover:text-white"
            >
              <div class="flex items-center gap-2 overflow-hidden">
                <Building2 class="w-4 h-4 text-indigo-400 shrink-0" />
                <span class="truncate" :title="ws.name">{{ ws.name }}</span>
              </div>
              <div class="flex items-center">
                <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 mr-1">
                  <button @click.stop="handleCreateProject(ws.id)" class="hover:bg-slate-200 dark:hover:bg-slate-700 p-0.5 rounded transition-colors text-slate-400 dark:text-slate-500 hover:text-slate-800 dark:hover:text-white border-none cursor-pointer bg-transparent" title="새 프로젝트 추가">
                    <Plus class="w-3 h-3" />
                  </button>
                  <button @click.stop="handleDeleteWorkspace(ws.id, ws.name)" class="hover:bg-red-500/10 dark:hover:bg-red-500/20 p-0.5 rounded transition-colors text-slate-400 dark:text-slate-500 hover:text-red-600 dark:hover:text-red-400 border-none cursor-pointer bg-transparent" title="워크스페이스 삭제">
                    <Trash class="w-3 h-3" />
                  </button>
                </div>
                <div class="text-slate-450 dark:text-slate-500 group-hover:text-slate-700 dark:group-hover:text-slate-300 flex items-center">
                  <Loader2 v-if="ws.isLoading" class="w-3.5 h-3.5 animate-spin text-indigo-500 dark:text-indigo-400" />
                  <template v-else>
                    <ChevronDown v-if="ws.isOpen" class="w-3.5 h-3.5" />
                    <ChevronRight v-else class="w-3.5 h-3.5" />
                  </template>
                </div>
              </div>
            </div>

            <!-- 2단계: 프로젝트 루프 (워크스페이스 오픈 시) -->
            <div 
              v-if="ws.isOpen && ws.projects.length > 0" 
              class="pl-4 flex flex-col gap-1 mt-0.5 border-l border-slate-200 dark:border-slate-800 ml-5"
            >
              <div v-for="proj in ws.projects" :key="proj.id" class="flex flex-col">
                <div 
                  @click="handleProjectToggle(ws.id, proj.id)"
                  class="group flex items-center justify-between px-2.5 py-1.5 rounded-lg cursor-pointer hover:bg-slate-150/30 dark:hover:bg-slate-800/40 transition-colors text-xs font-medium text-slate-550 dark:text-slate-400 hover:text-slate-950 dark:hover:text-white"
                >
                  <div class="flex items-center gap-2 overflow-hidden">
                    <Folder class="w-3.5 h-3.5 text-amber-500 shrink-0" />
                    <span class="truncate" :title="proj.name">{{ proj.name }}</span>
                  </div>
                  <div class="flex items-center">
                    <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 mr-1">
                      <button @click.stop="handleCreateBoard(ws.id, proj.id)" class="hover:bg-slate-200 dark:hover:bg-slate-700 p-0.5 rounded transition-colors text-slate-400 dark:text-slate-500 hover:text-slate-800 dark:hover:text-white border-none cursor-pointer bg-transparent" title="새 보드 추가">
                        <Plus class="w-3 h-3" />
                      </button>
                      <button @click.stop="handleDeleteProject(ws.id, proj.id, proj.name)" class="hover:bg-red-500/10 dark:hover:bg-red-500/20 p-0.5 rounded transition-colors text-slate-400 dark:text-slate-500 hover:text-red-600 dark:hover:text-red-405 border-none cursor-pointer bg-transparent" title="프로젝트 삭제">
                        <Trash class="w-3 h-3" />
                      </button>
                    </div>
                    <div class="text-slate-500 dark:text-slate-600 group-hover:text-slate-700 dark:group-hover:text-slate-400 flex items-center">
                      <Loader2 v-if="proj.isLoading" class="w-3 h-3 animate-spin text-indigo-500 dark:text-indigo-400" />
                      <template v-else>
                        <ChevronDown v-if="proj.isOpen" class="w-3 h-3" />
                        <ChevronRight v-else class="w-3 h-3" />
                      </template>
                    </div>
                  </div>
                </div>

                <!-- 3단계: 보드 루프 (프로젝트 오픈 시) -->
                <div 
                  v-if="proj.isOpen && proj.boards.length > 0" 
                  class="pl-3.5 flex flex-col gap-0.5 mt-0.5 border-l border-slate-200 dark:border-slate-800 ml-4"
                >
                  <div 
                    v-for="board in proj.boards" 
                    :key="board.id"
                    @click="navigateToBoard(board.id)"
                    class="group/board flex items-center justify-between px-2.5 py-1.5 rounded-lg cursor-pointer hover:bg-indigo-50 dark:hover:bg-indigo-600/20 hover:text-indigo-600 dark:hover:text-indigo-200 transition-all text-[11px] font-medium text-slate-550 dark:text-slate-400"
                    :class="route.params.id === board.id ? 'bg-indigo-100/50 dark:bg-indigo-600/30 text-indigo-600 dark:text-indigo-300 font-bold shadow-inner' : ''"
                  >
                    <div class="flex items-center gap-2 overflow-hidden">
                      <Trello class="w-3 h-3 text-indigo-500 dark:text-indigo-400 shrink-0" />
                      <span class="truncate" :title="board.name">{{ board.name }}</span>
                    </div>
                    <div class="opacity-0 group-hover/board:opacity-100 flex items-center">
                      <button @click.stop="handleDeleteBoard(ws.id, proj.id, board.id, board.name)" class="hover:bg-red-500/10 dark:hover:bg-red-500/20 p-0.5 rounded transition-colors text-slate-400 dark:text-slate-500 hover:text-red-600 dark:hover:text-red-400 border-none cursor-pointer bg-transparent" title="보드 삭제">
                        <Trash class="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="p-4 border-t border-slate-200/80 dark:border-slate-800">
      <div 
        @click="emit('openSettings')"
        class="flex items-center px-3 py-2 rounded-lg cursor-pointer transition-colors text-sm font-medium hover:bg-slate-150/40 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
      >
        <Settings class="w-5 h-5 shrink-0" />
        <span v-if="isOpen" class="ml-3">Settings</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
/* 라이트 모드 기본 */
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
/* 다크 모드 동적 덮어쓰기 */
:deep(.dark) .custom-scrollbar::-webkit-scrollbar-thumb,
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #334155;
}
:deep(.dark) .custom-scrollbar::-webkit-scrollbar-thumb:hover,
.dark .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #475569;
}
</style>

