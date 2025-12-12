import apiClient from '@/components/api'
import { defineStore } from 'pinia'

export const useAutoOrderStore = defineStore('autoOrderStore', {
  state: () => ({
    items: [],
    details: [],
    originalItems: [],
    editedItems: [],
    selectedPurchase: null,
    page: 1,
    numOfRows: 10,
    totalCount: 0,
    totalPages: 1,
    loading: false,
    error: null,
  }),

  actions: {
    async fetchAutoOrders({ page = 1, numOfRows = 10 } = {}) {
      try {
        this.loading = true

        const res = await apiClient.get(`/api/v1/purchase-orders/auto`, {
          params: { page, numOfRows },
        })

        this.items = res.data.items
        this.page = res.data.page
        this.totalCount = res.data.totalCount
        this.totalPages = Math.ceil(this.totalCount / this.numOfRows)
      } catch (e) {
        // 자동발주 목록 조회 실패
      } finally {
        this.loading = false
      }
    },

    async searchAutoOrders({
      supplierId = null,
      startDate = null,
      endDate = null,
      page = 1,
      numOfRows = 5,
    }) {
      try {
        this.loading = true

        const res = await apiClient.post(`/api/v1/purchase-orders/auto/search`, {
          supplierId,
          startDate,
          endDate,
          page,
          numOfRows,
        })
        
        this.items = res.data.items
        this.page = res.data.page ?? 1
        this.totalCount = res.data.totalCount ?? 0
        this.numOfRows = numOfRows

        const total = Number(this.totalCount) || 0
        const rows = Number(this.numOfRows) || 1
        this.totalPages = Math.max(1, Math.ceil(total / rows))
      } catch (e) {
        // 자동발주 검색 실패
      } finally {
        this.loading = false
      }
    },

    async fetchAutoOrderDetail(purchaseId) {
      try {
        this.loading = true

        const res = await apiClient.get(`/api/v1/purchase-orders/auto/${purchaseId}`)
        const data = res.data

        if (data.items && data.items.length > 0) {
          const detail = data.items[0]
          this.details = detail.purchaseItems
          this.selectedPurchase = {
            purchaseId: detail.purchaseId,
            poNo: detail.poNo,
            supplierName: detail.supplierName,
            userName: detail.userName,
            requestedAt: detail.requestedAt,
            status: detail.status,
          }

          this.originalItems = structuredClone(detail.purchaseItems)
          this.editedItems = structuredClone(detail.purchaseItems).map((i) => ({
            ...i,
            isModified: false,
          }))
        } else {
          this.details = []
        }
      } catch (e) {
        // 자동발주 상세 조회 실패
        this.details = []
      } finally {
        this.loading = false
      }
    },

    async setStatus(newStatus) {
      this.status = newStatus
      this.page = 1
      await this.fetchAutoOrders({ page: 1, numOfRows: this.numOfRows })
    },

    async submitAutoOrder(purchaseId) {
      const requestItems = this.originalItems.map((orig) => {
        const edited = this.editedItems.find((e) => e.productId === orig.productId)
        return {
          purchaseDetailId: orig.detailId,
          productId: orig.productId,
          orderQty: edited.orderQty,
        }
      })

      try {
        const res = await apiClient.patch(`/api/v1/purchase-orders/auto/${purchaseId}/submit`, {
          items: requestItems,
        })

        return res.data
      } catch (e) {
        // 자동발주 제출 실패
      }
    },

    async updateStatus(purchaseId, status) {
      try {
        const res = await apiClient.patch(
          `/api/v1/purchase-orders/auto/${purchaseId}/status`,
          null,
          { params: { status } },
        )

        await this.fetchAutoOrderDetail(purchaseId)
        return res.data
      } catch (e) {
        // 상태 변경 실패
        throw e
      }
    },
  },
})
