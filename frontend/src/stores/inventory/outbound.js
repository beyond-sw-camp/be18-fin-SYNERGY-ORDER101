import { defineStore } from 'pinia'
import axios from 'axios'

export const useOutboundStore = defineStore('outbound', {

    state: () => ({
        items: [],
        loading: false,
        error: null,
        details: []
    }),

    actions: {
        async fetchOutbound({ page = 1, numOfRows = 20, totalCount = 0 } = {}) {
            const res = await axios.get(`/api/v1/outbounds`, {
                params: { page, numOfRows, totalCount }
            })
            this.items = res.data.items
            this.loading = false
        },

          async fetchOutboundDetail(outboundId) {
            const { data } = await axios.get(`/api/v1/outbounds/${outboundId}`)
            this.details = data.items[0].items
            this.selectedOutboundNo = data.items[0].outboundNo
        }
    }
})