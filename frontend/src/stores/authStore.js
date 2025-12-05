import { reactive, computed } from 'vue'
import { defineStore } from 'pinia'
import apiClient from '@/components/api'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = reactive({
    accessToken: localStorage.getItem('authToken') || '',
    userId: localStorage.getItem('userId') || 0,
    phone: localStorage.getItem('phone') || '',
    name: localStorage.getItem('name') || '',
    role: localStorage.getItem('role') || '',
    storeId: localStorage.getItem('storeId') || '',
    type: '',
    issuedAt: 0,
    expiresAt: Number(localStorage.getItem('expiresAt')) || 0,
  })

  const isLoggedIn = computed(() => {
    return !!userInfo.accessToken && userInfo.expiresAt > Date.now()
  })

  const login = async (email, password, stayloggedin) => {
    try {
      const response = await apiClient.post('/api/v1/auth/login', { email, password })
      const data = response.data.items[0]

      const userName = data.userName || data.name || ''
      const role = data.role || (data.roles && data.roles[0]) || ''
      const expires = data.expiresAt || data.expireAt || data.expires || 0

      // If the backend indicates this user is a STORE_ADMIN, ensure storeId is present
      const backendStoreId = data.storeId == null ? null : String(data.storeId)
      if (
        (role === 'STORE_ADMIN' || data.type === 'STORE_ADMIN') &&
        (backendStoreId === null || backendStoreId === '')
      ) {
        // Missing storeId for a store admin is a fatal auth condition
        return {
          success: false,
          message: '매장 식별자(storeId)가 없습니다. 관리자에게 문의하세요.',
        }
      }

      Object.assign(userInfo, {
        accessToken: data.accessToken || '',
        userId: data.userId || data.id || 0,
        name: data.name || userName,
        phone: data.phone || '',
        role: data.role || role,
        storeId: backendStoreId || userInfo.storeId || '',
        type: data.type || role || '',
        issuedAt: Number(data.issuedAt) || userInfo.issuedAt,
        expiresAt: Number(expires) || 0,
      })

      const storage = stayloggedin ? localStorage : sessionStorage

      storage.setItem('authToken', userInfo.accessToken)
      storage.setItem('userId', String(userInfo.userId))
      storage.setItem('name', userInfo.name)
      storage.setItem('phone', userInfo.phone || '')
      storage.setItem('role', userInfo.role || '')
      storage.setItem('storeId', userInfo.storeId || '')
      storage.setItem('type', userInfo.type || '')
      storage.setItem('expiresAt', String(userInfo.expiresAt))

      return { success: true }
    } catch (error) {
      if (error.response && error.response.status === 404) {
        return { success: false, message: '이메일 또는 비밀번호를 확인하세요.' }
      } else {
        return { success: false, message: '로그인 중 오류가 발생했습니다.' }
      }
    }
  }

  const logout = () => {
    Object.assign(userInfo, {
      accessToken: '',
      userId: 0,
      name: '',
      phone: '',
      role: '',
      storeId: '',
      type: '',
      expiresAt: 0,
    })

    // localStorage와 sessionStorage 모두에서 모든 토큰 관련 데이터 삭제
    localStorage.removeItem('authToken')
    localStorage.removeItem('userId')
    localStorage.removeItem('name')
    localStorage.removeItem('phone')
    localStorage.removeItem('role')
    localStorage.removeItem('storeId')
    localStorage.removeItem('expiresAt')

    sessionStorage.removeItem('authToken')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('name')
    sessionStorage.removeItem('phone')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('storeId')
    sessionStorage.removeItem('expiresAt')
  }

  const refreshAccessToken = async () => {
    try {
      const response = await apiClient.post('/api/v1/auth/refresh')
      const tokenData = response.data.items[0]

      // 새로 받은 액세스 토큰 정보만 업데이트
      userInfo.accessToken = tokenData.accessToken
      userInfo.expiresAt = Number(tokenData.expiresAt) || userInfo.expiresAt
      localStorage.setItem('authToken', tokenData.accessToken)

      return true
    } catch (error) {
      // 토큰 재발급 실패 - 로그인 페이지로 리다이렉트 필요
    }
  }

  return { userInfo, login, logout, isLoggedIn, refreshAccessToken }
})
