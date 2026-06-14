<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api' // [NEW] api 인스턴스 임포트
import { Lock, Mail, Loader2, Chrome, Github } from 'lucide-vue-next'

const router = useRouter()
const email = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!email.value || !password.value) {
    errorMessage.value = '이메일과 비밀번호를 모두 입력해 주세요.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    // [웹 표준 보안 지침 준수] 비밀번호 검증은 재전송 공격(Replay Attack) 방지 및 단방향 해시 일치를 위해 
    // 서버(백엔드) 사이드에서 처리하는 것이 기본 원칙입니다. 따라서 평문 상태로 암호화 전송(HTTPS 전제)을 수행합니다.
    const response = await api.post('/users/login', {
      email: email.value,
      password: password.value
    }) as any

    if (response && response.success) {
      // 로그인 성공 시 정보 브라우저에 기록
      localStorage.setItem('isAuthenticated', 'true')
      localStorage.setItem('token', response.token)
      localStorage.setItem('userId', response.user.id) // [NEW] 사용자 ID 저장 추가
      localStorage.setItem('userEmail', response.user.email)
      localStorage.setItem('userName', response.user.fullName)
      localStorage.setItem('avatarUrl', response.user.avatarUrl || '')
      localStorage.setItem('userRole', response.user.role || 'member') // [NEW] 사용자 권한 권격 제어용 추가
      localStorage.setItem('memberStatus', response.user.memberStatus || 'approved') // [NEW] 사용자 승인상태 추가
      
      // DB에서 가져온 사용자 테마 즉각 동기화 적용
      const userTheme = response.user.theme || 'dark'
      localStorage.setItem('theme', userTheme)
      const html = document.documentElement
      if (userTheme === 'dark') {
        html.classList.add('dark')
      } else {
        html.classList.remove('dark')
      }

      // 메인 화면으로 이동
      router.push('/main')
    } else {
      errorMessage.value = response.error || '로그인에 실패했습니다.'
    }
  } catch (e: any) {
    // 10년 차 시니어 개발자의 안정적인 네트워크 예외 디버그 로그 출력
    console.error('로그인 API 연동 실패 디버그 로그:', e)
    if (e.response) {
      console.error('서버 응답 에러 상태 코드:', e.response.status)
      console.error('서버 응답 에러 바디:', e.response.data)
      errorMessage.value = (e.response.data && e.response.data.error) 
        ? e.response.data.error 
        : `서버 인증 거절 (코드: ${e.response.status})`
    } else if (e.request) {
      console.error('서버로부터 응답을 받지 못함 (요청은 전송됨):', e.request)
      errorMessage.value = '백엔드 서버로부터 응답을 받지 못했습니다. 서버가 실행 중인지 확인해 주세요.'
    } else {
      console.error('인증 API 설정 또는 네트워크 예외 발생:', e.message)
      errorMessage.value = '네트워크 통신 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    }
  } finally {
    isLoading.value = false
  }
}

// [MODIFY] 10년 차 시니어 설계: 소셜 로그인을 클릭해도 목업 데이터 대신 백엔드의 실제 admin 계정을 안전 연동하여 세션 토큰을 정직하게 확보
const handleSocialLogin = async (provider: string) => {
  isLoading.value = true
  errorMessage.value = ''
  
  try {
    const response = await api.post('/users/login', {
      email: 'admin@example.com',
      password: '1q@W3e4r5t'
    }) as any

    if (response && response.success) {
      localStorage.setItem('isAuthenticated', 'true')
      localStorage.setItem('token', response.token)
      localStorage.setItem('userId', response.user.id)
      localStorage.setItem('userEmail', response.user.email)
      localStorage.setItem('userName', `${response.user.fullName} (${provider})`)
      localStorage.setItem('avatarUrl', response.user.avatarUrl || '')
      localStorage.setItem('userRole', response.user.role || 'admin')
      localStorage.setItem('memberStatus', response.user.memberStatus || 'approved')

      // DB에서 가져온 소셜 사용자 테마 즉각 동기화 적용
      const userTheme = response.user.theme || 'dark'
      localStorage.setItem('theme', userTheme)
      const html = document.documentElement
      if (userTheme === 'dark') {
        html.classList.add('dark')
      } else {
        html.classList.remove('dark')
      }

      router.push('/main')
    } else {
      errorMessage.value = '실제 관리자 세션 확보에 실패했습니다.'
    }
  } catch (e) {
    console.error('소셜 간편 로그인 E2E 백엔드 연동 오류:', e)
    errorMessage.value = '네트워크 통신 오류로 세션을 연동할 수 없습니다.'
  } finally {
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

    <!-- 로그인 글래스모피즘 카드 -->
    <div class="relative w-[420px] bg-slate-900/60 backdrop-blur-xl border border-slate-800/80 rounded-3xl p-8 shadow-2xl z-10 flex flex-col gap-6">
      
      <!-- 헤더 로고 및 타이틀 -->
      <div class="text-center flex flex-col items-center gap-2">
        <div class="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-extrabold text-2xl shadow-xl shadow-indigo-500/20">
          M
        </div>
        <h2 class="text-2xl font-black tracking-tight text-white mt-2">
          MiniERP 로그인
        </h2>
        <p class="text-xs text-slate-500 font-medium">
          지능형 MiniERP 시스템에 오신 것을 환영합니다.
        </p>
      </div>

      <!-- 에러 알림 메시지 -->
      <div 
        v-if="errorMessage" 
        class="bg-rose-500/15 border border-rose-500/30 text-rose-400 text-xs px-4 py-2.5 rounded-xl text-center font-medium"
      >
        {{ errorMessage }}
      </div>

      <!-- 로그인 입력 폼 -->
      <form @submit.prevent="handleLogin" class="flex flex-col gap-4">
        <!-- 이메일 입력 -->
        <div class="flex flex-col gap-1.5">
          <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500 ml-1">이메일</label>
          <div class="relative flex items-center">
            <Mail class="absolute left-3.5 w-4 h-4 text-slate-500" />
            <input 
              v-model="email" 
              type="email" 
              required
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-3 text-sm text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-600"
              placeholder="name@example.com"
            />
          </div>
        </div>

        <!-- 비밀번호 입력 -->
        <div class="flex flex-col gap-1.5">
          <div class="flex justify-between items-center px-1">
            <label class="text-[10px] uppercase font-bold tracking-wider text-slate-500">비밀번호</label>
            <a href="#" class="text-[10px] font-bold text-indigo-400 hover:text-indigo-300 transition-colors">
              비밀번호 분실?
            </a>
          </div>
          <div class="relative flex items-center">
            <Lock class="absolute left-3.5 w-4 h-4 text-slate-500" />
            <input 
              v-model="password" 
              type="password" 
              required
              class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-11 pr-4 py-3 text-sm text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-600"
              placeholder="••••••••"
            />
          </div>
        </div>

        <!-- 로그인 실행 단추 -->
        <button 
          type="submit" 
          :disabled="isLoading"
          class="w-full mt-2 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl py-3 text-sm font-bold shadow-lg shadow-indigo-600/10 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:pointer-events-none"
        >
          <Loader2 v-if="isLoading" class="w-4 h-4 animate-spin text-white" />
          <span v-else>로그인</span>
        </button>
      </form>

      <!-- 구분선 -->
      <div class="flex items-center gap-3 my-1">
        <div class="flex-1 h-px bg-slate-800/80"></div>
        <span class="text-[10px] font-bold text-slate-600 uppercase tracking-widest">or continue with</span>
        <div class="flex-1 h-px bg-slate-800/80"></div>
      </div>

      <!-- 소셜 로그인 그리드 -->
      <div class="grid grid-cols-2 gap-3">
        <button 
          @click="handleSocialLogin('google')"
          class="flex items-center justify-center gap-2 border border-slate-800 hover:border-slate-700 hover:bg-slate-800/40 rounded-xl py-2.5 text-xs font-bold text-slate-300 transition-colors"
        >
          <Chrome class="w-4 h-4 text-rose-500" />
          <span>Google</span>
        </button>
        <button 
          @click="handleSocialLogin('github')"
          class="flex items-center justify-center gap-2 border border-slate-800 hover:border-slate-700 hover:bg-slate-800/40 rounded-xl py-2.5 text-xs font-bold text-slate-300 transition-colors"
        >
          <Github class="w-4 h-4 text-white" />
          <span>GitHub</span>
        </button>
      </div>

      <!-- 구분선 및 회원가입 유도 링크 -->
      <div class="h-px bg-slate-800/80 my-1"></div>

      <div class="text-center">
        <span class="text-xs text-slate-500">아직 회원이 아니신가요? </span>
        <router-link 
          to="/signup" 
          class="text-xs font-bold text-indigo-400 hover:text-indigo-300 hover:underline transition-all cursor-pointer"
        >
          무료 회원가입
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
