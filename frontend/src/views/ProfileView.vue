<!-- src/views/ProfileView.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { User, Lock, Mail, Globe, Sparkles, Loader2, Save, KeyRound, X } from 'lucide-vue-next'

const router = useRouter()

// 사용자 폼 데이터 상태
const userId = ref('')
const email = ref('')
const fullName = ref('')
const timezone = ref('Asia/Seoul')
const avatarUrl = ref('bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500')

// 파일 업로드 관련 상태 (10년 차 시니어 프리미엄 업로드 UX 설계)
const fileInputRef = ref<HTMLInputElement | null>(null)
const isUploadingAvatar = ref(false)

// 패스워드 변경을 위한 상태
const changePassword = ref(false)
const newPassword = ref('')
const confirmNewPassword = ref('')

// 공통 상태
const isLoading = ref(true)
const isSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 10년 차 시니어 디자이너가 큐레이션한 프리미엄 그라데이션 아바타 프리셋 6종
const avatarPresets = [
  'bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500',
  'bg-gradient-to-tr from-cyan-400 to-blue-600',
  'bg-gradient-to-tr from-emerald-400 to-teal-700',
  'bg-gradient-to-tr from-orange-400 to-rose-600',
  'bg-gradient-to-tr from-fuchsia-600 to-pink-500',
  'bg-gradient-to-tr from-violet-600 to-indigo-600'
]

// 타임존 옵션
const timezoneOptions = [
  { value: 'Asia/Seoul', label: 'Asia/Seoul (한국 표준시 - UTC+09)' },
  { value: 'UTC', label: 'UTC (협정 세계시)' },
  { value: 'America/New_York', label: 'America/New_York (미 동부 표준시 - UTC-05)' },
  { value: 'Europe/London', label: 'Europe/London (런던 그리니치 - UTC+00)' },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo (도쿄 표준시 - UTC+09)' }
]

// 현재 아바타 그라데이션이 프리셋인지 확인하는 헬퍼
const isPresetAvatar = (url: string) => {
  return avatarPresets.includes(url)
}

// 1. 내 정보 실시간 DB 데이터 로드
const loadUserProfile = async () => {
  const savedId = localStorage.getItem('userId')
  if (!savedId) {
    errorMessage.value = '로그인 세션 만료. 다시 로그인해 주세요.'
    router.push('/login')
    return
  }
  userId.value = savedId

  try {
    isLoading.value = true
    const response = await api.get(`/users/${savedId}`) as any
    if (response) {
      email.value = response.email
      fullName.value = response.fullName
      timezone.value = response.timezone || 'Asia/Seoul'
      avatarUrl.value = response.avatarUrl || 'bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500'
    }
  } catch (err: any) {
    console.error('프로필 정보 로드 실패:', err)
    errorMessage.value = '내 정보를 로드하는 데 실패했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadUserProfile()
})

// 프리셋 아바타 선택 시 핸들러
const selectAvatar = (preset: string) => {
  avatarUrl.value = preset
}

// 2. 프로필 수정 저장
const handleUpdateProfile = async () => {
  if (!fullName.value.trim()) {
    errorMessage.value = '사용자 이름은 공백일 수 없습니다.'
    return
  }

  // 패스워드 변경을 원할 경우
  if (changePassword.value) {
    if (!newPassword.value) {
      errorMessage.value = '변경할 새 비밀번호를 입력해 주세요.'
      return
    }
    if (newPassword.value.length < 8) {
      errorMessage.value = '비밀번호는 최소 8자리 이상이어야 합니다.'
      return
    }
    if (newPassword.value !== confirmNewPassword.value) {
      errorMessage.value = '새 비밀번호 재확인이 일치하지 않습니다.'
      return
    }
  }

  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const updatePayload: any = {
      fullName: fullName.value.trim(),
      avatarUrl: avatarUrl.value,
      timezone: timezone.value
    }

    if (changePassword.value && newPassword.value) {
      updatePayload.passwordHash = newPassword.value // 백엔드 단에서 자동으로 BCrypt 해싱 수행
    }

    const response = await api.put(`/users/${userId.value}`, updatePayload) as any

    if (response) {
      // 3. 실시간 로컬 세션 동기화
      localStorage.setItem('userName', response.fullName)
      localStorage.setItem('avatarUrl', response.avatarUrl || '')

      successMessage.value = '프로필 정보가 성공적으로 저장되었습니다! 🚀'
      
      // 스크립트 상태 갱신
      changePassword.value = false
      newPassword.value = ''
      confirmNewPassword.value = ''
      
      // 탑 헤더 등에 즉시 세션 갱신 신호를 주기 위해 강제 커스텀 이벤트 디스패치
      window.dispatchEvent(new Event('profile-updated'))
      
      // 1초 뒤에 알림 리셋
      setTimeout(() => {
        successMessage.value = ''
      }, 3000)
    }
  } catch (err: any) {
    console.error('프로필 저장 실패:', err)
    errorMessage.value = err.response?.data?.error || '프로필 정보 업데이트에 실패했습니다.'
  } finally {
    isSaving.value = false
  }
}

// 닫기 및 이전 화면 회귀 핸들러 (10년 차 시니어 라우터 히스토리 폴백 설계)
const handleClose = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/dashboard')
  }
}

// 개인 사진 업로드 트리거 및 E2E 통신 파일 적재 핸들러
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleAvatarUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return

  const file = input.files[0]
  
  // 5MB 용량 가드 제한 (B2B 성능 최적화)
  if (file.size > 5 * 1024 * 1024) {
    alert('개인 사진은 최대 5MB 이하의 이미지 파일만 업로드 가능합니다.')
    return
  }

  isUploadingAvatar.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)

    // 새로 신설한 독립형 업로드 API 호출
    const response = await api.post('/attachments/upload/profile', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }) as any

    if (response && response.url) {
      avatarUrl.value = response.url
      successMessage.value = '개인 사진이 서버에 적재되었습니다. 하단의 [저장] 단추를 누르면 최종 확정됩니다. 📸'
      setTimeout(() => { successMessage.value = '' }, 3000)
    }
  } catch (err: any) {
    console.error('프로필 사진 파일 업로드 연동 에러:', err)
    errorMessage.value = '사진 파일을 업로드하는 데 실패했습니다. 이미지 파일인지 확인해 주세요.'
  } finally {
    isUploadingAvatar.value = false
    input.value = '' // 인풋 초기화
  }
}
</script>

<template>
  <div class="flex-1 w-full h-full bg-slate-950 p-6 sm:p-10 overflow-y-auto flex justify-center items-start text-sm select-none">
    
    <!-- 오로라 백그라운드 효과 (글래스모피즘 입체감 증진) -->
    <div class="absolute top-10 left-1/3 w-80 h-80 bg-indigo-500/5 rounded-full blur-[80px]"></div>
    <div class="absolute bottom-10 right-1/3 w-80 h-80 bg-purple-500/5 rounded-full blur-[80px]"></div>

    <!-- 로딩 스피너 -->
    <div v-if="isLoading" class="flex flex-col items-center gap-3 my-20">
      <Loader2 class="w-8 h-8 animate-spin text-indigo-500" />
      <span class="text-xs text-slate-500 font-bold tracking-wider">안전하게 프로필 정보를 불러오는 중...</span>
    </div>

    <!-- 메인 프로필 패널 -->
    <div v-else class="relative w-full max-w-[620px] bg-slate-900/60 backdrop-blur-xl border border-slate-800/80 rounded-3xl p-6 sm:p-8 shadow-2xl z-10 flex flex-col gap-6">
      
      <!-- 패널 헤더 (우측 상단 닫기 X 버튼 결합) -->
      <div class="flex items-center justify-between border-b border-slate-800 pb-4">
        <h2 class="text-lg font-black text-white tracking-tight flex items-center gap-2">
          <Sparkles class="w-4 h-4 text-indigo-400" />
          <span>개인 프로필 설정</span>
        </h2>
        <button 
          type="button"
          @click="handleClose"
          class="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors cursor-pointer"
          title="닫기"
        >
          <X class="w-4.5 h-4.5" />
        </button>
      </div>

      <!-- 에러 및 성공 메시지 박스 -->
      <div 
        v-if="errorMessage" 
        class="bg-rose-500/15 border border-rose-500/30 text-rose-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium transition-all"
      >
        {{ errorMessage }}
      </div>

      <div 
        v-if="successMessage" 
        class="bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium transition-all"
      >
        {{ successMessage }}
      </div>

      <form @submit.prevent="handleUpdateProfile" class="flex flex-col gap-5">
        
        <!-- 아바타 설정 그리드 (10년 차 프리미엄 그라데이션 및 사진 업로드 설계) -->
        <div class="flex flex-col gap-2">
          <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 ml-1">프로필 아바타</label>
          <div class="flex items-center gap-6 bg-slate-950/35 border border-slate-800/60 p-4 rounded-2xl">
            
            <!-- 숨겨진 파일 선택기 -->
            <input 
              type="file" 
              ref="fileInputRef" 
              accept="image/*" 
              class="hidden" 
              @change="handleAvatarUpload" 
            />

            <!-- 대형 아바타 프리뷰 (개인 사진일 경우 백엔드 포트 localhost:9090 매핑 렌더링) -->
            <div 
              class="w-16 h-16 rounded-full flex items-center justify-center text-white text-2xl font-black shadow-xl shrink-0 ring-2 ring-slate-800 relative overflow-hidden group/avatarpreview"
              :class="avatarUrl && isPresetAvatar(avatarUrl) ? avatarUrl : 'bg-indigo-600'"
              :style="avatarUrl && !isPresetAvatar(avatarUrl) ? { backgroundImage: `url(${avatarUrl.startsWith('/api') ? 'http://localhost:9090' + avatarUrl : avatarUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
            >
              <span v-if="(!avatarUrl || isPresetAvatar(avatarUrl)) && !isUploadingAvatar">
                {{ fullName ? fullName[0].toUpperCase() : 'A' }}
              </span>
              
              <!-- 사진 업로드 중 흐린 딤 처리 오버레이 -->
              <div v-if="isUploadingAvatar" class="absolute inset-0 bg-slate-950/75 flex items-center justify-center">
                <Loader2 class="w-5 h-5 animate-spin text-indigo-400" />
              </div>
            </div>
            
            <!-- 프리셋 리스트 그리드 -->
            <div class="flex flex-col gap-2.5 w-full">
              <div class="flex items-center justify-between">
                <span class="text-[10px] font-bold text-slate-500">사진 등록 또는 프리셋 선택</span>
                <button
                  type="button"
                  @click="triggerFileInput"
                  :disabled="isUploadingAvatar"
                  class="bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-400 hover:text-indigo-300 rounded-lg px-2.5 py-1 text-[9px] font-black transition-colors cursor-pointer border border-indigo-500/30 disabled:opacity-50 disabled:pointer-events-none"
                >
                  개인사진 업로드
                </button>
              </div>
              <div class="flex items-center flex-wrap gap-2.5">
                <button
                  v-for="(preset, i) in avatarPresets"
                  :key="i"
                  type="button"
                  @click="selectAvatar(preset)"
                  class="w-8 h-8 rounded-full cursor-pointer hover:scale-108 active:scale-95 transition-all ring-offset-2 ring-offset-slate-900 border-none"
                  :class="[
                    preset,
                    avatarUrl === preset ? 'ring-2 ring-indigo-500' : 'ring-1 ring-slate-800'
                  ]"
                />
              </div>
            </div>
          </div>

          <!-- 커스텀 아바타 이미지 주소 입력 옵션 -->
          <div class="flex flex-col gap-1.5 mt-1">
            <span class="text-[10px] font-bold text-slate-500 ml-1">또는 외부 이미지 주소 URL 사용</span>
            <input
              v-model="avatarUrl"
              type="text"
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-700"
              placeholder="https://example.com/avatar.png"
            />
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <!-- 이메일 정보 (계정 고유 식별자로서 수정 불가 안내) -->
          <div class="flex flex-col gap-1.5">
            <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 ml-1">이메일 (수정 불가)</label>
            <div class="relative flex items-center">
              <Mail class="absolute left-3.5 w-4 h-4 text-slate-600" />
              <input 
                v-model="email" 
                type="email" 
                disabled
                class="w-full bg-slate-950/30 border border-slate-900 rounded-xl pl-11 pr-4 py-2.5 text-xs text-slate-500 cursor-not-allowed outline-none select-none"
              />
            </div>
          </div>

          <!-- 이름 입력 -->
          <div class="flex flex-col gap-1.5">
            <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 ml-1">사용자 이름</label>
            <div class="relative flex items-center">
              <User class="absolute left-3.5 w-4 h-4 text-slate-500" />
              <input 
                v-model="fullName" 
                type="text" 
                required
                class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-600"
                placeholder="사용자 실명"
              />
            </div>
          </div>
        </div>

        <!-- 타임존 정보 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 ml-1">국가/시간대 (Timezone)</label>
          <div class="relative flex items-center">
            <Globe class="absolute left-3.5 w-4 h-4 text-slate-500 pointer-events-none" />
            <select 
              v-model="timezone"
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all appearance-none cursor-pointer"
            >
              <option 
                v-for="opt in timezoneOptions" 
                :key="opt.value" 
                :value="opt.value"
                class="bg-slate-900 text-white"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>
        </div>

        <!-- 비밀번호 변경 아코디언 패널 (10년 차 시니어 설계) -->
        <div class="border border-slate-800 rounded-2xl bg-slate-950/20 overflow-hidden">
          <div 
            @click="changePassword = !changePassword"
            class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-slate-800/20 transition-colors"
          >
            <div class="flex items-center gap-2 text-slate-400">
              <KeyRound class="w-4 h-4 text-indigo-400" />
              <span class="text-xs font-bold">비밀번호 변경</span>
            </div>
            <span class="text-[10px] font-bold" :class="changePassword ? 'text-indigo-400' : 'text-slate-600'">
              {{ changePassword ? '접기 ▲' : '열기 ▼' }}
            </span>
          </div>

          <transition
            enter-active-class="transition-all duration-300 ease-out"
            leave-active-class="transition-all duration-200 ease-in"
            enter-from-class="max-h-0 opacity-0"
            enter-to-class="max-h-[200px] opacity-100"
            leave-from-class="max-h-[200px] opacity-100"
            leave-to-class="max-h-0 opacity-0"
          >
            <div v-if="changePassword" class="px-4 pb-4 flex flex-col gap-3.5 border-t border-slate-950/50">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-3">
                <div class="flex flex-col gap-1">
                  <span class="text-[9px] font-bold text-slate-500 ml-1">새 비밀번호 (8자리 이상)</span>
                  <input
                    v-model="newPassword"
                    type="password"
                    class="w-full bg-slate-950/40 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-800"
                    placeholder="••••••••"
                  />
                </div>
                <div class="flex flex-col gap-1">
                  <span class="text-[9px] font-bold text-slate-500 ml-1">새 비밀번호 확인</span>
                  <input
                    v-model="confirmNewPassword"
                    type="password"
                    class="w-full bg-slate-950/40 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-800"
                    placeholder="••••••••"
                  />
                </div>
              </div>
            </div>
          </transition>
        </div>

        <!-- 저장 단추 -->
        <button 
          type="submit" 
          :disabled="isSaving"
          class="w-full bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl py-3 text-xs font-bold shadow-lg shadow-indigo-600/10 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:pointer-events-none mt-2"
        >
          <Loader2 v-if="isSaving" class="w-4 h-4 animate-spin text-white" />
          <Save v-else class="w-4 h-4 text-white" />
          <span>프로필 설정 저장</span>
        </button>

      </form>

    </div>

  </div>
</template>

<style scoped>
/* 드롭다운/셀렉터 기본 스타일링 */
select {
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%2394a3b8' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 0.75rem center;
  background-repeat: no-repeat;
  background-size: 1.25em 1.25em;
  padding-right: 2.5rem;
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}
</style>
