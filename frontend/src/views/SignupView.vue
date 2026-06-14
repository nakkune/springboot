<!-- src/views/SignupView.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { Lock, Mail, User, Loader2, Sparkles, ArrowRight } from 'lucide-vue-next'

const router = useRouter()
const email = ref('')
const fullName = ref('')
const password = ref('')
const confirmPassword = ref('')
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 이메일 정규식 유효성 검사
const validateEmail = (emailStr: string) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailStr)
}

const handleSignup = async () => {
  // 1. 유효성 검사
  if (!email.value || !fullName.value || !password.value || !confirmPassword.value) {
    errorMessage.value = '모든 필드를 빠짐없이 입력해 주세요.'
    return
  }
  
  if (!validateEmail(email.value)) {
    errorMessage.value = '올바른 이메일 형식이 아닙니다.'
    return
  }

  if (fullName.value.trim().length < 2) {
    errorMessage.value = '이름은 최소 2글자 이상 입력해 주세요.'
    return
  }

  if (password.value.length < 8) {
    errorMessage.value = '비밀번호는 안전을 위해 최소 8자리 이상 입력해 주세요.'
    return
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = '비밀번호가 일치하지 않습니다. 다시 확인해 주세요.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    // 2. 백엔드 회원 생성 API 호출 (POST /users)
    // passwordHash 필드에 평문 패스워드를 전송하면 백엔드 서비스단에서 안전하게 BCrypt 처리합니다.
    await api.post('/users', {
      email: email.value.trim(),
      fullName: fullName.value.trim(),
      passwordHash: password.value
    })

    successMessage.value = '회원가입이 완료되었습니다! 관리자의 가입 승인(Approved) 후 로그인이 활성화됩니다. 잠시 후 로그인 화면으로 이동합니다...'

    // 관리자 승인 대기 정책 도입에 따라 자동 로그인을 차단하고 로그인 화면으로 복귀 처리
    setTimeout(() => {
      router.push('/login')
    }, 3500)

  } catch (e: any) {
    console.error('회원가입 API 호출 실패 디버그 로그:', e)
    if (e.response && e.response.data && e.response.data.error) {
      errorMessage.value = e.response.data.error
    } else if (e.response && e.response.status === 409) {
      errorMessage.value = '이미 등록된 이메일 주소입니다.'
    } else {
      errorMessage.value = '회원가입 중 네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    }
    isLoading.value = false
  }
}
</script>

<template>
  <div class="relative min-h-screen w-full bg-slate-950 flex items-center justify-center overflow-hidden font-sans select-none">
    
    <!-- 배경 오로라 글로우 서클 1 (10년 차 시니어 디자인 관점의 비주얼 강화) -->
    <div class="absolute top-1/4 left-1/4 w-96 h-96 bg-indigo-500/10 rounded-full blur-[100px] animate-pulse duration-[6000ms]"></div>
    <!-- 배경 오로라 글로우 서클 2 -->
    <div class="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-[100px] animate-pulse duration-[8000ms]"></div>

    <!-- 회원가입 글래스모피즘 카드 -->
    <div class="relative w-[440px] bg-slate-900/60 backdrop-blur-xl border border-slate-800/80 rounded-3xl p-8 shadow-2xl z-10 flex flex-col gap-5">
      
      <!-- 헤더 로고 및 타이틀 -->
      <div class="text-center flex flex-col items-center gap-1.5">
        <div class="w-11 h-11 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-extrabold text-xl shadow-xl shadow-indigo-500/20">
          M
        </div>
        <h2 class="text-xl font-black tracking-tight text-white mt-2">
          MiniERP 회원가입
        </h2>
        <p class="text-2xs text-slate-500 font-medium leading-relaxed">
          간단한 정보 입력만으로 스마트한 워크 플랫폼의 오너가 되어 보세요.
        </p>
      </div>

      <!-- 메시지 박스 알림 (에러 / 성공) -->
      <div 
        v-if="errorMessage" 
        class="bg-rose-500/15 border border-rose-500/30 text-rose-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium"
      >
        {{ errorMessage }}
      </div>

      <div 
        v-if="successMessage" 
        class="bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium"
      >
        {{ successMessage }}
      </div>

      <!-- 회원가입 입력 폼 -->
      <form @submit.prevent="handleSignup" class="flex flex-col gap-3.5">
        
        <!-- 이메일 입력 -->
        <div class="flex flex-col gap-1">
          <label class="text-[9px] uppercase font-bold tracking-wider text-slate-500 ml-1">이메일 주소</label>
          <div class="relative flex items-center">
            <Mail class="absolute left-3.5 w-4 h-4 text-slate-500" />
            <input 
              v-model="email" 
              type="email" 
              required
              :disabled="isLoading"
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-700 disabled:opacity-50"
              placeholder="yourmail@example.com"
            />
          </div>
        </div>

        <!-- 이름(풀네임) 입력 -->
        <div class="flex flex-col gap-1">
          <label class="text-[9px] uppercase font-bold tracking-wider text-slate-500 ml-1">사용자 이름</label>
          <div class="relative flex items-center">
            <User class="absolute left-3.5 w-4 h-4 text-slate-500" />
            <input 
              v-model="fullName" 
              type="text" 
              required
              :disabled="isLoading"
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-700 disabled:opacity-50"
              placeholder="실명을 입력해 주세요"
            />
          </div>
        </div>

        <!-- 비밀번호 입력 -->
        <div class="flex flex-col gap-1">
          <label class="text-[9px] uppercase font-bold tracking-wider text-slate-500 ml-1">비밀번호 (최소 8자리)</label>
          <div class="relative flex items-center">
            <Lock class="absolute left-3.5 w-4 h-4 text-slate-500" />
            <input 
              v-model="password" 
              type="password" 
              required
              :disabled="isLoading"
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-700 disabled:opacity-50"
              placeholder="••••••••"
            />
          </div>
        </div>

        <!-- 비밀번호 확인 입력 -->
        <div class="flex flex-col gap-1">
          <label class="text-[9px] uppercase font-bold tracking-wider text-slate-500 ml-1">비밀번호 재확인</label>
          <div class="relative flex items-center">
            <Lock class="absolute left-3.5 w-4 h-4 text-slate-500" />
            <input 
              v-model="confirmPassword" 
              type="password" 
              required
              :disabled="isLoading"
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-700 disabled:opacity-50"
              placeholder="••••••••"
            />
          </div>
        </div>

        <!-- 가입 승인 실행 단추 -->
        <button 
          type="submit" 
          :disabled="isLoading"
          class="w-full mt-2 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl py-3 text-xs font-bold shadow-lg shadow-indigo-600/10 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:pointer-events-none"
        >
          <Loader2 v-if="isLoading" class="w-4 h-4 animate-spin text-white" />
          <span v-else class="flex items-center gap-1">
            가입 및 무료로 시작하기 <ArrowRight class="w-3.5 h-3.5" />
          </span>
        </button>
      </form>

      <!-- 로그인 화면 회귀 유도 구분선 -->
      <div class="h-px bg-slate-800/80 my-1"></div>

      <div class="text-center">
        <span class="text-2xs font-semibold text-slate-500">이미 계정이 있으신가요? </span>
        <router-link 
          to="/login" 
          class="text-2xs font-bold text-indigo-400 hover:text-indigo-300 hover:underline transition-all cursor-pointer"
        >
          로그인 화면으로 이동
        </router-link>
      </div>

    </div>

  </div>
</template>

<style scoped>
/* Chrome, Safari, Edge, Opera에서 인풋 배경 채워짐 제거 */
input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus {
  -webkit-text-fill-color: #f1f5f9 !important;
  -webkit-box-shadow: 0 0 0px 1000px rgba(15, 23, 42, 0.95) inset !important;
  transition: background-color 5000s ease-in-out 0s;
}
</style>
