<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  value: string
  itemId: string
  columnId: string
  options: string[]
}>()

const emit = defineEmits(['update'])

const isEditing = ref(false)

const handleChange = (e: Event) => {
  const val = (e.target as HTMLSelectElement).value
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: val })
  isEditing.value = false
}
</script>

<template>
  <div class="w-full h-full flex items-center justify-center p-1">
    <div v-if="isEditing" class="w-full">
      <select 
        :value="value"
        @change="handleChange"
        @blur="isEditing = false"
        class="w-full bg-slate-800 border border-slate-700 rounded px-1 py-1 text-[10px] text-white outline-none focus:border-indigo-500 cursor-pointer"
      >
        <option value="">선택 안함</option>
        <option v-for="opt in options" :key="opt" :value="opt">{{ opt }}</option>
      </select>
    </div>
    <div 
      v-else 
      @click="isEditing = true"
      class="w-full h-full flex items-center justify-center text-xs text-slate-300 hover:bg-slate-700/50 rounded cursor-pointer transition-colors"
    >
      {{ value || '-' }}
    </div>
  </div>
</template>
