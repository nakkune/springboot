<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore' // [NEW] 워크스페이스 멤버 초대 스토어 연계
import { Search, Bell, Plus, LogOut, User, Sparkles, X, Loader2 } from 'lucide-vue-next' // [NEW] 필수 아이콘 임포트 추가
import NotificationDropdown from './NotificationDropdown.vue' // [NEW] 실시간 알림 드롭다운 연동

const router = useRouter()
const workspaceStore = useWorkspaceStore()

// 로컬스토리지에서 로그인 세션 정보 추출
const userName = ref('관리자')
const userEmail = ref('admin@example.com')
const avatarUrl = ref('bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500')
const showDropdown = ref(false)

// [NEW] 팀원 초대용 상태 변수들
const isInviteModalOpen = ref(false)
const inviteEmail = ref('')
const isInviting = ref(false)
const inviteError = ref('')

const isPresetAvatar = (url: string) => {
  return url && url.startsWith('bg-gradient-to-tr')
}

const updateSessionData = () => {
  const savedName = localStorage.getItem('userName')
  const savedEmail = localStorage.getItem('userEmail')
  const savedAvatar = localStorage.getItem('avatarUrl')
  if (savedName) userName.value = savedName
  if (savedEmail) userEmail.value = savedEmail
  if (savedAvatar) avatarUrl.value = savedAvatar
}

onMounted(() => {
  updateSessionData()
  // 프로필 설정(ProfileView)에서 정보가 변경되면 리로드 없이 실시간 전역 반응형 동기화를 윈도우 이벤트를 통해 결합
  window.addEventListener('profile-updated', updateSessionData)
})

onUnmounted(() => {
  window.removeEventListener('profile-updated', updateSessionData)
})

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

// 로그아웃 비즈니스 로직
const handleLogout = () => {
  localStorage.clear() // 세션 비우기
  router.push('/login') // 로그인 화면으로 강제 이동
}

// [NEW] 워크스페이스 신규 팀원 초대 E2E API 동기화 핸들러
const handleInviteSubmit = async () => {
  if (!inviteEmail.value.trim()) return
  
  isInviting.value = true
  inviteError.value = ''
  try {
    const workspaceId = workspaceStore.workspaces.length > 0 ? workspaceStore.workspaces[0].id : ''
    if (!workspaceId) {
      throw new Error('선택된 워크스페이스가 없습니다.')
    }
    const newMember = await workspaceStore.inviteWorkspaceMember(workspaceId, inviteEmail.value.trim())
    
    isInviteModalOpen.value = false
    inviteEmail.value = ''
    
    alert(`'${newMember.fullName}' 님이 워크스페이스 멤버로 성공적으로 초대 및 자동 가입되었습니다! 🚀`)
  } catch (err: any) {
    console.error(err)
    inviteError.value = err.response?.data?.error || '초대 전송에 실패했습니다. 이메일을 다시 확인해 주세요.'
  } finally {
    isInviting.value = false
  }
}
</script>

<template>
  <header class="h-14 bg-white dark:bg-slate-900 border-b border-slate-200/80 dark:border-slate-800 flex items-center justify-between px-6 z-20 select-none relative transition-colors duration-200">
    
    <!-- Search Bar (글래스모피즘 아웃라인 적용) -->
    <div class="flex-1 flex items-center max-w-md">
      <div class="relative w-full">
        <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none">
          <Search class="h-4 w-4 text-slate-500" />
        </div>
        <input 
          type="text" 
          class="block w-full pl-11 pr-3 py-1.5 border border-slate-200 dark:border-slate-800 rounded-xl leading-5 bg-slate-50 dark:bg-slate-950/40 text-slate-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-600 focus:outline-none focus:bg-white dark:focus:bg-slate-950/40 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 sm:text-xs transition-all duration-200" 
          placeholder="업무, 프로젝트, 문서 통합 검색..."
        >
      </div>
    </div>

    <!-- Actions & User Profile -->
    <div class="flex items-center gap-4 relative">
      
      <!-- 알림 드롭다운 컴포넌트 -->
      <NotificationDropdown />

      <!-- 구분 종선 -->
      <div class="w-px h-5 bg-slate-200 dark:bg-slate-800"></div>

      <!-- 사용자 정보 & 아바타 (클릭 시 로그아웃 드롭다운 연동) -->
      <div class="flex items-center gap-2.5 relative">
        <div v-if="userName" class="text-right flex flex-col justify-center">
          <span class="text-xs font-bold text-slate-800 dark:text-slate-200">{{ userName }}</span>
          <span class="text-[9px] text-slate-400 dark:text-slate-500 font-semibold leading-none mt-0.5">{{ userEmail }}</span>
        </div>

        <div 
          @click="toggleDropdown"
          class="w-8 h-8 rounded-full cursor-pointer flex items-center justify-center text-white text-xs font-black ring-2 ring-slate-200 dark:ring-slate-800 hover:ring-indigo-500 transition-all active:scale-95"
          :class="avatarUrl && isPresetAvatar(avatarUrl) ? avatarUrl : 'bg-indigo-600'"
          :style="avatarUrl && !isPresetAvatar(avatarUrl) ? { backgroundImage: `url(${avatarUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
        >
          <span v-if="!avatarUrl || isPresetAvatar(avatarUrl)">
            {{ userName ? userName[0].toUpperCase() : 'A' }}
          </span>
        </div>

        <!-- 미려한 다크 드롭다운 메뉴 (10년 차 시니어 디자인 팝업 애니메이션) -->
        <Transition name="dropdown-fade">
          <div 
            v-if="showDropdown" 
            class="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-xl dark:shadow-2xl p-2 z-50 flex flex-col gap-0.5 transition-colors duration-200"
          >
            <div class="px-3 py-2 flex flex-col mb-1.5 border-b border-slate-100 dark:border-slate-800/80">
              <span class="text-[10px] font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                <Sparkles class="w-3 h-3 text-indigo-400" />
                <span>My Workspace</span>
              </span>
            </div>

            <!-- 프로필 바로가기 (E2E 라우터 연계) -->
            <div 
              @click="router.push('/profile'); showDropdown = false"
              class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-slate-650 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white cursor-pointer transition-colors text-xs font-medium"
            >
              <User class="w-4 h-4 text-slate-400 dark:text-slate-500" />
              <span>프로필 설정</span>
            </div>

            <!-- 로그아웃 (실제 로컬 세션 리셋) -->
            <div 
              @click="handleLogout"
              class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-rose-500 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-500/10 cursor-pointer transition-colors text-xs font-bold"
            >
              <LogOut class="w-4 h-4" />
              <span>로그아웃</span>
            </div>
          </div>
        </Transition>
      </div>

    </div>

    <!-- 초대 프리미엄 글래스모피즘 모달 (10년 차 시니어 디자인) -->
    <div v-if="isInviteModalOpen" class="fixed inset-0 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center z-50 transition-opacity">
      <div class="w-[380px] bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col gap-5 transform scale-100 transition-all duration-200">
        <div class="flex justify-between items-center">
          <h4 class="text-base font-black text-slate-950 dark:text-white flex items-center gap-1.5">
            <Sparkles class="w-4 h-4 text-indigo-500 dark:text-indigo-400" />
            <span>새로운 팀원 초대</span>
          </h4>
          <button @click="isInviteModalOpen = false" class="p-1 rounded-lg hover:bg-slate-150 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-700 dark:hover:text-white transition-colors border-none cursor-pointer bg-transparent">
            <X class="w-4 h-4" />
          </button>
        </div>
        
        <p class="text-2xs text-slate-600 dark:text-slate-500 font-medium leading-relaxed">
          초대할 팀원의 이메일을 입력해 주세요. 등록되지 않은 이메일의 경우, 테스트 온보딩을 위해 **신규 계정(초기 비밀번호: 1234)**으로 실시간 자동 가입을 지원합니다.
        </p>

        <div v-if="inviteError" class="bg-rose-500/15 border border-rose-500/30 text-rose-400 text-2xs p-2.5 rounded-xl text-center font-bold">
          {{ inviteError }}
        </div>

        <form @submit.prevent="handleInviteSubmit" class="flex flex-col gap-4">
          <div class="flex flex-col gap-1.5">
            <label class="text-[9px] uppercase font-black tracking-wider text-slate-400 dark:text-slate-500 ml-1">이메일 주소</label>
            <input 
              v-model="inviteEmail" 
              type="email" 
              required
              class="w-full bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-slate-800 dark:text-white focus:outline-none focus:bg-white dark:focus:bg-slate-950/40 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-400 dark:placeholder-slate-700"
              placeholder="teammate@example.com"
            />
          </div>

          <button 
            type="submit" 
            :disabled="isInviting"
            class="w-full bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl py-2.5 text-xs font-bold shadow-lg shadow-indigo-600/10 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <Loader2 v-if="isInviting" class="w-3.5 h-3.5 animate-spin" />
            <span v-else>초대 발송</span>
          </button>
        </form>
      </div>
    </div>

  </header>
</template>

<style scoped>
/* 드롭다운 트랜지션 모션 */
.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.dropdown-fade-enter-from {
  opacity: 0;
  transform: translateY(10px) scale(0.95);
}
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}
</style>
