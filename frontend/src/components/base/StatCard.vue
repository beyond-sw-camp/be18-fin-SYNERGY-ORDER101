<template>
    <div class="stat-card">
        <div class="card-title">{{ title }}</div>

        <div class="card-value-container">
            <span class="card-value">{{ value }}</span>
            <span v-if="unit" class="card-unit">{{ unit }}</span>
        </div>
    </div>
</template>

<script setup>
import { defineProps } from 'vue';

const props = defineProps({
    /**
     * 카드의 제목 (예: 총 순 금액)
     */
    title: {
        type: String,
        required: true,
    },
    /**
     * 표시할 실제 값 (SettlementReport.vue에서 이미 포맷팅된 문자열)
     */
    value: {
        type: [String, Number],
        required: true,
    },
    /**
     * 값 뒤에 붙일 단위 (예: '건', '%' 등)
     */
    unit: {
        type: String,
        default: '',
    },
});
</script>

<style scoped>
.stat-card {
    /* 카드 기본 스타일 */
    flex: 1;
    /* flex 컨테이너 내에서 균등한 공간 차지 */
    min-width: 200px;
    background-color: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    /* 그림자를 주어 강조 */
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.card-title {
    /* 제목 스타일 */
    font-size: 14px;
    color: #6c757d;
    /* 회색 텍스트 */
    margin-bottom: 10px;
    font-weight: 500;
}

.card-value-container {
    /* 값 컨테이너 */
    display: flex;
    align-items: flex-end;
}

.card-value {
    /* 실제 값 스타일 (크고 굵게) */
    font-size: 28px;
    font-weight: 700;
    color: #333;
    margin-right: 8px;
    /* 금액 표시를 위한 고정 폭 폰트 */
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.card-unit {
    /* 단위 스타일 */
    font-size: 16px;
    color: #6c757d;
    font-weight: 600;
    margin-bottom: 3px;
    /* 값의 하단과 맞추기 위해 약간 조정 */
}
</style>