<script setup lang="ts">
import { AlertTriangle, X } from 'lucide-vue-next'

const props = defineProps<{
  isOpen: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <Transition name="modal-fade">
    <div v-if="isOpen" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="emit('cancel')"></div>
      
      <!-- Modal Content -->
      <div class="relative bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-2xl w-full max-w-sm p-6 transform transition-all overflow-hidden flex flex-col gap-4">
        
        <!-- Premium Gradient Header -->
        <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-red-500 to-rose-400"></div>

        <div class="flex items-start justify-between">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-red-500/10 flex items-center justify-center shrink-0">
              <AlertTriangle class="w-5 h-5 text-red-500" />
            </div>
            <div>
              <h3 class="text-lg font-bold text-slate-800 dark:text-white">{{ title }}</h3>
            </div>
          </div>
          <button @click="emit('cancel')" class="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="text-sm text-slate-600 dark:text-slate-400 font-medium leading-relaxed pl-13">
          {{ message }}
        </div>

        <div class="flex gap-3 justify-end mt-2">
          <button 
            @click="emit('cancel')" 
            class="px-4 py-2 rounded-xl text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            {{ cancelText || '취소' }}
          </button>
          <button 
            @click="emit('confirm')" 
            class="px-4 py-2 rounded-xl text-sm font-bold bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/20 transition-all active:scale-95"
          >
            {{ confirmText || '삭제' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
