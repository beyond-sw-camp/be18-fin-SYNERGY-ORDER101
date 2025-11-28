import { defineStore } from 'pinia'
import axios from 'axios'

const SSE_URL = (token) =>
  `http://localhost:8080/api/v1/sse/notifications?token=${encodeURIComponent(token)}`

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,

    es: null,
    connected: false,
    bootstrapped: false,
    lastEventId: null,
  }),

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

    async fetchNotifications(page = 0, size = 20) {
      const { data } = await axios.get('/api/v1/notifications', { params: { page, size } })

      const raw = data?.items?.content ?? data?.items ?? data?.content ?? data ?? []

      const list = Array.isArray(raw) ? raw : (raw.content ?? raw.notifications ?? [])

      this.notifications = page === 0 ? list : [...this.notifications, ...list]
    },

    async fetchUnreadCount() {
      const { data } = await axios.get('/api/v1/notifications/unread-count')
      this.unreadCount = data.items?.[0]?.count ?? 0
      console.log(this.unreadCount)
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
      await axios.post('/api/v1/notifications/read-all')
      this.unreadCount = 0
    },

    async deleteNotification(id) {
      await axios.delete(`/api/v1/notifications/${id}`)
      const targetId = Number(id)

      this.notifications = this.notifications.filter(
        (n) => Number(n.notificationId ?? n.id) !== targetId,
      )
    },

    reset() {
      this.disconnectSSE()
      this.notifications = []
      this.unreadCount = 0
    },
  },
})
