<script setup lang="ts">
import { ref, computed } from 'vue'
import { FileEdit, X, Expand } from 'lucide-vue-next'
import RichTextEditor from '@/components/ui/RichTextEditor.vue'

const props = defineProps<{
  itemId: string
  columnId: string
  value: string // HTML string
}>()

const emit = defineEmits<{
  (e: 'update', payload: { itemId: string, columnId: string, value: string }): void
}>()

const isModalOpen = ref(false)
const localValue = ref('')

// HTML 태그를 모두 제거하고 순수 텍스트만 추출하여 뷰에 표시하기 위한 Computed
const snippetText = computed(() => {
  if (!props.value) return ''
  const doc = new DOMParser().parseFromString(props.value, 'text/html')
  return doc.body.textContent || ''
})

const openEditor = () => {
  localValue.value = props.value || ''
  isModalOpen.value = true
}

const saveAndClose = () => {
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: localValue.value })
  isModalOpen.value = false
}

const closeWithoutSaving = () => {
  isModalOpen.value = false
}
</script>

<template>
  <div class="w-full h-full relative flex items-center group/richtext">
    <!-- Readonly Snippet Area (Cell View) -->
    <div 
      @click.stop="openEditor"
      class="w-full h-full flex items-center px-2 cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors group-hover/richtext:opacity-90 overflow-hidden relative"
    >
      <div v-if="snippetText" class="text-[11px] text-slate-600 dark:text-slate-300 font-medium truncate w-full pr-6 leading-relaxed flex items-center gap-1.5">
        <FileEdit class="w-3 h-3 text-indigo-400 shrink-0 opacity-50 group-hover/richtext:opacity-100 transition-opacity" />
        <span class="truncate">{{ snippetText }}</span>
      </div>
      <div v-else class="text-[11px] text-slate-400 font-medium opacity-50 group-hover/richtext:opacity-100 transition-opacity w-full text-center">
        + 내용 추가
      </div>
      
      <!-- Hover Icon -->
      <div class="absolute right-2 top-1/2 -translate-y-1/2 opacity-0 group-hover/richtext:opacity-100 transition-opacity bg-slate-100 dark:bg-slate-800 rounded p-1">
        <Expand class="w-3.5 h-3.5 text-slate-500" />
      </div>
    </div>

    <!-- Edit Modal (Teleport to body to avoid clipping inside table) -->
    <Teleport to="body">
      <Transition name="fade-modal">
        <div v-if="isModalOpen" class="fixed inset-0 z-[9999] flex items-center justify-center p-4 sm:p-6" @click.stop="closeWithoutSaving">
          <!-- Backdrop -->
          <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"></div>
          
          <!-- Modal Box -->
          <div 
            @click.stop 
            class="relative w-full max-w-4xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]"
          >
            <!-- Header -->
            <div class="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/80">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-indigo-500/10 flex items-center justify-center">
                  <FileEdit class="w-4 h-4 text-indigo-500" />
                </div>
                <h3 class="text-sm font-bold text-slate-800 dark:text-white">세부 내용 편집</h3>
              </div>
              <div class="flex items-center gap-2">
                <button @click="closeWithoutSaving" class="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-500 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors">
                  취소
                </button>
                <button @click="saveAndClose" class="px-4 py-1.5 rounded-lg text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-500/30 transition-colors">
                  저장하기
                </button>
              </div>
            </div>
            
            <!-- Editor Content -->
            <div class="flex-1 overflow-hidden p-6 bg-slate-50 dark:bg-slate-950 flex flex-col">
              <RichTextEditor 
                v-model="localValue" 
                placeholder="마크다운 스타일의 텍스트, 리스트, 코드 블록 등을 자유롭게 입력하세요..."
                class="flex-1 h-full shadow-sm"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.fade-modal-enter-active,
.fade-modal-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-modal-enter-from,
.fade-modal-leave-to {
  opacity: 0;
  transform: scale(0.98) translateY(10px);
}
</style>
