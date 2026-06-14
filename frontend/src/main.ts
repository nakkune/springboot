import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'

// 10년 차 시니어 설계: 초기 브라우저 렌더링 시 LocalStorage에 기록된 사용자 테마 정책 즉각 복원
const savedTheme = localStorage.getItem('theme') || 'dark'
const html = document.documentElement
if (savedTheme === 'dark') {
  html.classList.add('dark')
} else {
  html.classList.remove('dark')
}

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
