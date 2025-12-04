import apiClient from '@/components/api'
import { defineStore } from 'pinia'

export const useInventoryStore = defineStore('inventory', {
  state: () => ({
    items: [],
    page: 1,
    numOfRows: 10,
    totalCount: 0,
    totalPages: 1,
    loading: false,
    error: null,
  }),

  actions: {
    async fetchInventory({
      page = 1,
      numOfRows = 10,
      largeId = null,
      mediumId = null,
      smallId = null,
    } = {}) {
      try {
        this.loading = true

        const params = { page, numOfRows }

        if (largeId) params.largeId = largeId
        if (mediumId) params.mediumId = mediumId
        if (smallId) params.smallId = smallId

        const res = await apiClient.get(`/api/v1/warehouses/inventory`, { params })

        this.items = res.data.items
        this.page = res.data.page
        this.totalCount = res.data.totalCount
        this.totalPages = Math.ceil(this.totalCount / this.numOfRows)
      } catch (e) {
        console.error('재고 조회 실패:', e)
      } finally {
        this.loading = false
      }
    },

    refresh() {
      this.fetchInventory({})
    },
  },
})
