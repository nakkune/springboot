import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:9090/api', // [NEW] 백엔드 servlet.context-path=/api 규격 연동
  timeout: 300000, // 5분 (파일 업로드 대응)
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request Interceptor
api.interceptors.request.use(
  (config) => {
    // 10년 차 시니어 설계: 브라우저 로컬스토리지에서 토큰을 추출하여 Bearer 규격으로 안전하게 주입
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response Interceptor
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    // 공통 에러 처리 로직: 인증(세션) 만료 시 자동 로그아웃 및 로그인 페이지 튕김 처리
    if (error.response && error.response.status === 401) {
      console.warn('세션이 만료되었거나 인증되지 않았습니다. 로그인 페이지로 이동합니다.')
      localStorage.removeItem('token')
      localStorage.removeItem('isAuthenticated')
      localStorage.removeItem('userEmail')
      localStorage.removeItem('userName')
      localStorage.removeItem('avatarUrl')
      
      // router 객체를 직접 사용할 경우 순환참조가 발생할 수 있어 window.location.href 사용
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
