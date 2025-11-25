import { defineStore } from 'pinia'
import axios from 'axios'

export const useInventoryStore = defineStore('inventory', {

    state: () => ({
        items: [],
        page: 1,
        numOfRows: 10,
        totalCount: 0,
        loading: false,
        error: null,
        categories: []
    }),

    actions: {
        async fetchInventory({ page = 1, numOfRows = 10 } = {}) {
            try {
                this.loading = true

                const res = await axios.get(`/api/v1/warehouses/inventory`, {
                    params: { page, numOfRows }
                })

                this.items = res.data.items
                this.page = res.data.page
                this.totalCount = res.data.totalCount
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
