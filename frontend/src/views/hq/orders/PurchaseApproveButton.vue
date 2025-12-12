<template>
    <div class="actions-container">
        <!-- 승인 버튼 -->
        <button class="btn btn-approve" @click="handleAction('CONFIRMED')" :disabled="isProcessing">
            <span v-if="isProcessing && actionType === 'CONFIRMED'" class="spinner"></span>
            <span v-else>{{ approveLabel }}</span>
        </button>

        <!-- 반려 버튼 -->
        <button class="btn btn-reject" @click="handleAction('REJECTED')" :disabled="isProcessing">
            <span v-if="isProcessing && actionType === 'REJECTED'" class="spinner"></span>
            <span v-else>{{ rejectLabel }}</span>
        </button>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { updatePurchaseStatus, updateSmartOrderStatus } from '@/components/api/purchase/purchaseService.js'

// 부모로부터 받을 데이터 정의
const props = defineProps({
    orderId: {
        type: [String, Number],
        required: false
    },
    poId: {
        type: [String, Number],
        required: false
    },
    sourceType: {
        type: String,
        default: 'REGULAR' // 'REGULAR' or 'SMART'
    },
    smartOrderIds: {
        type: Array,
        default: () => []
    },
    updateStatusApi: {
        type: Function,
        required: false
    },
    entityName: {
        type: String,
        default: '주문'
    },
    approveLabel: {
        type: String,
        default: '승인'
    },
    rejectLabel: {
        type: String,
        default: '반려'
    }
})

// 부모에게 보낼 이벤트 정의
const emit = defineEmits(['success', 'error'])

// 로딩 상태 관리
const isProcessing = ref(false)
const actionType = ref(null) // 현재 눌린 버튼 추적

const handleAction = async (status) => {
    const label = status === 'CONFIRMED' ? props.approveLabel : props.rejectLabel
    const id = props.orderId || props.poId

    if (!confirm(`정말로 이 ${props.entityName}을(를) ${label} 하시겠습니까?`)) return

    try {
        isProcessing.value = true
        actionType.value = status

        // 스마트 발주인 경우
        if (props.sourceType === 'SMART' && props.smartOrderIds && props.smartOrderIds.length > 0) {
            await updateSmartOrderStatus(props.smartOrderIds, status)
            alert(`스마트 발주가 정상적으로 ${label} 되었습니다.`)
        }
        // 일반 발주인 경우
        else if (props.updateStatusApi) {
            await props.updateStatusApi(id, status)
            alert(`${props.entityName}이(가) 정상적으로 ${label} 되었습니다.`)
        }
        // 기본 일반 발주 처리
        else {
            await updatePurchaseStatus(id, status)
            alert(`발주가 정상적으로 ${label} 되었습니다.`)
        }

        // 부모에게 성공 이벤트 발송
        emit('success', status)

    } catch (error) {
        alert(`${label} 처리 중 오류가 발생했습니다.`)
        emit('error', error)
    } finally {
        isProcessing.value = false
        actionType.value = null
    }
}
</script>

<style scoped>
.actions-container {
    display: flex;
    justify-content: center;
    gap: 8px;
}

.btn {
    padding: 10px 20px;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 80px;
}

.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

/* 승인 버튼 스타일 (보라색 채움) */
.btn-approve {
    background: #6b46ff;
    color: #fff;
    border: 1px solid transparent;
}

.btn-approve:hover:not(:disabled) {
    background: #5a37e6;
}

/* 반려 버튼 스타일 (보라색 테두리) */
.btn-reject {
    background: #fff;
    color: #6b46ff;
    border: 1px solid #c7b8ff;
}

.btn-reject:hover:not(:disabled) {
    background: #f5f3ff;
}

/* 간단한 로딩 스피너 */
.spinner {
    width: 14px;
    height: 14px;
    border: 2px solid currentColor;
    border-top-color: transparent;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}
</style>