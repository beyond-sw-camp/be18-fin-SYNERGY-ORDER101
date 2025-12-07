import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 5000,
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
      if (status === 401 && !originalRequest._retry) {
        originalRequest._retry = true
        const authStore = useAuthStore()
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
