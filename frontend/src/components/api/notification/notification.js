import { defineStore } from 'pinia'
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

      await Promise.all([this.fetchUnreadCount(), this.fetchNotifications(0)])

      this.connectSSE(token)
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

      es.onerror = () => {}
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
