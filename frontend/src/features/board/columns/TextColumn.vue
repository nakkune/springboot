<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  value: string
  itemId: string
  columnId: string
}>()

const emit = defineEmits(['update'])

const isEditing = ref(false)
const editValue = ref('')

const startEditing = () => {
  editValue.value = props.value || ''
  isEditing.value = true
}

const save = () => {
  const trimmed = editValue.value.trim()
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: trimmed })
  isEditing.value = false
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    save()
  } else if (e.key === 'Escape') {
    isEditing.value = false
  }
}
</script>

<template>
  <div class="w-full h-full flex items-center justify-center p-1">
    <div v-if="isEditing" class="w-full">
      <input
        ref="inputRef"
        :value="editValue"
        @input="editValue = ($event.target as HTMLInputElement).value"
        @blur="save"
        @keydown="handleKeydown"
        class="w-full bg-white dark:bg-slate-800 border border-slate-250 dark:border-slate-700 rounded-md px-2 py-1 text-xs text-slate-800 dark:text-white outline-none focus:border-indigo-500 transition-colors shadow-sm"
        placeholder="내용 입력..."
        autofocus
      />
    </div>
    <!-- 10년 차 시니어: 빈 값일 때 '+ 내용 추가'를 옅은 회색으로 은은하게 렌더링하고, 호버 스타일을 라이트/다크 범용으로 최적화 -->
    <div
      v-else
      @click.stop="startEditing"
      class="w-full truncate text-left text-[11px] rounded-md px-2.5 py-1.5 cursor-pointer transition-colors font-medium"
      :class="[
        value ? 'text-slate-700 dark:text-slate-200' : 'text-slate-400/80 dark:text-slate-500/80'
      ]"
      :title="value || '내용 추가'"
    >
      {{ value || '+ 내용 추가' }}
    </div>
  </div>
</template>
