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
        status: null,        // SUBMITTED, DRAFT_AUTO 등
        loading: false,
        error: null,
    }),

    actions: {
        async fetchAutoOrders({ status = null, page = 1, numOfRows = 5 } = {}) {
            try {
                this.loading = true

                const params = {
                page,
                numOfRows,
                }

                if (status) params.status = status   // status가 있을 때만 쿼리 주입

                const response = await axios.get('/api/v1/purchase-orders/auto', { params })
                const data = response.data

                this.items = data.items
                this.page = data.page
                this.totalCount = data.totalCount
                this.numOfRows = data.numOfRows

            } catch (e) {
                console.error("자동발주 목록 조회 실패", e)
            } finally {
                this.loading = false
            }
        },

        async fetchAutoOrderDetail(purchaseId) {
            try {
                this.loading = true

                const response = await axios.get(`/api/v1/purchase-orders/auto/${purchaseId}`)
                const data = response.data

                if (data.items && data.items.length > 0) {
                    const detail = data.items[0]

                    // purchaseItems만 details에 바인딩
                    this.details = detail.purchaseItems

                    // 만약 header 정보를 별도 사용하고 싶으면 추후 추가
                    this.selectedPurchase = {
                        purchaseId: detail.purchaseId,
                        poNo: detail.poNo,
                        supplierName: detail.supplierName,
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

        // // 페이지 이동 관련 메서드
        // async setPage(newPage) {
        //     this.page = newPage
        //     await this.fetchAutoOrders({
        //         status: this.status,
        //         page: this.page,
        //         numOfRows: this.numOfRows,
        //     })
        // },

        async setStatus(newStatus) {
            this.status = newStatus
            this.page = 1           // 필터 변경 시 페이지 1로 초기화
            await this.fetchAutoOrders({
                status: this.status,
                page: 1,
                numOfRows: this.numOfRows,
            })
        }
    }
})
