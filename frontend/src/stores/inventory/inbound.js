import { defineStore } from 'pinia'
import axios from 'axios'

export const useInboundStore = defineStore('inbound', {

    state: () => ({
        items: [],
        loading: false,
        error: null,
    }),

    actions: {
        async fetchInbound({ page = 1, numOfRows = 20, totalCount = 0 } = {}) {
            const res = await axios.get(`/api/v1/inbounds`, {
                params: { page, numOfRows, totalCount }
            })
            this.items = res.data.items
            this.loading = false
        },

        async fetchInboundDetail(inboundId) {
            const { data } = await axios.get(`/api/v1/inbounds/${inboundId}`)
            this.details = data.items[0].items
            this.selectedInboundNo = data.items[0].InboundNo
        }
    }
})