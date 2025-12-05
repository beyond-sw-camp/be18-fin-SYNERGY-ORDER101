import apiClient from '@/components/api'
import { defineStore } from 'pinia'

export const useInboundStore = defineStore('inbound', {
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
    async fetchInbound({ page = 1, numOfRows = 10 } = {}) {
      try {
        this.loading = true

        const res = await apiClient.get(`/api/v1/inbounds`, {
          params: { page, numOfRows },
        })

        this.items = res.data.items
        this.page = res.data.page
        this.totalCount = res.data.totalCount
        this.numOfRows = numOfRows
        this.totalPages = Math.ceil(this.totalCount / this.numOfRows)
      } catch (e) {
        console.error('입고 조회 실패: ', e)
      } finally {
        this.loading = false
      }
    },

    async fetchInboundDetail(inboundId) {
      try {
        const { data } = await apiClient.get(`/api/v1/inbounds/${inboundId}`)
        this.details = data.items[0].items
        this.selectedInboundNo = data.items[0].InboundNo
      } catch (e) {
        console.error('입고 상세 조회 실패: ', e)
      }
    },

    async searchInbound({
      supplierId = null,
      startDate = null,
      endDate = null,
      page = 1,
      numOfRows = 10,
    }) {
      try {
        this.loading = true

        const res = await apiClient.post(`/api/v1/inbounds/search`, {
          supplierId,
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
        console.error('입고 검색 실패: ', e)
      } finally {
        this.loading = false
      }
    },
  },
})
