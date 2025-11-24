import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
  state: () => ({
    isLoggedIn: true,
    accessToken: 'demo-access-token',
    user: {
      id: 15,
      name: 'test',
      tier: 'BRONZE',
    },
  }),
  actions: {
    setLoggedIn(flag) {
      this.isLoggedIn = flag
    },
    setAccessToken(token) {
      this.accessToken = token
    },
  },
})
