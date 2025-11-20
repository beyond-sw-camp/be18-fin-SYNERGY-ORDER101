import { defineStore } from 'pinia'
import axios from 'axios'

export const useInventoryStore = defineStore('inventory', {

    state: () => ({
        items: [],
        loading: false,
        error: null,
        categories: []
    }),

    actions: {
        async fetchInventory({ page = 1, numOfRows = 20, totalCount = 0 } = {}) {
            try {
                this.loading = true

                const res = await axios.get(`/api/v1/warehouses/inventory`, {
                    params: { page, numOfRows, totalCount }
                })

                this.items = res.data.items
            } catch (e) {
                this.error = e
                console.error('재고 조회 실패:', e)
            } finally {
                this.loading = false
            }
        },

        // async loadCategories({ parentId = 1 } = {}) {
        //     try {
        //         this.loading = true

        //         const res = await axios.get('/api/v1/categories/${parentId}/children', {
        //             params: { parentId }
        //         })
        //     this.categories = res.data
        //     } catch (e) {
        //         console.warn('카테고리 로드 실패:', e)
        //         largeCategories.value = []
        //     } finally {
        //         this.loading = false
        //     }
        // },

        refresh() {
            this.fetchInventory({})
        }
    }
})
