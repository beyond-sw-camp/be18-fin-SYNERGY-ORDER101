import { defineStore } from 'pinia'
import axios from 'axios'

export const useInventoryStore = defineStore('inventory', {

    state: () => ({
        items: [],
        page: 1,
        numOfRows: 10,
        totalCount: 0,
        totalPages: 1,
        loading: false,
        error: null,
        categories: []
    }),

    actions: {
        async fetchInventory({ page = 1, numOfRows = 10, largeId = null, mediumId = null, smallId = null } = {}) {
            try {
                this.loading = true

                const params = { page, numOfRows } 
                // 카테고리 선택 시 QueryParam로 추가
                if (largeId) params.largeId = largeId
                if (mediumId) params.mediumId = mediumId
                if (smallId) params.smallId = smallId

                const res = await axios.get(`/api/v1/warehouses/inventory`, { params })

                this.items = res.data.items
                this.page = res.data.page
                this.totalCount = res.data.totalCount
                this.numOfRows = res.data.numOfRows
                this.totalPages = Math.ceil(this.totalCount / this.numOfRows)
            } catch (e) {
                console.error('재고 조회 실패:', e)
            } finally {
                this.loading = false
            }
        },

        refresh() {
            this.fetchInventory({})
        }
    }
})
