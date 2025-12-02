<template>
    <div class="order-approval">
        <div class="page-inner">
            <div class="left-col">
                <h2 class="title">{{ title }}</h2>

                <section class="card">
                    <h3 class="card-title">{{ detailTitle }}</h3>
                    <div class="form-row">
                        <label>{{ orderNumberLabel }}</label>
                        <input class="input" :value="orderData.orderNo" readonly />
                    </div>

                    <div class="form-row">
                        <label>{{ vendorLabel }}</label>
                        <input class="input" :value="orderData.vendorName" readonly />
                    </div>

                    <div class="form-row">
                        <label>요청 담당자</label>
                        <input class="input" :value="orderData.requesterName" readonly />
                    </div>

                    <div class="form-row">
                        <label>요청일자</label>
                        <input class="input" :value="formatDateTimeMinute(orderData.requestedAt)" readonly />
                    </div>
                </section>

                <section class="card">
                    <h3 class="card-title">품목 세부 정보</h3>

                    <table class="order-table">
                        <thead>
                            <tr>
                                <th class="text-center">SKU</th>
                                <th class="text-center">이름</th>
                                <th class="text-right">주문 수량</th>
                                <th class="text-right">공급가</th>
                                <th class="text-right">판매가</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(row, idx) in (orderData.items || [])" :key="`item-${idx}-${row.sku || row.productId || idx}`">
                                <td class="text-center">{{ row.sku }}</td>
                                <td class="text-center">{{ row.name }}</td>
                                <td class="text-right">{{ row.qty }}</td>
                                <td class="text-right">
                                    <Money :value="row.purchasePrice || row.price" />
                                </td>
                                <td class="text-right">
                                    <Money :value="row.price" />
                                </td>
                            </tr>
                            <tr v-if="!orderData.items || orderData.items.length === 0">
                                <td colspan="5" class="empty">품목이 없습니다.</td>
                            </tr>
                        </tbody>
                    </table>
                </section>

                <div class="approval-actions-wrapper">
                    <slot name="actions" :order="orderData" :showButtons="showApprovalButtons"></slot>
                </div>
            </div>

            <aside class="right-col">
                <div class="summary card">
                    <h4>{{ summaryTitle }}</h4>
                    <div class="summary-row">
                        <span>소계:</span><span class="numeric">
                            <Money :value="subtotal" />
                        </span>
                    </div>

                    <hr />
                    <div class="summary-row total">
                        <span>총액:</span><span class="numeric">
                            <Money :value="total" />
                        </span>
                    </div>
                </div>
            </aside>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date.js'

const props = defineProps({
    title: {
        type: String,
        default: '상세 정보'
    },
    detailTitle: {
        type: String,
        default: '세부 정보'
    },
    orderNumberLabel: {
        type: String,
        default: '주문 번호'
    },
    vendorLabel: {
        type: String,
        default: '업체'
    },
    summaryTitle: {
        type: String,
        default: '금액 요약'
    },
    orderData: {
        type: Object,
        required: true
    },
    showApprovalButtons: {
        type: Boolean,
        default: false
    }
})

const subtotal = computed(() => {
    const items = props.orderData.items || []
    return items.reduce((s, r) => s + Number(r.price || 0) * Number(r.qty || 0), 0) || 0
})

const total = computed(() => subtotal.value)
</script>

<style scoped>
.order-approval {
    padding: 24px 32px;
}

.page-inner {
    display: flex;
    gap: 24px;
    align-items: flex-start;
}

.left-col {
    flex: 1;
}

.right-col {
    width: 320px;
}

.title {
    margin: 8px 0 16px;
}

.card {
    background: #fff;
    border: 1px solid #eef2f7;
    padding: 16px;
    border-radius: 8px;
    margin-bottom: 16px;
}

.card-title {
    font-size: 16px;
    margin-bottom: 12px;
}

.form-row {
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
}

.form-row label {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 4px;
    color: #374151;
}

.input {
    padding: 8px 10px;
    border-radius: 6px;
    border: 1px solid #e2e8f0;
    background: #f9fafb;
}

.order-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 12px;
}

.order-table th,
.order-table td {
    padding: 12px;
    border-top: 1px solid #f7f7f9;
}

.order-table th {
    background: #f9fafb;
    font-weight: 600;
}

.text-center {
    text-align: center;
}

.text-right {
    text-align: right;
}

.numeric {
    text-align: right;
}

.empty {
    text-align: center;
    color: #9ca3af;
    padding: 18px;
}

.summary-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
}

.total {
    font-weight: 700;
    color: #4f46e5;
    border-top: 2px solid #e5e7eb;
    padding-top: 12px;
    margin-top: 8px;
}

.approval-actions-wrapper {
    display: flex;
    justify-content: flex-end;
}
</style>
