import { defineStore } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import apiClient from '..'

const SSE_URL = (token) =>
  `https://order101.link/api/v1/sse/notifications?token=${encodeURIComponent(token)}`

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,

    es: null,
    connected: false,
    bootstrapped: false,
    lastEventId: null,

    page: 0, // 현재 서버 페이지 (0-based)
    size: 20, // 한 번에 가져올 개수
    totalCount: 0, // 서버에서 내려준 전체 개수
    loadingPage: false, // 페이지 로딩중 여부
  }),

  getters: {
    // 더 불러올 게 남았는지 여부
    hasMore: (s) => s.notifications.length < s.totalCount,
  },

  actions: {
    async init() {
      const token = localStorage.getItem('authToken')

      if (!token) {
        this.reset()
        return
      }

      // SSE 연결 전에 토큰 유효성 검증 (API 호출로 확인)
      try {
        await Promise.all([this.fetchUnreadCount(), this.fetchNotifications(0)])
        // API 호출이 성공하면 토큰이 유효함 -> SSE 연결
        this.connectSSE(token)
      } catch (error) {
        // API 호출 실패 시 (JWT 에러 포함) SSE 연결하지 않음
        console.warn('[Notification] Token validation failed, skipping SSE connection')
        this.reset()
      }
    },

    async fetchNotifications(page = 0, size = this.size) {
      if (this.loadingPage) return
      this.loadingPage = true

      try {
        const { data } = await apiClient.get('/api/v1/notifications', { params: { page, size } })

        const raw = data?.items?.content ?? data?.items ?? data?.content ?? data ?? []

        const list = Array.isArray(raw) ? raw : (raw.content ?? raw.notifications ?? [])

        this.notifications = page === 0 ? list : [...this.notifications, ...list]

        this.page = data.page ?? page
        this.totalCount = data.totalCount ?? this.totalCount
        this.size = size
      } finally {
        this.loadingPage = false
      }
    },

    async loadMore() {
      if (this.loadingPage || !this.hasMore) return
      const nextPage = this.page + 1
      await this.fetchNotifications(nextPage, this.size)
    },

    async fetchUnreadCount() {
      const { data } = await apiClient.get('/api/v1/notifications/unread-count')
      this.unreadCount = data.items?.[0]?.count ?? 0
    },

    connectSSE(token) {
      if (this.connected) return

      const es = new EventSource(SSE_URL(token))
      this.es = es
      this.connected = true

      es.addEventListener('connected', () => {})

      es.addEventListener('notification', (event) => {
        const payload = JSON.parse(event.data)
        const id = event.lastEventId

        // 1. 중복 방지(같은 탭에서 재연결 시)
        if (id && id === this.lastEventId) return
        this.lastEventId = id

        this.notifications.unshift(payload)

        this.unreadCount += 1

        this.totalCount += 1
      })

      es.addEventListener('error', () => {
        // SSE 연결 에러 - 401 (JWT 토큰 문제)일 가능성 높음
        console.warn('[SSE] Connection error detected')
        
        // 토큰 검증 실패 시 자동 로그아웃 처리
        if (es.readyState === EventSource.CLOSED) {
          console.warn('[SSE] Connection refused - likely JWT signature mismatch')
          
          // 현재 연결 정리
          this.connected = false
          if (this.es) {
            this.es.close()
            this.es = null
          }
          
          // JWT 서명 오류로 인한 사용자 강제 로그아웃
          const authStore = useAuthStore()
          localStorage.clear()
          sessionStorage.clear()
          authStore.logout()
          
          // 로그인 페이지로 리다이렉트
          import('@/router').then((routerModule) => {
            routerModule.default.push({ name: 'login' })
          })
          return
        }
        
        // 일반 에러는 재연결 시도
        this.connected = false
        if (this.es) {
          this.es.close()
          this.es = null
        }

        // 지수 backoff로 재연결
        setTimeout(() => {
          const t = localStorage.getItem('authToken')
          if (t) this.connectSSE(t)
        }, 3_000)
      })

      es.onerror = () => {
        console.warn('[SSE] error event triggered')
        // onerror 이벤트는 error 리스너로 통합됨
      }
    },

    disconnectSSE() {
      if (this.es) this.es.close()
      this.es = null
      this.connected = false
      this.lastEventId = null
    },
    async markAllRead() {
      await apiClient.post('/api/v1/notifications/read-all')
      this.unreadCount = 0
    },

    async deleteNotification(id) {
      await apiClient.delete(`/api/v1/notifications/${id}`)
      const targetId = Number(id)

      this.notifications = this.notifications.filter(
        (n) => Number(n.notificationId ?? n.id) !== targetId,
      )
      this.totalCount = Math.max(0, this.totalCount - 1)
    },
    async clearAll() {
      await apiClient.delete('/api/v1/notifications')

      this.notifications = []
      this.unreadCount = 0
      this.totalCount = 0 // 페이징 쓰고 있다면 이것도 0으로
      this.page = 0
    },

    reset() {
      this.disconnectSSE()
      this.notifications = []
      this.unreadCount = 0
      this.page = 0
      this.totalCount = 0
    },
  },
})
