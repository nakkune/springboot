<script setup lang="ts">
import { ref, computed } from 'vue'

interface StatusOption {
  id?: string
  label: string
  color: string
}

const props = defineProps<{
  value: string
  itemId: string
  columnId: string
  options: StatusOption[]
}>()

const emit = defineEmits<{
  (e: 'update', data: { itemId: string; columnId: string; value: string }): void
}>()

const isOpen = ref(false)

const selectedOption = computed(() => {
  return props.options.find(o => o.label === props.value) || null
})

const displayLabel = computed(() => {
  return selectedOption.value?.label || props.value || '시작 전'
})

const displayColor = computed(() => {
  return selectedOption.value?.color || '#64748b'
})

const selectOption = (opt: StatusOption) => {
  emit('update', { itemId: props.itemId, columnId: props.columnId, value: opt.label })
  isOpen.value = false
}

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
}
</script>

<template>
  <div class="w-full h-full flex items-center justify-center p-1 relative">
    <!-- Status Badge (10년 차 시니어: 셀 중앙에 아담하게 안착시키는 콤팩트 배지 구조) -->
    <div
      @click.stop="toggleDropdown"
      class="w-full max-w-[105px] h-[26px] flex items-center justify-center gap-1 rounded-md cursor-pointer transition-all hover:scale-[0.97] active:scale-[0.95] select-none relative px-2 shadow-sm"
      :style="{ backgroundColor: displayColor }"
    >
      <span class="text-white drop-shadow-sm font-bold text-[11px] tracking-tight truncate">{{ displayLabel }}</span>

      <!-- 드롭다운 아이콘 (은은하게 표시하여 직관성 확보) -->
      <svg
        class="w-2.5 h-2.5 text-white/80 shrink-0 transition-transform duration-200"
        :class="{ 'rotate-180': isOpen }"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </div>

    <!-- Dropdown Popover (배지 아래 정밀 중앙 매핑) -->
    <transition
      enter-active-class="transition duration-100 ease-out"
      enter-from-class="transform scale-95 opacity-0 -translate-y-1"
      enter-to-class="transform scale-100 opacity-100 translate-y-0"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="transform scale-100 opacity-100 translate-y-0"
      leave-to-class="transform scale-95 opacity-0 -translate-y-1"
    >
      <div
        v-if="isOpen"
        class="absolute top-full left-1/2 -translate-x-1/2 mt-1.5 w-[120px] bg-white dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700 rounded-xl shadow-2xl z-[999] p-1 flex flex-col gap-0.5"
      >
        <div
          v-for="opt in options"
          :key="opt.id || opt.label"
          @click.stop="selectOption(opt)"
          class="px-2 py-1.5 rounded-lg text-[11px] font-bold text-white transition-all cursor-pointer flex items-center justify-center gap-1.5 hover:scale-[1.02] hover:brightness-105"
          :style="{ backgroundColor: opt.color }"
        >
          <span>{{ opt.label }}</span>
          <span v-if="opt.label === selectedOption?.label" class="text-white/80 shrink-0">
            <svg class="w-2.5 h-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </span>
        </div>
      </div>
    </transition>
  </div>
</template>
