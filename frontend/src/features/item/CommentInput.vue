<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Send, User } from 'lucide-vue-next'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'

const props = defineProps<{
  itemId: string
}>()

const emit = defineEmits(['submit'])

const workspaceStore = useWorkspaceStore()
const commentText = ref('')
const isMentioning = ref(false)
const mentionSearch = ref('')
const mentionDropdownIndex = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

// 멘션 목록 필터링
const filteredMembers = computed(() => {
  if (!mentionSearch.value) return workspaceStore.currentWorkspaceMembers
  return workspaceStore.currentWorkspaceMembers.filter(m => 
    m.fullName.toLowerCase().includes(mentionSearch.value.toLowerCase())
  )
})

const handleInput = (e: Event) => {
  const value = (e.target as HTMLInputElement).value
  
  // 간단한 @멘션 정규식 매칭
  const mentionMatch = value.match(/@(\w*)$/)
  if (mentionMatch) {
    isMentioning.value = true
    mentionSearch.value = mentionMatch[1]
    mentionDropdownIndex.value = 0
  } else {
    isMentioning.value = false
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (isMentioning.value) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      mentionDropdownIndex.value = (mentionDropdownIndex.value + 1) % filteredMembers.value.length
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      mentionDropdownIndex.value = (mentionDropdownIndex.value - 1 + filteredMembers.value.length) % filteredMembers.value.length
    } else if (e.key === 'Enter') {
      e.preventDefault()
      selectMention(filteredMembers.value[mentionDropdownIndex.value])
    } else if (e.key === 'Escape') {
      isMentioning.value = false
    }
  }
}

const selectMention = (member: any) => {
  if (!member) return
  
  // 텍스트에서 @mentionSearch 부분을 실제 멘션 포맷으로 교체
  const regex = new RegExp(`@${mentionSearch.value}$`)
  commentText.value = commentText.value.replace(regex, `@${member.fullName} `)
  isMentioning.value = false
  
  // input 포커스 복귀
  inputRef.value?.focus()
}

const handleSubmit = () => {
  if (!commentText.value.trim()) return
  
  // 실제 백엔드 연동 시 mentionedUserIds 배열을 생성해서 같이 넘길 수 있음
  const mentionedNames = commentText.value.match(/@\w+/g) || []
  const mentionedUserIds = mentionedNames.map(name => {
    const rawName = name.replace('@', '')
    const member = workspaceStore.currentWorkspaceMembers.find(m => m.fullName === rawName)
    return member ? member.id : null
  }).filter(Boolean)
  
  emit('submit', { text: commentText.value, mentionedUserIds })
  commentText.value = ''
}
</script>

<template>
  <div class="relative w-full">
    <!-- Mention Popover -->
    <div 
      v-if="isMentioning && filteredMembers.length > 0" 
      class="absolute bottom-full left-0 mb-2 w-64 bg-slate-800 border border-slate-700 rounded-xl shadow-2xl overflow-hidden z-50"
    >
      <div class="px-3 py-2 bg-slate-900/50 border-b border-slate-700 text-xs text-slate-400 font-bold">
        멤버 멘션하기
      </div>
      <div class="max-h-48 overflow-y-auto">
        <div 
          v-for="(member, idx) in filteredMembers" 
          :key="member.id"
          @click="selectMention(member)"
          class="px-3 py-2 flex items-center gap-2 cursor-pointer transition-colors"
          :class="idx === mentionDropdownIndex ? 'bg-indigo-600 text-white' : 'hover:bg-slate-700 text-slate-300'"
        >
          <div class="w-6 h-6 rounded-full bg-slate-600 flex items-center justify-center shrink-0">
            <User class="w-3 h-3 text-slate-300" />
          </div>
          <span class="text-xs font-semibold">{{ member.fullName }}</span>
        </div>
      </div>
    </div>
    
    <!-- Input Form -->
    <form @submit.prevent="handleSubmit" class="flex gap-2">
      <input 
        ref="inputRef"
        v-model="commentText" 
        @input="handleInput"
        @keydown="handleKeyDown"
        type="text" 
        autocomplete="off"
        class="flex-1 bg-slate-800 border border-slate-700/80 rounded-lg px-3.5 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all placeholder-slate-600"
        placeholder="피드백이나 진행 상황을 공유해 보세요... (@로 팀원 멘션)"
      />
      <button 
        type="submit"
        class="bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg px-3.5 py-2 flex items-center justify-center transition-colors active:scale-95 shadow-lg shadow-indigo-600/10"
      >
        <Send class="w-3.5 h-3.5" />
      </button>
    </form>
  </div>
</template>
