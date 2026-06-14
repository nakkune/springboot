<script setup lang="ts">
import { ref } from 'vue'
import { X, Save, ArrowRight } from 'lucide-vue-next'
import api from '@/services/api'

const props = defineProps<{
  boardId: string
}>()

const emit = defineEmits(['close', 'saved'])

// 문장형 빌더 State
const triggerType = ref('status_change')
const triggerColumn = ref('status')
const triggerValue = ref('완료')

const actionType = ref('notify')
const actionTarget = ref('@admin')

const isSaving = ref(false)

const handleSave = async () => {
  isSaving.value = true
  try {
    const triggerConfig = JSON.stringify({
      type: triggerType.value,
      column_id: triggerColumn.value,
      to_value: triggerValue.value
    })
    
    const actionConfig = JSON.stringify({
      type: actionType.value,
      target: actionTarget.value
    })
    
    const name = `When ${triggerColumn.value} changes to ${triggerValue.value}, ${actionType.value} ${actionTarget.value}`
    
    await api.post('/automations', {
      boardId: props.boardId,
      name,
      isActive: true,
      triggerConfig,
      conditionConfig: '{}',
      actionConfig,
      createdBy: 'f1b9b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c' // Mock user
    })
    
    emit('saved')
  } catch (e) {
    console.error(e)
    alert('자동화 저장에 실패했습니다.')
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-4xl bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl flex flex-col overflow-hidden h-full max-h-[600px]">
    <!-- Header -->
    <div class="px-6 py-4 border-b border-slate-800 flex justify-between items-center bg-slate-900/50">
      <h3 class="font-bold text-white text-lg">새 자동화 규칙 생성</h3>
      <button @click="$emit('close')" class="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors">
        <X class="w-5 h-5" />
      </button>
    </div>
    
    <!-- Builder Body -->
    <div class="flex-1 p-8 bg-slate-900 flex flex-col gap-8 overflow-y-auto">
      
      <div class="text-center text-slate-400 font-medium mb-4">
        조건과 동작을 조합하여 문장을 완성하세요.
      </div>
      
      <div class="flex flex-wrap items-center justify-center gap-4 text-xl font-black text-slate-300">
        <!-- TRIGGER -->
        <span class="bg-indigo-500/20 text-indigo-400 px-4 py-2 rounded-xl border border-indigo-500/30">
          When
        </span>
        
        <select v-model="triggerColumn" class="bg-slate-800 border-b-2 border-indigo-500 text-white px-2 py-1 outline-none cursor-pointer focus:bg-slate-700 transition-colors rounded-t-lg">
          <option value="status">상태</option>
          <option value="priority">우선순위</option>
          <option value="date">마감일</option>
        </select>
        
        <span class="text-slate-500">changes to</span>
        
        <input 
          v-model="triggerValue" 
          type="text"
          class="bg-slate-800 border-b-2 border-indigo-500 text-white px-2 py-1 outline-none focus:bg-slate-700 w-28 text-center rounded-t-lg transition-colors"
        />
        
        <!-- ARROW -->
        <ArrowRight class="w-8 h-8 text-slate-600 mx-2" />
        
        <!-- ACTION -->
        <span class="bg-rose-500/20 text-rose-400 px-4 py-2 rounded-xl border border-rose-500/30">
          Then
        </span>
        
        <select v-model="actionType" class="bg-slate-800 border-b-2 border-rose-500 text-white px-2 py-1 outline-none cursor-pointer focus:bg-slate-700 rounded-t-lg transition-colors">
          <option value="notify">알림 발송</option>
          <option value="change_status">상태 변경</option>
        </select>
        
        <span class="text-slate-500">to</span>
        
        <input 
          v-model="actionTarget" 
          type="text"
          placeholder="담당자"
          class="bg-slate-800 border-b-2 border-rose-500 text-white px-2 py-1 outline-none focus:bg-slate-700 w-32 text-center rounded-t-lg transition-colors"
        />
      </div>
      
    </div>
    
    <!-- Footer -->
    <div class="px-6 py-4 border-t border-slate-800 bg-slate-950 flex justify-end gap-3">
      <button 
        @click="$emit('close')"
        class="px-5 py-2.5 rounded-xl font-bold text-slate-400 hover:text-white hover:bg-slate-800 transition-colors text-sm"
      >
        취소
      </button>
      <button 
        @click="handleSave"
        :disabled="isSaving"
        class="bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl px-6 py-2.5 text-sm font-bold transition-all shadow-lg shadow-indigo-600/20 flex items-center gap-2 disabled:opacity-50"
      >
        <Save class="w-4 h-4" />
        <span>{{ isSaving ? '저장 중...' : '자동화 저장' }}</span>
      </button>
    </div>
  </div>
</template>
