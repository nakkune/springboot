<!-- src/views/AdminView.vue -->
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { 
  UserCheck, 
  ShieldAlert, 
  CheckCircle2, 
  XCircle, 
  Trash2, 
  ArrowLeft, 
  Loader2, 
  Search,
  Sparkles,
  UserX,
  Mail,
  ShieldCheck
} from 'lucide-vue-next'

const router = useRouter()
const users = ref<any[]>([])
const isLoading = ref(false)
const isSavingId = ref<string | null>(null)
const searchQuery = ref('')
const selectedTab = ref<'all' | 'pending' | 'approved' | 'rejected'>('all')

const successMessage = ref('')
const errorMessage = ref('')

// 가입 승인 프리셋
const statusPresets = [
  { value: 'pending', label: '승인 대기 (Pending)', color: 'text-amber-400 bg-amber-500/10 border-amber-500/30' },
  { value: 'approved', label: '승인 완료 (Approved)', color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30' },
  { value: 'rejected', label: '가입 거절 (Rejected)', color: 'text-rose-400 bg-rose-500/10 border-rose-500/30' }
]

// 권한 등급 프리셋
const rolePresets = [
  { value: 'member', label: '일반 회원 (Member)' },
  { value: 'moderator', label: '부관리자 (Moderator)' },
  { value: 'admin', label: '시스템 관리자 (Admin)' }
]

// 모든 유저 정보 조회
const fetchUsers = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const res = await api.get('/users') as any
    users.value = res || []
  } catch (err: any) {
    console.error('어드민 회원 목록 조회 에러:', err)
    errorMessage.value = '회원 목록을 불러오는 중 네트워크 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})

// 가입 승인 상태 변경 E2E API 연동
const handleUpdateStatus = async (user: any, newStatus: string) => {
  if (user.memberStatus === newStatus) return
  
  isSavingId.value = user.id
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const updated = await api.put(`/users/${user.id}`, {
      memberStatus: newStatus
    }) as any
    
    if (updated) {
      user.memberStatus = updated.memberStatus
      user.updatedAt = updated.updatedAt
      successMessage.value = `'${user.fullName}' 님의 가입 상태가 '${newStatus.toUpperCase()}'으로 실시간 변경 수립되었습니다.`
      
      // 자동 토스트 해제
      setTimeout(() => { successMessage.value = '' }, 3000)
    }
  } catch (err: any) {
    console.error('회원 상태 변경 오류:', err)
    errorMessage.value = `'${user.fullName}' 님의 상태 변경 처리에 실패했습니다.`
  } finally {
    isSavingId.value = null
  }
}

// 사용자 권한 역할(Role) 변경 E2E API 연동
const handleUpdateRole = async (user: any, newRole: string) => {
  if (user.role === newRole) return
  
  isSavingId.value = user.id
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const updated = await api.put(`/users/${user.id}`, {
      role: newRole
    }) as any
    
    if (updated) {
      user.role = updated.role
      user.updatedAt = updated.updatedAt
      successMessage.value = `'${user.fullName}' 님의 시스템 권한이 '${newRole.toUpperCase()}' 등급으로 실시간 격상/조정되었습니다.`
      
      // 변경된 사용자가 현재 본인일 경우 로컬스토리지 정보도 리프레시
      if (user.id === localStorage.getItem('userId')) {
        localStorage.setItem('userRole', newRole)
      }

      setTimeout(() => { successMessage.value = '' }, 3000)
    }
  } catch (err: any) {
    console.error('회원 권한 변경 오류:', err)
    errorMessage.value = `'${user.fullName}' 님의 권한 수정 처리에 실패했습니다.`
  } finally {
    isSavingId.value = null
  }
}

// 사용자 시스템 영구 삭제(강제 탈퇴/영구 거절)
const handleDeleteUser = async (user: any) => {
  const currentUserId = localStorage.getItem('userId')
  if (user.id === currentUserId) {
    alert('본인 계정은 관리자 콘솔에서 직접 삭제할 수 없습니다.')
    return
  }

  if (!window.confirm(`정말 '${user.fullName}' 회원을 시스템에서 영구 탈퇴/삭제 처리하시겠습니까?\n이 작업은 되돌릴 수 없으며 관련 프로젝트 멤버 관계가 영구 삭제됩니다!`)) {
    return
  }

  isSavingId.value = user.id
  successMessage.value = ''
  errorMessage.value = ''

  try {
    await api.delete(`/users/${user.id}`)
    users.value = users.value.filter(u => u.id !== user.id)
    successMessage.value = `'${user.fullName}' 님의 계정이 시스템 데이터베이스에서 영구적으로 소멸/제거되었습니다.`
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err: any) {
    console.error('회원 영구 삭제 에러:', err)
    errorMessage.value = `'${user.fullName}' 계정 삭제 처리에 실패했습니다.`
  } finally {
    isSavingId.value = null
  }
}

// 실시간 필터 및 검색 처리 (10년 차 시니어 검색 엔진 설계)
const filteredUsers = computed(() => {
  return users.value.filter(u => {
    // 1. 탭 필터링
    if (selectedTab.value !== 'all' && u.memberStatus !== selectedTab.value) {
      return false
    }
    // 2. 검색어 필터링 (이메일 및 풀네임 통합)
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const nameMatch = u.fullName && u.fullName.toLowerCase().includes(q)
      const emailMatch = u.email && u.email.toLowerCase().includes(q)
      return nameMatch || emailMatch
    }
    return true
  })
})

const getStatusBadgeClass = (status: string) => {
  const preset = statusPresets.find(p => p.value === status)
  return preset ? preset.color : 'text-slate-400 bg-slate-500/10 border-slate-500/30'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const navigateBack = () => {
  router.push('/dashboard')
}
</script>

<template>
  <div class="relative min-h-[calc(100vh-100px)] w-full flex flex-col gap-6 p-1 md:p-2 text-slate-100 font-sans select-none">
    
    <!-- 오로라 백그라운드 글로우 서클 -->
    <div class="absolute top-10 left-1/3 w-[500px] h-[500px] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none"></div>
    <div class="absolute bottom-10 right-1/4 w-[400px] h-[400px] bg-purple-500/5 rounded-full blur-[100px] pointer-events-none"></div>

    <!-- 헤더 영역 -->
    <div class="relative flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-slate-900/40 backdrop-blur-xl border border-slate-800/80 rounded-2xl p-6 shadow-xl">
      <div class="flex items-center gap-4">
        <button 
          @click="navigateBack"
          class="p-2.5 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-400 hover:text-white hover:border-slate-700 hover:bg-slate-800/50 active:scale-95 transition-all"
          title="대시보드로 돌아가기"
        >
          <ArrowLeft class="w-4 h-4" />
        </button>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-xl font-black tracking-tight text-white">Admin Console</h1>
            <span class="px-2 py-0.5 rounded bg-gradient-to-r from-indigo-500/20 to-purple-500/20 border border-indigo-500/30 text-[10px] font-bold text-indigo-300 flex items-center gap-1">
              <Sparkles class="w-3 h-3 text-indigo-400 animate-pulse" />
              SYSTEM GOVERNANCE
            </span>
          </div>
          <p class="text-xs text-slate-500 mt-1 font-medium leading-relaxed">
            MiniERP 전체 회원 계정의 가입 승인 상태 및 시스템 접근 권한 등급을 E2E로 안전하게 감시 및 통제합니다.
          </p>
        </div>
      </div>
      
      <!-- 검색 창 -->
      <div class="relative w-full md:w-72 flex items-center">
        <Search class="absolute left-3.5 w-4 h-4 text-slate-500" />
        <input 
          v-model="searchQuery"
          type="text"
          class="w-full bg-slate-950/40 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-600"
          placeholder="이름 또는 이메일로 회원 검색..."
        />
      </div>
    </div>

    <!-- 알림 메시지 배너 (에러 / 성공) -->
    <Transition name="fade">
      <div 
        v-if="successMessage" 
        class="bg-indigo-500/15 border border-indigo-500/30 text-indigo-300 text-xs px-5 py-3 rounded-xl font-semibold shadow-lg shadow-indigo-500/5 flex items-center gap-2.5"
      >
        <CheckCircle2 class="w-4 h-4 text-indigo-400 shrink-0" />
        <span>{{ successMessage }}</span>
      </div>
    </Transition>

    <Transition name="fade">
      <div 
        v-if="errorMessage" 
        class="bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs px-5 py-3 rounded-xl font-semibold shadow-lg shadow-rose-500/5 flex items-center gap-2.5"
      >
        <ShieldAlert class="w-4 h-4 text-rose-400 shrink-0" />
        <span>{{ errorMessage }}</span>
      </div>
    </Transition>

    <!-- 메인 대시보드 뷰포트 -->
    <div class="flex-1 flex flex-col bg-slate-900/40 backdrop-blur-xl border border-slate-800/80 rounded-2xl overflow-hidden shadow-2xl">
      
      <!-- 탭 컨트롤바 -->
      <div class="flex items-center justify-between border-b border-slate-800/80 px-6 py-3 bg-slate-950/20">
        <div class="flex gap-2">
          <button 
            @click="selectedTab = 'all'"
            class="px-4 py-1.5 rounded-lg text-xs font-bold transition-all"
            :class="selectedTab === 'all' ? 'bg-slate-800 text-white shadow-inner border border-slate-700/60' : 'text-slate-500 hover:text-slate-300'"
          >
            전체 회원 ({{ users.length }})
          </button>
          <button 
            @click="selectedTab = 'pending'"
            class="px-4 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5"
            :class="selectedTab === 'pending' ? 'bg-amber-500/15 text-amber-300 border border-amber-500/20' : 'text-slate-500 hover:text-slate-300'"
          >
            가입 승인 대기 ({{ users.filter(u => u.memberStatus === 'pending').length }})
          </button>
          <button 
            @click="selectedTab = 'approved'"
            class="px-4 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5"
            :class="selectedTab === 'approved' ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/20' : 'text-slate-500 hover:text-slate-300'"
          >
            승인 완료 ({{ users.filter(u => u.memberStatus === 'approved').length }})
          </button>
          <button 
            @click="selectedTab = 'rejected'"
            class="px-4 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5"
            :class="selectedTab === 'rejected' ? 'bg-rose-500/15 text-rose-300 border border-rose-500/20' : 'text-slate-500 hover:text-slate-300'"
          >
            가입 반려 ({{ users.filter(u => u.memberStatus === 'rejected').length }})
          </button>
        </div>

        <button 
          @click="fetchUsers"
          :disabled="isLoading"
          class="flex items-center gap-1.5 px-3 py-1.5 text-slate-400 hover:text-white hover:bg-slate-800/50 border border-slate-800 hover:border-slate-700 rounded-lg text-2xs font-bold transition-all"
        >
          <Loader2 v-if="isLoading" class="w-3.5 h-3.5 animate-spin text-indigo-400" />
          <span>목록 새로고침</span>
        </button>
      </div>

      <!-- 로딩 인터페이스 -->
      <div v-if="isLoading && users.length === 0" class="flex flex-col items-center justify-center py-24 gap-4">
        <Loader2 class="w-10 h-10 text-indigo-500 animate-spin" />
        <p class="text-xs text-slate-500 font-semibold tracking-wider animate-pulse">데이터베이스로부터 회원 정보를 동기화하는 중...</p>
      </div>

      <!-- 검색 결과 없음 인터페이스 -->
      <div v-else-if="filteredUsers.length === 0" class="flex flex-col items-center justify-center py-20 gap-3">
        <UserX class="w-12 h-12 text-slate-700" />
        <h3 class="text-sm font-bold text-slate-400 mt-2">해당 조건에 맞는 회원이 없습니다</h3>
        <p class="text-2xs text-slate-600 font-medium">검색어 철자 또는 필터 탭 옵션을 확인해 주세요.</p>
      </div>

      <!-- 회원 테이블 목록 (10년 차 시니어 오로라 디자인 탑재) -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-slate-800 text-[10px] font-bold text-slate-500 uppercase tracking-widest bg-slate-950/10">
              <th class="px-6 py-4">사용자 정보</th>
              <th class="px-6 py-4">이메일</th>
              <th class="px-6 py-4">가입 신청일</th>
              <th class="px-6 py-4 text-center">승인 상태 (Member Status)</th>
              <th class="px-6 py-4 text-center">권한 등급 (System Role)</th>
              <th class="px-6 py-4 text-center">계정 작업</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-800/60">
            <tr 
              v-for="u in filteredUsers" 
              :key="u.id"
              class="hover:bg-slate-800/10 transition-colors text-slate-300"
            >
              <!-- 프로필 요약 -->
              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div class="relative w-9 h-9 rounded-xl overflow-hidden bg-slate-950 border border-slate-800/50 flex items-center justify-center text-xs font-bold text-slate-400 shadow-md">
                    <img 
                      v-if="u.avatarUrl" 
                      :src="u.avatarUrl" 
                      alt="Avatar" 
                      class="w-full h-full object-cover"
                    />
                    <div 
                      v-else 
                      class="w-full h-full bg-gradient-to-tr from-slate-800 to-slate-900 flex items-center justify-center font-extrabold text-slate-500"
                    >
                      {{ u.fullName ? u.fullName.slice(0, 1) : 'U' }}
                    </div>
                  </div>
                  <div>
                    <h4 class="text-xs font-black text-white leading-none">{{ u.fullName }}</h4>
                    <span class="text-[9px] text-slate-600 font-bold uppercase tracking-wider block mt-1">ID: {{ u.id.slice(0, 8) }}...</span>
                  </div>
                </div>
              </td>

              <!-- 이메일 -->
              <td class="px-6 py-4">
                <div class="flex items-center gap-1.5">
                  <Mail class="w-3.5 h-3.5 text-slate-500" />
                  <span class="text-xs font-semibold text-slate-400">{{ u.email }}</span>
                </div>
              </td>

              <!-- 가입일자 -->
              <td class="px-6 py-4">
                <span class="text-[11px] font-semibold text-slate-500">{{ formatDate(u.createdAt) }}</span>
              </td>

              <!-- 가입 승인 상태 변경 드롭다운 & 퀵버튼 -->
              <td class="px-6 py-4 text-center">
                <div class="inline-flex items-center gap-2">
                  <span 
                    class="px-2.5 py-1 rounded-lg text-2xs font-bold border shrink-0 transition-colors"
                    :class="getStatusBadgeClass(u.memberStatus)"
                  >
                    {{ u.memberStatus.toUpperCase() }}
                  </span>

                  <!-- 간편 원클릭 승인 버튼 (대기 유저 전용) -->
                  <button 
                    v-if="u.memberStatus === 'pending'"
                    @click="handleUpdateStatus(u, 'approved')"
                    :disabled="isSavingId === u.id"
                    class="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-500/80 hover:bg-emerald-600 border border-emerald-500/20 text-white text-3xs font-extrabold shadow-md active:scale-95 transition-all disabled:opacity-50"
                  >
                    <UserCheck class="w-3 h-3" />
                    <span>즉시 승인</span>
                  </button>

                  <!-- 상태 변경 셀렉트 박스 -->
                  <select 
                    v-model="u.memberStatus"
                    @change="handleUpdateStatus(u, u.memberStatus)"
                    :disabled="isSavingId === u.id"
                    class="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1 text-3xs font-bold text-slate-300 focus:outline-none focus:border-indigo-500 transition-colors disabled:opacity-50 cursor-pointer"
                  >
                    <option v-for="p in statusPresets" :key="p.value" :value="p.value">
                      {{ p.label }}
                    </option>
                  </select>
                </div>
              </td>

              <!-- 사용자 역할 권한 변경 드롭다운 -->
              <td class="px-6 py-4 text-center">
                <div class="inline-flex items-center gap-2">
                  <span 
                    class="px-2 py-0.5 rounded border text-3xs font-extrabold flex items-center gap-0.5"
                    :class="u.role === 'admin' 
                      ? 'text-indigo-400 bg-indigo-500/10 border-indigo-500/20' 
                      : u.role === 'moderator' 
                      ? 'text-purple-400 bg-purple-500/10 border-purple-500/20' 
                      : 'text-slate-500 bg-slate-500/5 border-slate-800'"
                  >
                    <ShieldCheck v-if="u.role === 'admin'" class="w-2.5 h-2.5 text-indigo-400" />
                    {{ u.role.toUpperCase() }}
                  </span>

                  <select 
                    v-model="u.role"
                    @change="handleUpdateRole(u, u.role)"
                    :disabled="isSavingId === u.id"
                    class="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1 text-3xs font-bold text-slate-300 focus:outline-none focus:border-indigo-500 transition-colors disabled:opacity-50 cursor-pointer"
                  >
                    <option v-for="r in rolePresets" :key="r.value" :value="r.value">
                      {{ r.label }}
                    </option>
                  </select>
                </div>
              </td>

              <!-- 삭제 등 행동 관리 -->
              <td class="px-6 py-4 text-center">
                <button 
                  @click="handleDeleteUser(u)"
                  :disabled="isSavingId === u.id"
                  class="p-2 rounded-lg bg-rose-500/10 hover:bg-rose-500 border border-rose-500/20 text-rose-400 hover:text-white active:scale-95 transition-all disabled:opacity-50 disabled:pointer-events-none"
                  title="사용자 시스템에서 영구 탈퇴/삭제"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>

  </div>
</template>

<style scoped>
/* 드롭다운 셀렉트 박스 화살표 수려하게 커스터마이징 */
select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 10px;
  padding-right: 24px;
}

/* 전환 모션 페이드 정의 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
