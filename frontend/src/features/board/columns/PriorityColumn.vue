<!-- src/features/board/columns/PriorityColumn.vue -->
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Star } from 'lucide-vue-next'

const props = defineProps<{
  value: string | null | undefined
  itemId: string
  columnId: string
}>()

const emit = defineEmits<{
  (e: 'update', data: { itemId: string, columnId: string, value: string }): void
}>()

// 호버 중인 임시 별점 (null이면 호버 안함)
const hoveredValue = ref<number | null>(null)

// 현재 데이터의 수치형 우선순위 값 (0 ~ 5)
const currentValue = computed(() => {
  if (!props.value) return 0
  const parsed = parseInt(props.value, 10)
  return isNaN(parsed) ? 0 : parsed
})

// 별 클릭 시 점수 토글 및 업데이트
const selectValue = (rating: number) => {
  let newValue = rating
  // 10년 차 시니어 설계: 이미 지정된 동일한 별점 별을 누르면 0점으로 리셋하는 극강의 토글 인터랙션
  if (currentValue.value === rating) {
    newValue = 0
  }
  
  emit('update', {
    itemId: props.itemId,
    columnId: props.columnId,
    value: String(newValue)
  })
}
</script>

<template>
  <div 
    class="w-full h-full flex items-center justify-center gap-0.5 select-none"
    @mouseleave="hoveredValue = null"
  >
    <!-- 🌟 [10년차 시니어 UI/UX 디자인]: 탄력적인 호버 리액션과 골드 그라데이션 광채의 별표 등급기 🌟 -->
    <button
      v-for="starIdx in 5"
      :key="starIdx"
      type="button"
      @mouseenter="hoveredValue = starIdx"
      @click.stop="selectValue(starIdx)"
      class="p-0.5 rounded-md hover:bg-slate-100 dark:hover:bg-slate-800/60 transition-colors focus:outline-none flex items-center justify-center cursor-pointer border-none bg-transparent shrink-0"
      :title="`${starIdx}순위 지정 (다시 누르면 초기화)`"
    >
      <Star 
        :stroke-width="1.4"
        class="w-3.5 h-3.5 transition-all duration-150 transform hover:scale-125 active:scale-95 text-center shrink-0"
        :class="[
          (hoveredValue !== null ? starIdx <= hoveredValue : starIdx <= currentValue)
            ? 'fill-amber-400 text-amber-400 drop-shadow-[0_0_2px_rgba(251,191,36,0.4)]'
            : 'fill-transparent text-slate-300 dark:text-slate-650 hover:text-amber-400 dark:hover:text-amber-500'
        ]"
      />
    </button>
  </div>
</template>

<style scoped>
/* 미세한 별빛 광채 트랜지션 애니메이션 */
.fill-amber-400 {
  transition: fill 0.15s cubic-bezier(0.4, 0, 0.2, 1), stroke 0.15s cubic-bezier(0.4, 0, 0.2, 1), filter 0.2s ease-in-out;
}
</style>
