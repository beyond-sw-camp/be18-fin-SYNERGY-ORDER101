import { defineStore } from 'pinia'
import axios from 'axios'

export const useAutoOrderStore = defineStore('autoOrderStore', {
    state: () => ({
        items: [],
        details: [],
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
                this.details = response.data.items

            } catch (e) {
                console.error("자동발주 상세 조회 실패:", e)
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
