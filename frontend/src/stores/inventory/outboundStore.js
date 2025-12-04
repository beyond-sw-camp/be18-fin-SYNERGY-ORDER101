import apiClient from '@/components/api'
import { defineStore } from 'pinia'

export const useOutboundStore = defineStore('outbound', {
  state: () => ({
    items: [],
    details: [],
    page: 1,
    numOfRows: 10,
    totalCount: 0,
    totalPages: 1,
    loading: false,
    error: null,
  }),

  actions: {
    async fetchOutbound({ page = 1, numOfRows = 10 } = {}) {
      try {
        this.loading = true

        const res = await apiClient.get(`/api/v1/outbounds`, {
          params: { page, numOfRows },
        })

        this.items = res.data.items
        this.page = res.data.page
        this.totalCount = res.data.totalCount
        this.totalPages = Math.ceil(this.totalCount / this.numOfRows)
      } catch (e) {
        console.error('출고 목록 조회 실패', e)
      } finally {
        this.loading = false
      }
    },

    async fetchOutboundDetail(outboundId) {
      try {
        const { data } = await apiClient.get(`/api/v1/outbounds/${outboundId}`)
        this.details = data.items[0].items
        this.selectedOutboundNo = data.items[0].outboundNo
      } catch (e) {
        console.error('출고 상세 조회 실패', e)
      }
    },

    async searchOutbound({
      storeId = null,
      startDate = null,
      endDate = null,
      page = 1,
      numOfRows = 10,
    }) {
      try {
        this.loading = true

        const res = await apiClient.post(`/api/v1/outbounds/search`, {
          storeId,
          startDate,
          endDate,
          page,
          numOfRows,
        })

        this.items = res.data.items
        this.page = res.data.page
        this.totalCount = res.data.totalCount
        this.numOfRows = numOfRows
        this.totalPages = Math.ceil(this.totalCount / this.numOfRows)
      } catch (e) {
        console.error('출고 검색 실패: ', e)
      } finally {
        this.loading = false
      }
    },
  },
})
