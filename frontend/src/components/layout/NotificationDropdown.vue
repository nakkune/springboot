<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell, Check, AtSign, RefreshCcw } from 'lucide-vue-next'
import api from '@/services/api'

const notifications = ref<any[]>([])
const unreadCount = ref(0)
const isOpen = ref(false)
let eventSource: EventSource | null = null

const userId = 'f1b9b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c' // Mock current user

const fetchNotifications = async () => {
  try {
    const res = await api.get(`/notifications/user/${userId}`) as any
    notifications.value = res || []
    
    const countRes = await api.get(`/notifications/user/${userId}/unread-count`) as any
    unreadCount.value = countRes?.count || 0
  } catch (e) {
    console.error(e)
  }
}

const setupSSE = () => {
  eventSource = new EventSource(`http://localhost:9090/api/notifications/subscribe/${userId}`)
  
  eventSource.addEventListener('connect', (e: any) => {
    console.log('SSE Connected', e.data)
  })
  
  eventSource.addEventListener('notification', (e: any) => {
    const newNoti = JSON.parse(e.data)
    notifications.value.unshift(newNoti)
    unreadCount.value++
  })
  
  eventSource.onerror = (e) => {
    console.error('SSE Error', e)
    eventSource?.close()
    // Retry after 5s
    setTimeout(setupSSE, 5000)
  }
}

onMounted(() => {
  fetchNotifications()
  setupSSE()
})

onUnmounted(() => {
  eventSource?.close()
})

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
}

const markAsRead = async (id: string) => {
  try {
    await api.put(`/notifications/${id}/read`)
    const target = notifications.value.find(n => n.id === id)
    if (target && !target.isRead) {
      target.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch (e) {
    console.error(e)
  }
}
</script>

<template>
  <div class="relative">
    <button @click="toggleDropdown" class="text-slate-400 hover:text-white relative cursor-pointer active:scale-95 transition-transform p-1">
      <Bell class="w-4.5 h-4.5" />
      <span v-if="unreadCount > 0" class="absolute top-0 right-0 block h-2 w-2 rounded-full bg-rose-500 ring-2 ring-slate-900"></span>
    </button>
    
    <!-- Dropdown -->
    <Transition name="fade">
      <div v-if="isOpen" class="absolute right-0 top-full mt-2 w-80 bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl flex flex-col z-50 overflow-hidden">
        <div class="px-4 py-3 border-b border-slate-800 flex items-center justify-between bg-slate-900/50">
          <h3 class="text-sm font-bold text-white flex items-center gap-2">
            알림
            <span v-if="unreadCount > 0" class="bg-rose-500 text-white text-[10px] px-1.5 py-0.5 rounded-full">{{ unreadCount }}</span>
          </h3>
          <button @click="fetchNotifications" class="text-slate-400 hover:text-white transition-colors p-1" title="새로고침">
            <RefreshCcw class="w-3.5 h-3.5" />
          </button>
        </div>
        
        <div class="max-h-96 overflow-y-auto">
          <div v-if="notifications.length === 0" class="p-6 text-center text-slate-500 text-xs font-medium">
            새로운 알림이 없습니다.
          </div>
          
          <div 
            v-for="noti in notifications" 
            :key="noti.id"
            class="px-4 py-3 border-b border-slate-800 hover:bg-slate-800/50 transition-colors cursor-pointer group flex gap-3"
            :class="noti.isRead ? 'opacity-60' : 'bg-slate-800/20'"
            @click="markAsRead(noti.id)"
          >
            <div class="mt-1 shrink-0">
              <div v-if="noti.type === 'mention'" class="w-6 h-6 rounded-full bg-indigo-500/20 flex items-center justify-center">
                <AtSign class="w-3 h-3 text-indigo-400" />
              </div>
              <div v-else-if="noti.type === 'automation'" class="w-6 h-6 rounded-full bg-rose-500/20 flex items-center justify-center">
                <Bell class="w-3 h-3 text-rose-400" />
              </div>
              <div v-else class="w-6 h-6 rounded-full bg-slate-700 flex items-center justify-center">
                <Bell class="w-3 h-3 text-slate-400" />
              </div>
            </div>
            
            <div class="flex-1 min-w-0">
              <p class="text-xs font-bold text-slate-200 mb-0.5 truncate">{{ noti.title }}</p>
              <p class="text-[11px] text-slate-400 line-clamp-2 leading-relaxed">{{ noti.body }}</p>
              <p class="text-[9px] text-slate-500 mt-1 font-medium">{{ new Date(noti.createdAt).toLocaleString() }}</p>
            </div>
            
            <div v-if="!noti.isRead" class="shrink-0 w-2 h-2 rounded-full bg-indigo-500 self-center"></div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
