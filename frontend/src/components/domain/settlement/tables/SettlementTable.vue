<template>
    <div v-if="!tableData || tableData.length === 0" class="no-data">
        <p>조회된 정산 요약 데이터가 없습니다.</p>
    </div>

    <table v-else class="settlement-table">
        <thead>
            <tr>
                <th>가맹점/공급업체</th>
                <th>결제 건수</th>
                <th>상품 금액</th>
                <th>순 금액</th>
                <th>마감 상태</th>
            </tr>
        </thead>
        <tbody>
            <tr v-for="(item, index) in tableData" :key="index">
                <td>{{ item.vendor }}</td>
                <td class="text-right">{{ item.count }}건</td>
                <td class="text-right">{{ formatCurrency(item.total) }}</td>
                <td class="text-right">{{ formatCurrency(item.net) }}</td>
                <td>
                    <span :class="['status-badge', getStatusClass(item.status)]">
                        {{ item.status }}
                    </span>
                </td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { defineProps } from 'vue';

const props = defineProps({
    /**
     * @typedef {object} TableItem
     * @property {string} vendor - 가맹점/공급업체 이름
     * @property {number} count - 결제 건수
     * @property {number} total - 상품 금액 (총액)
     * @property {number} net - 순 금액 (정산 대상 금액)
     * @property {string} status - 마감 상태 ('완료', '처리 중', '미결제' 등)
     */
    /** @type {import('vue').PropType<TableItem[]>} */
    tableData: Array, // SettlementReport.vue에서 전달받은 summaryTable 데이터
});

/**
 * 숫자를 KRW 포맷의 통화 문자열로 변환합니다.
 * @param {number} amount
 * @returns {string}
 */
const formatCurrency = (amount) => {
    if (typeof amount !== 'number') return '₩0';
    return `₩${amount.toLocaleString('ko-KR')}`;
};

/**
 * 마감 상태에 따라 CSS 클래스를 반환합니다.
 * @param {string} status
 * @returns {string}
 */
const getStatusClass = (status) => {
    switch (status) {
        case '완료': return 'status-completed';
        case '처리 중': return 'status-pending';
        case '미결제': return 'status-unpaid';
        default: return '';
    }
};
</script>

<style scoped>
.settlement-table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
    font-size: 14px;
    background-color: white;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    border-radius: 6px;
    overflow: hidden;
    /* 라운딩 처리를 위해 필요 */
}

.settlement-table th,
.settlement-table td {
    padding: 12px 15px;
}

.settlement-table th {
    background-color: #f7f9fc;
    font-weight: 600;
    color: #333;
    border-bottom: 2px solid #e0e0e0;
}

.settlement-table td {
    border-bottom: 1px solid #f0f0f0;
}

.settlement-table tbody tr:last-child td {
    border-bottom: none;
}

.text-right {
    text-align: right;
    font-family: monospace, sans-serif;
    /* 금액 강조 */
}

/* 상태 뱃지 스타일링 */
.status-badge {
    display: inline-block;
    padding: 4px 10px;
    border-radius: 14px;
    font-size: 0.85em;
    font-weight: 700;
    color: white;
    text-align: center;
}

.status-completed {
    /* '완료' (파란색) */
    background-color: #4a90e2;
}

.status-pending {
    /* '처리 중' (주황색) */
    background-color: #ff9800;
}

.status-unpaid {
    /* '미결제' (빨간색) */
    background-color: #e53935;
}

.no-data {
    text-align: center;
    padding: 40px;
    color: #6c757d;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
</style>