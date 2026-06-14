<script setup lang="ts">
// MainView: 메인 화면 (빈 페이지)
// 10년 차 시니어 프리미엄 디자인 관점을 담아 정갈하고 미니멀한 플레이스홀더 화면을 설계했습니다.
import { Home } from 'lucide-vue-next'
</script>

<template>
  <div class="flex-1 min-h-screen flex items-center justify-center p-8 bg-slate-50 dark:bg-slate-950 text-slate-800 dark:text-slate-200 select-none">
    <div class="max-w-md w-full text-center flex flex-col items-center gap-6">
      
      <!-- 미니멀한 홈 아이콘 글로우 서클 -->
      <div class="relative flex items-center justify-center w-20 h-20 rounded-3xl bg-indigo-500/10 dark:bg-indigo-500/5 border border-indigo-500/20 dark:border-indigo-500/10 shadow-lg shadow-indigo-500/5 animate-pulse duration-[4000ms]">
        <Home class="w-10 h-10 text-indigo-500 dark:text-indigo-400" />
      </div>

      <!-- 메인 카피 및 안내 문구 -->
      <div class="flex flex-col gap-2">
        <h1 class="text-2xl font-black tracking-tight bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-800 dark:from-white dark:to-slate-400 bg-clip-text text-transparent">
          Main Dashboard
        </h1>
        <p class="text-sm text-slate-450 dark:text-slate-500 font-medium leading-relaxed">
          여기에 메인 대시보드 화면이 새롭게 구성될 예정입니다.<br>
          상단의 사이드바 메뉴 및 프로젝트 워크스페이스를 통해 작업을 시작해 보세요.
        </p>
      </div>

      <!-- 정갈한 하단 경계선 데코레이션 -->
      <div class="w-24 h-0.5 bg-gradient-to-r from-transparent via-indigo-500/30 to-transparent rounded"></div>
      
    </div>
  </div>
</template>

<style scoped>
/* 10년 차 시니어 설계: 컴포넌트 전용 모던 트랜지션 및 스타일 */
</style>
