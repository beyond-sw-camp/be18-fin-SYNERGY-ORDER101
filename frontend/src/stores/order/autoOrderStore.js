import { defineStore } from 'pinia'
import axios from 'axios'

export const useAutoOrderStore = defineStore('autoOrderStore', {
    state: () => ({
        items: [],
        details: [],
        selectedPurchase: null,
        page: 1,
        numOfRows: 5,
        totalCount: 0,
        totalPages: 1,
        loading: false,
        error: null,
    }),

    actions: {

        async fetchAutoOrders({ page = 1, numOfRows = 5 } = {}) {
            try {
                this.loading = true

                const res = await axios.get(`/api/v1/purchase-orders/auto`, {
                    params: { page, numOfRows }
                })

                this.items = res.data.items
                this.page = res.data.page
                this.totalCount = res.data.totalCount
                this.numOfRows = res.data.numOfRows
                this.totalPages = Math.ceil(this.totalCount / this.numOfRows)

            } catch (e) {
                console.error("자동발주 목록 조회 실패:", e)
            } finally {
                this.loading = false
            }
        },

        async searchAutoOrders({ supplierId = null, startDate = null, endDate = null, page = 1, numOfRows = 5 }) {
            try {
                this.loading = true

                const res = await axios.post(`/api/v1/purchase-orders/auto/search`, {
                    supplierId,
                    startDate,
                    endDate,
                    page,
                    numOfRows
                })

                this.items = res.data.items
                this.page = res.data.page
                this.totalCount = res.data.totalCount
                this.numOfRows = numOfRows
                this.totalPages = Math.ceil(this.totalCount / this.numOfRows)

            } catch (e) {
                console.error("자동발주 검색 실패:", e)
            } finally {
                this.loading = false
            }
        },

        async fetchAutoOrderDetail(purchaseId) {
            try {
                this.loading = true

                const res = await axios.get(`/api/v1/purchase-orders/auto/${purchaseId}`)
                const data = res.data

                if (data.items && data.items.length > 0) {
                    const detail = data.items[0]
                    this.details = detail.purchaseItems
                    this.selectedPurchase = {
                        purchaseId: detail.purchaseId,
                        poNo: detail.poNo,
                        supplierName: detail.supplierName,
                        userName: detail.userName,
                        requestedAt: detail.requestedAt
                    }
                } else {
                    this.details = []
                }

            } catch (e) {
                console.error("자동발주 상세 조회 실패:", e)
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

        async submitAutoOrder(purchaseId, items) {
            try {
                const res = await axios.put(`/api/v1/purchase-orders/auto/${purchaseId}/submit`, {
                    items: items.map(i => ({
                        purchaseDetailId: i.purchaseDetailId,
                        productId: i.productId,
                        orderQty: i.orderQty
                    }))
                })
                return res.data

            } catch (e) {
                console.error("자동발주 제출 실패:", e)
            }
        },
    }
})
