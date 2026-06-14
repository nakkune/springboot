<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from '@/components/layout/Sidebar.vue'
import TopHeader from '@/components/layout/TopHeader.vue'
import SettingsPopup from '@/components/ui/SettingsPopup.vue'

const isSettingsOpen = ref(false)
const settingsTab = ref('general')

const openSettings = (tab = 'general') => {
  settingsTab.value = tab
  isSettingsOpen.value = true
}

const closeSettings = () => {
  isSettingsOpen.value = false
}
</script>

<template>
  <div class="flex h-screen w-full overflow-hidden bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-slate-100">
    <Sidebar @open-settings="openSettings" />
    <div class="flex-1 flex flex-col h-full overflow-hidden">
      <TopHeader />
      <main class="flex-1 overflow-auto p-4 md:p-6">
        <router-view />
      </main>
    </div>
  </div>

  <!-- Settings Popup -->
  <SettingsPopup v-if="isSettingsOpen" :initialTab="settingsTab" @close="closeSettings" />
</template>
