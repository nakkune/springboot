<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHrStore, type Employee } from '@/stores/erp/useHrStore'
import { Loader2, ArrowLeft, Briefcase } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const hrStore = useHrStore()

const employee = ref<Employee | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const id = route.params.id as string
    // For now navigate back to list - detail view is placeholder
    // In a full implementation, fetch single employee via store
    router.replace('/hr/employees')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="p-6 max-w-4xl mx-auto">
    <div class="flex items-center justify-center py-20">
      <Loader2 class="w-8 h-8 text-indigo-500 animate-spin" />
    </div>
  </div>
</template>
