<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  value: string
  itemId: string
  columnId: string
}>()

const emit = defineEmits(['update'])

const isEditing = ref(false)
const inputTag = ref('')

const tags = computed(() => {
  if (!props.value) return []
  try {
    return JSON.parse(props.value)
  } catch (e) {
    // 쉼표 기반 폴백
    return props.value.split(',').map(s => s.trim()).filter(Boolean)
  }
})

const removeTag = (idx: number | string) => {
  const newTags = [...tags.value]
  newTags.splice(Number(idx), 1)
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: JSON.stringify(newTags) })
}

const addTag = () => {
  if (!inputTag.value.trim()) {
    isEditing.value = false
    return
  }
  const newTags = [...tags.value, inputTag.value.trim()]
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: JSON.stringify(newTags) })
  inputTag.value = ''
  isEditing.value = false
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') addTag()
  else if (e.key === 'Escape') isEditing.value = false
}
</script>

<template>
  <div class="w-full h-full flex items-center flex-wrap gap-1 p-1" @click="isEditing = true">
    <div v-for="(tag, idx) in tags" :key="idx" class="bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-1.5 py-0.5 rounded text-[10px] flex items-center gap-1">
      <span>{{ tag }}</span>
      <button @click.stop="removeTag(idx)" class="hover:text-white">&times;</button>
    </div>
    
    <div v-if="isEditing" class="flex-1 min-w-[50px]">
      <input 
        v-model="inputTag"
        @blur="addTag"
        @keydown="handleKeydown"
        type="text"
        placeholder="태그 입력..."
        class="w-full bg-slate-800 border border-slate-700 rounded px-1 py-0.5 text-[10px] text-white outline-none focus:border-indigo-500"
        autofocus
      />
    </div>
    <div v-else-if="tags.length === 0" class="text-xs text-slate-500 w-full text-center hover:text-slate-300 cursor-pointer">
      + 태그
    </div>
  </div>
</template>
