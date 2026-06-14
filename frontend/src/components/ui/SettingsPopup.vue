<!-- src/components/ui/SettingsPopup.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/services/api'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'
import { 
  X, 
  Moon, 
  Sun, 
  Globe, 
  User, 
  Mail, 
  Loader2, 
  Save, 
  Settings,
  Users,
  Trash2,
  Check,
  Sparkles
} from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  initialTab?: string
}>(), {
  initialTab: 'general'
})

const emit = defineEmits<{
  (e: 'close'): void
}>()

const activeTab = ref(props.initialTab)

// 사용자 세션 정보
const userName = ref('')
const userEmail = ref('')
const avatarUrl = ref('')

// 설정 상태
const systemTheme = ref(localStorage.getItem('theme') || 'dark')
const systemTimezone = ref(localStorage.getItem('timezone') || 'Asia/Seoul')
const isLoading = ref(true)
const isSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 멤버 관리 상태
const workspaceStore = useWorkspaceStore()
const selectedWorkspaceId = ref('')
const membersLoading = ref(false)
const inviteEmail = ref('')
const inviteRole = ref('member')

const isPresetAvatar = (url: string) => {
  return url && url.startsWith('bg-gradient-to-tr')
}

const timezoneOptions = [
  { value: 'Asia/Seoul', label: 'Asia/Seoul (UTC+09:00)' },
  { value: 'UTC', label: 'UTC' },
  { value: 'America/New_York', label: 'America/New_York (UTC-05:00)' },
  { value: 'Europe/London', label: 'Europe/London (UTC+00:00)' },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo (UTC+09:00)' }
]

onMounted(async () => {
  userName.value = localStorage.getItem('userName') || ''
  userEmail.value = localStorage.getItem('userEmail') || ''
  avatarUrl.value = localStorage.getItem('avatarUrl') || ''

  await workspaceStore.fetchWorkspaces()
  if (workspaceStore.workspaces.length > 0) {
    selectedWorkspaceId.value = workspaceStore.workspaces[0].id
    await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
  }

  isLoading.value = false
})

const handleWorkspaceChange = async () => {
  if (!selectedWorkspaceId.value) return
  membersLoading.value = true
  await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
  membersLoading.value = false
}

const deleteMember = async (memberId: string) => {
  if (!selectedWorkspaceId.value) return
  try {
    await workspaceStore.deleteWorkspaceMember(selectedWorkspaceId.value, memberId)
    await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
  } catch (e) {
    console.error('멤버 삭제 실패', e)
  }
}

const inviteMember = async () => {
  if (!selectedWorkspaceId.value || !inviteEmail.value) return
  try {
    await workspaceStore.inviteWorkspaceMember(selectedWorkspaceId.value, inviteEmail.value, inviteRole.value)
    await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
    inviteEmail.value = ''
    inviteRole.value = 'member'
  } catch (e) {
    console.error('멤버 초대 실패', e)
  }
}

const savePreferences = async () => {
  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  // 테마 적용
  localStorage.setItem('theme', systemTheme.value)
  const html = document.documentElement
  if (systemTheme.value === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }

  // 타임존 저장
  localStorage.setItem('timezone', systemTimezone.value)

  // 백엔드 동기화
  const userId = localStorage.getItem('userId')
  if (userId) {
    try {
      await api.put(`/users/${userId}`, {
        theme: systemTheme.value,
        timezone: systemTimezone.value
      })
    } catch (err) {
      console.error('설정 저장 실패:', err)
    }
  }

  window.dispatchEvent(new Event('profile-updated'))

  isSaving.value = false
  successMessage.value = '설정이 저장되었습니다.'
  setTimeout(() => { successMessage.value = '' }, 2500)
}
</script>

<template>
  <Transition name="popup-fade">
    <div class="fixed inset-0 z-[9999] flex items-center justify-center">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-sm" @click="emit('close')"></div>

      <!-- Modal -->
      <div class="relative w-full max-w-lg mx-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-2xl overflow-hidden transition-all duration-200">
        
        <!-- Gradient Header Bar -->
        <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-indigo-500 to-purple-600"></div>

        <!-- Header -->
        <div class="flex items-center justify-between px-6 pt-6 pb-4 border-b border-slate-100 dark:border-slate-800">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-lg bg-indigo-500/10 dark:bg-indigo-500/10 flex items-center justify-center">
              <Settings class="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
            </div>
            <div>
              <h2 class="text-sm font-black text-slate-900 dark:text-white tracking-tight">설정</h2>
              <p class="text-[10px] text-slate-500 dark:text-slate-400 font-semibold">환경 설정 및 팀 관리</p>
            </div>
          </div>
          <button 
            type="button"
            @click="emit('close')"
            class="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-800 dark:hover:text-white transition-colors cursor-pointer border-none bg-transparent"
            title="닫기"
          >
            <X class="w-4.5 h-4.5" />
          </button>
        </div>

        <!-- Tab Navigation -->
        <div class="flex border-b border-slate-200 dark:border-slate-800 px-6">
          <button
            @click="activeTab = 'general'"
            class="flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all cursor-pointer bg-transparent"
            :class="activeTab === 'general'
              ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'"
          >
            <Settings class="w-4 h-4" />
            <span>General</span>
          </button>
          <button
            @click="activeTab = 'members'"
            class="flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all cursor-pointer bg-transparent"
            :class="activeTab === 'members'
              ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'"
          >
            <Users class="w-4 h-4" />
            <span>Members</span>
          </button>
        </div>

        <!-- 로딩 -->
        <div v-if="isLoading" class="flex items-center justify-center py-10">
          <Loader2 class="w-6 h-6 animate-spin text-indigo-500" />
        </div>

        <!-- Content -->
        <div v-else class="p-6 flex flex-col gap-5 max-h-[70vh] overflow-y-auto custom-scrollbar">

          <!-- ============================== -->
          <!-- GENERAL TAB -->
          <!-- ============================== -->
          <template v-if="activeTab === 'general'">

            <!-- 에러/성공 메시지 -->
            <div v-if="errorMessage" class="bg-rose-500/15 border border-rose-500/30 text-rose-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium">
              {{ errorMessage }}
            </div>
            <div v-if="successMessage" class="bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium">
              {{ successMessage }}
            </div>

            <!-- ===== Profile Section ===== -->
            <div class="flex flex-col gap-3 bg-slate-50/50 dark:bg-slate-950/30 border border-slate-200 dark:border-slate-800 rounded-2xl p-4">
              <span class="text-[10px] uppercase font-bold tracking-wider text-slate-500 flex items-center gap-1.5">
                <User class="w-3 h-3" />
                <span>내 프로필</span>
              </span>
              <div class="flex items-center gap-3.5">
                <div 
                  class="w-12 h-12 rounded-full flex items-center justify-center text-white text-base font-black shrink-0 ring-2 ring-slate-200 dark:ring-slate-700"
                  :class="avatarUrl && isPresetAvatar(avatarUrl) ? avatarUrl : 'bg-indigo-600'"
                  :style="avatarUrl && !isPresetAvatar(avatarUrl) ? { backgroundImage: `url(${avatarUrl.startsWith('/api') ? 'http://localhost:9090' + avatarUrl : avatarUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
                >
                  <span v-if="!avatarUrl || isPresetAvatar(avatarUrl)">
                    {{ userName ? userName[0].toUpperCase() : 'P' }}
                  </span>
                </div>
                <div class="flex flex-col gap-0.5">
                  <span class="text-sm font-bold text-slate-900 dark:text-slate-100">{{ userName || '사용자' }}</span>
                  <span class="text-[11px] text-slate-500 dark:text-slate-400 font-medium flex items-center gap-1">
                    <Mail class="w-3 h-3" />
                    {{ userEmail || '이메일 없음' }}
                  </span>
                </div>
              </div>
            </div>

            <!-- ===== Theme Section ===== -->
            <div class="flex flex-col gap-2">
              <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 flex items-center gap-1.5">
                <Moon class="w-3 h-3" />
                <span>테마</span>
              </label>
              <div class="grid grid-cols-2 gap-3 bg-slate-50/50 dark:bg-slate-950/30 border border-slate-200 dark:border-slate-800 rounded-2xl p-3">
                <label 
                  class="flex items-center gap-3 border border-slate-200 dark:border-slate-800 rounded-xl px-3.5 py-3 cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800/20 transition-all select-none"
                  :class="{ 'ring-1 ring-indigo-500 border-indigo-500/50 bg-indigo-50/50 dark:bg-indigo-500/5': systemTheme === 'dark' }"
                >
                  <input type="radio" value="dark" v-model="systemTheme" class="hidden" />
                  <Moon class="w-4.5 h-4.5 text-indigo-500 shrink-0" />
                  <div class="flex flex-col">
                    <span class="text-xs font-bold text-slate-800 dark:text-slate-200">다크</span>
                    <span class="text-[9px] text-slate-500 font-medium">어두운 테마</span>
                  </div>
                </label>
                <label 
                  class="flex items-center gap-3 border border-slate-200 dark:border-slate-800 rounded-xl px-3.5 py-3 cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800/20 transition-all select-none"
                  :class="{ 'ring-1 ring-indigo-500 border-indigo-500/50 bg-indigo-50/50 dark:bg-indigo-500/5': systemTheme === 'light' }"
                >
                  <input type="radio" value="light" v-model="systemTheme" class="hidden" />
                  <Sun class="w-4.5 h-4.5 text-amber-500 shrink-0" />
                  <div class="flex flex-col">
                    <span class="text-xs font-bold text-slate-800 dark:text-slate-200">라이트</span>
                    <span class="text-[9px] text-slate-500 font-medium">밝은 테마</span>
                  </div>
                </label>
              </div>
            </div>

            <!-- ===== Timezone Section ===== -->
            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 flex items-center gap-1.5">
                <Globe class="w-3 h-3" />
                <span>시간대</span>
              </label>
              <div class="relative">
                <Globe class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
                <select 
                  v-model="systemTimezone"
                  class="w-full bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-xs text-slate-800 dark:text-white focus:outline-none focus:bg-white dark:focus:bg-slate-950/40 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all appearance-none cursor-pointer"
                >
                  <option 
                    v-for="opt in timezoneOptions" 
                    :key="opt.value" 
                    :value="opt.value"
                    class="bg-white dark:bg-slate-900 text-slate-800 dark:text-white"
                  >
                    {{ opt.label }}
                  </option>
                </select>
              </div>
            </div>

            <!-- Footer (General) -->
            <div class="pt-2">
              <button 
                @click="savePreferences"
                :disabled="isSaving"
                class="w-full bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl py-2.5 text-xs font-bold shadow-lg shadow-indigo-600/10 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50 border-none cursor-pointer"
              >
                <Loader2 v-if="isSaving" class="w-4 h-4 animate-spin" />
                <Save v-else class="w-4 h-4" />
                <span>설정 저장</span>
              </button>
            </div>

          </template>

          <!-- ============================== -->
          <!-- MEMBERS TAB -->
          <!-- ============================== -->
          <template v-if="activeTab === 'members'">

            <!-- 대상 워크스페이스 선택 -->
            <div class="flex flex-col gap-1.5">
              <span class="text-[10px] uppercase font-bold tracking-wider text-slate-500 flex items-center gap-1.5">
                <Users class="w-3 h-3" />
                <span>워크스페이스</span>
              </span>
              <select 
                v-model="selectedWorkspaceId" 
                @change="handleWorkspaceChange"
                class="w-full bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800 rounded-xl px-4 py-2.5 text-xs text-slate-800 dark:text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all appearance-none cursor-pointer"
              >
                <option v-for="ws in workspaceStore.workspaces" :key="ws.id" :value="ws.id">
                  {{ ws.name }}
                </option>
              </select>
            </div>

            <!-- 초대 폼 -->
            <div class="flex flex-col gap-2 bg-slate-50/50 dark:bg-slate-950/30 border border-slate-200 dark:border-slate-800 rounded-2xl p-4">
              <span class="text-[10px] uppercase font-bold tracking-wider text-slate-500 flex items-center gap-1.5">
                <Mail class="w-3 h-3" />
                <span>멤버 초대</span>
              </span>
              <div class="flex gap-2">
                <input v-model="inviteEmail" type="email" placeholder="Email address" class="flex-1 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-slate-800 dark:text-white focus:outline-none focus:border-indigo-500 transition-all" />
                <select v-model="inviteRole" class="bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl px-3 py-2.5 text-xs text-slate-800 dark:text-white focus:outline-none focus:border-indigo-500 transition-all appearance-none cursor-pointer">
                  <option value="member">Member</option>
                  <option value="admin">Admin</option>
                </select>
                <button @click="inviteMember" class="bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl px-4 py-2.5 text-xs font-bold flex items-center gap-1.5 transition-all border-none cursor-pointer shadow-md">
                  <Check class="w-3.5 h-3.5" />
                  Invite
                </button>
              </div>
            </div>

            <!-- 멤버 목록 -->
            <div class="flex flex-col gap-2">
              <span class="text-[10px] uppercase font-bold tracking-wider text-slate-500 flex items-center gap-1.5">
                <Users class="w-3 h-3" />
                <span>멤버 목록</span>
              </span>

              <div v-if="membersLoading" class="flex items-center justify-center py-6">
                <Loader2 class="w-5 h-5 animate-spin text-indigo-500" />
              </div>
              <div v-else-if="workspaceStore.currentWorkspaceMembers.length === 0" class="text-center text-xs text-slate-500 py-6">
                멤버가 없습니다.
              </div>
              <ul v-else class="flex flex-col gap-1.5">
                <li v-for="member in workspaceStore.currentWorkspaceMembers" :key="member.id" 
                  class="flex items-center justify-between px-3.5 py-2.5 rounded-xl bg-slate-50/50 dark:bg-slate-950/30 border border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800/30 transition-colors">
                  <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-full bg-indigo-500/10 dark:bg-indigo-500/20 flex items-center justify-center text-indigo-600 dark:text-indigo-400 text-xs font-bold">
                      {{ member.fullName ? member.fullName[0].toUpperCase() : '?' }}
                    </div>
                    <div>
                      <div class="text-xs font-semibold text-slate-800 dark:text-slate-200">{{ member.fullName }}</div>
                      <div class="text-[10px] text-slate-500 dark:text-slate-400">{{ member.email }} 
                        <span class="text-indigo-500 dark:text-indigo-400 font-semibold">({{ member.role }})</span>
                      </div>
                    </div>
                  </div>
                  <button @click="deleteMember(member.id)" class="p-1.5 rounded-lg hover:bg-red-500/10 text-slate-400 hover:text-red-500 transition-colors border-none cursor-pointer bg-transparent" title="멤버 삭제">
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </li>
              </ul>
            </div>

          </template>

        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* 드롭다운/셀렉터 기본 스타일링 */
select {
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%2394a3b8' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 0.75rem center;
  background-repeat: no-repeat;
  background-size: 1.25em 1.25em;
  padding-right: 2.2rem;
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}

.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.popup-fade-enter-from {
  opacity: 0;
}
.popup-fade-enter-from > div {
  transform: scale(0.95) translateY(8px);
}
.popup-fade-leave-to {
  opacity: 0;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #334155;
}
</style>
