import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
})

apiClient.interceptors.request.use(
  (config) => {
    try {
      const authStore = useAuthStore()
      const accessToken =
        (authStore.userInfo && authStore.userInfo.accessToken) || localStorage.getItem('authToken')
      if (accessToken) {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${accessToken}`
      }
    } catch (e) {
      // store may not be ready in some contexts
    }
    return config
  },
  (error) => Promise.reject(error),
)

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (!originalRequest) return Promise.reject(error)

    try {
      const status = error.response ? error.response.status : null
      const errorCode = error.response?.data?.code
      const errorMessage = error.response?.data?.message || ''
      
      // JWT 서명 오류 감지 (에러 코드 또는 메시지로 판단)
      const isJwtSignatureError = 
        errorCode === 'INVALID_TOKEN_SIGNATURE' ||
        errorMessage.includes('JWT signature does not match') ||
        errorMessage.includes('SignatureException')
      
      // JWT 서명 오류 또는 유효하지 않은 토큰 에러
      if ((status === 401 || status === 500 && isJwtSignatureError) && !originalRequest._retry) {
        originalRequest._retry = true
        const authStore = useAuthStore()
        
        // JWT 서명 에러는 refresh 시도하지 말고 즉시 로그아웃
        if (isJwtSignatureError) {
          console.warn('JWT signature mismatch detected. Clearing cache and logging out.')
          // 로컬 스토리지/세션 스토리지 완전 초기화
          localStorage.clear()
          sessionStorage.clear()
          authStore.logout()
          router.push({ name: 'login' })
          return Promise.reject(error)
        }
        
        // 일반 401은 토큰 refresh 시도
        const refreshed = await authStore.refreshAccessToken()
        if (refreshed) {
          const newToken = authStore.userInfo && authStore.userInfo.accessToken
          if (newToken) {
            originalRequest.headers = originalRequest.headers || {}
            originalRequest.headers.Authorization = `Bearer ${newToken}`
          }
          return apiClient(originalRequest)
        }
        
        // refresh failed: force logout and redirect to login
        localStorage.clear()
        sessionStorage.clear()
        authStore.logout()
        router.push({ name: 'login' })
      }
    } catch (e) {
      console.error('API response interceptor error', e)
    }
    return Promise.reject(error)
  },
)

export default apiClient
