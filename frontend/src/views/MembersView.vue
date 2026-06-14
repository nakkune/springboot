<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'
import { useRoute } from 'vue-router'
import { Users, Trash2, Loader2, Edit3, Check } from 'lucide-vue-next'

const workspaceStore = useWorkspaceStore()
const route = useRoute()
// Assume the first workspace is selected for simplicity; in real app, you'd pass workspaceId via route params.
const selectedWorkspaceId = ref<string>('')
const loading = ref(true)

// 초대 폼 데이터
const inviteEmail = ref('')
const inviteRole = ref('member')

onMounted(async () => {
  // Ensure workspaces are loaded
  await workspaceStore.fetchWorkspaces()
  if (workspaceStore.workspaces.length > 0) {
    selectedWorkspaceId.value = workspaceStore.workspaces[0].id
    await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
  }
  loading.value = false
})

// 선택된 워크스페이스가 변경될 때마다 새로운 멤버 목록을 불러옴
const handleWorkspaceChange = async () => {
  if (!selectedWorkspaceId.value) return
  loading.value = true
  await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
  loading.value = false
}

const deleteMember = async (memberId: string) => {
  if (!selectedWorkspaceId.value) return
  try {
    await workspaceStore.deleteWorkspaceMember(selectedWorkspaceId.value, memberId)
    // 멤버 삭제 후 목록 갱신
    await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
  } catch (e) {
    console.error('멤버 삭제 실패', e)
  }
}

const inviteMember = async () => {
  if (!selectedWorkspaceId.value || !inviteEmail.value) return
  try {
    await workspaceStore.inviteWorkspaceMember(selectedWorkspaceId.value, inviteEmail.value, inviteRole.value)
    // 초대 후 멤버 목록 새로고침
    await workspaceStore.fetchWorkspaceMembers(selectedWorkspaceId.value)
    inviteEmail.value = ''
    inviteRole.value = 'member'
  } catch (e) {
    console.error('멤버 초대 실패', e)
  }
}
</script>

<template>
  <div class="p-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <h1 class="text-2xl font-bold text-slate-200 flex items-center gap-2">
        <Users class="w-6 h-6 text-indigo-400" />
        Workspace Members
      </h1>
      
      <!-- 대상 워크스페이스 선택 드롭다운 -->
      <div class="flex items-center gap-2">
        <label class="text-xs font-bold text-slate-400 uppercase tracking-widest">Target Workspace:</label>
        <select 
          v-model="selectedWorkspaceId" 
          @change="handleWorkspaceChange"
          class="px-4 py-2 rounded-xl bg-slate-800 border border-slate-700 text-sm font-semibold text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
        >
          <option v-for="ws in workspaceStore.workspaces" :key="ws.id" :value="ws.id">
            {{ ws.name }}
          </option>
        </select>
      </div>
    </div>
    
    <!-- 멤버 초대 폼 -->
    <div class="mb-6 p-4 bg-slate-800 rounded-lg flex space-x-2">
      <input v-model="inviteEmail" type="email" placeholder="Email address" class="flex-grow p-2 rounded bg-slate-900 border border-slate-700 text-slate-200" />
      <select v-model="inviteRole" class="p-2 rounded bg-slate-900 border border-slate-700 text-slate-200">
        <option value="member">Member</option>
        <option value="admin">Admin</option>
      </select>
      <button @click="inviteMember" class="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded flex items-center">
        <Check class="w-4 h-4 mr-1" /> Invite
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-8 h-8 text-indigo-400 animate-spin" />
    </div>
    <div v-else class="space-y-4">
      <div v-if="workspaceStore.currentWorkspaceMembers.length === 0" class="text-slate-400">
        멤버가 없습니다.
      </div>
      <ul class="space-y-2">
        <li v-for="member in workspaceStore.currentWorkspaceMembers" :key="member.id" class="flex items-center justify-between p-3 rounded-lg bg-slate-800 hover:bg-slate-700 transition-colors">
          <div class="flex items-center space-x-3">
            <Users class="w-5 h-5 text-slate-300" />
            <div>
              <div class="text-slate-100 font-medium">{{ member.fullName }}</div>
              <div class="text-slate-400 text-sm">{{ member.email }} ({{ member.role }})</div>
            </div>
          </div>
          <button @click="deleteMember(member.id)" class="text-red-500 hover:text-red-400 transition-colors">
            <Trash2 class="w-5 h-5" />
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
/* 작은 디자인 디테일 */
</style>
