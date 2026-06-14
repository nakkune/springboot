<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Play, Pause, Trash2, Settings2, Sparkles } from 'lucide-vue-next'
import api from '@/services/api'
import AutomationBuilder from './AutomationBuilder.vue'

const props = defineProps<{
  boardId: string
}>()

const automations = ref<any[]>([])
const isLoading = ref(true)
const isBuilderOpen = ref(false)

const loadAutomations = async () => {
  try {
    const res = await api.get(`/automations/board/${props.boardId}`) as any[]
    automations.value = res
  } catch (e) {
    console.error('Failed to load automations', e)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadAutomations()
})

const toggleActive = async (auto: any) => {
  try {
    auto.isActive = !auto.isActive
    await api.put(`/automations/${auto.id}`, auto)
  } catch (e) {
    console.error(e)
    auto.isActive = !auto.isActive // rollback
  }
}

const deleteAutomation = async (id: string) => {
  if (confirm('정말로 이 자동화 규칙을 삭제하시겠습니까?')) {
    try {
      await api.delete(`/automations/${id}`)
      automations.value = automations.value.filter(a => a.id !== id)
    } catch (e) {
      console.error(e)
    }
  }
}

const handleSaved = () => {
  isBuilderOpen.value = false
  loadAutomations()
}
</script>

<template>
  <div class="h-full flex flex-col select-none relative bg-slate-900 text-slate-200">
    <!-- Header -->
    <div class="px-6 py-5 border-b border-slate-800 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <Sparkles class="w-5 h-5 text-indigo-400" />
        <h2 class="text-lg font-bold text-white">Board Automations</h2>
      </div>
      <button 
        @click="isBuilderOpen = true"
        class="bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg px-4 py-2 text-xs font-bold transition-all active:scale-95 flex items-center gap-1.5 shadow-lg shadow-indigo-500/20"
      >
        <Plus class="w-4 h-4" />
        새 규칙 추가
      </button>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-6 space-y-4">
      <div v-if="isLoading" class="text-center text-slate-500 py-10">
        Loading...
      </div>
      <div v-else-if="automations.length === 0" class="text-center flex flex-col items-center justify-center py-20 border border-dashed border-slate-700 rounded-2xl bg-slate-800/20">
        <Settings2 class="w-10 h-10 text-slate-600 mb-3" />
        <p class="text-slate-400 font-medium text-sm">등록된 자동화 규칙이 없습니다.</p>
        <p class="text-slate-500 text-xs mt-1">반복되는 업무를 자동화하여 팀의 생산성을 높여보세요.</p>
      </div>
      <div v-else>
        <!-- Automation Cards -->
        <div 
          v-for="auto in automations" 
          :key="auto.id"
          class="bg-slate-800/50 border border-slate-700 rounded-xl p-4 flex items-center justify-between hover:border-indigo-500/50 transition-colors group mb-3"
        >
          <div class="flex items-center gap-4">
            <!-- Toggle Switch -->
            <button 
              @click="toggleActive(auto)"
              class="w-10 h-6 rounded-full flex items-center px-0.5 transition-colors duration-300"
              :class="auto.isActive ? 'bg-indigo-500' : 'bg-slate-600'"
            >
              <div 
                class="w-5 h-5 bg-white rounded-full shadow-sm transform transition-transform duration-300 flex items-center justify-center"
                :class="auto.isActive ? 'translate-x-4' : 'translate-x-0'"
              >
                <Play v-if="auto.isActive" class="w-3 h-3 text-indigo-500" />
                <Pause v-else class="w-3 h-3 text-slate-400" />
              </div>
            </button>
            <div>
              <h3 class="text-sm font-bold text-white mb-0.5">{{ auto.name }}</h3>
              <p class="text-xs text-slate-400 font-medium flex gap-2">
                <span>실행 횟수: <b class="text-slate-300">{{ auto.runCount }}</b></span>
                <span v-if="auto.lastRunAt">•</span>
                <span v-if="auto.lastRunAt">마지막 실행: {{ new Date(auto.lastRunAt).toLocaleDateString() }}</span>
              </p>
            </div>
          </div>
          
          <!-- Actions -->
          <div class="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
            <button class="p-2 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-white transition-colors">
              <Settings2 class="w-4 h-4" />
            </button>
            <button 
              @click="deleteAutomation(auto.id)"
              class="p-2 rounded-lg hover:bg-rose-500/20 text-rose-400 hover:text-rose-300 transition-colors"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Builder Modal -->
    <div v-if="isBuilderOpen" class="absolute inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm px-10 py-10">
      <AutomationBuilder :boardId="boardId" @close="isBuilderOpen = false" @saved="handleSaved" />
    </div>
  </div>
</template>
