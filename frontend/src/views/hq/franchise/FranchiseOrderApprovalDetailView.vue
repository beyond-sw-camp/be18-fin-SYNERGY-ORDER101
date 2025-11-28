<template>
    <OrderDetailView title="가맹점 주문 상세 정보" detailTitle="주문 세부 정보" orderNumberLabel="주문 번호" vendorLabel="가맹점"
        summaryTitle="총 결제 금액" :orderData="orderData" :showApprovalButtons="showApprovalButtons">
        <template #actions="{ showButtons }">
            <ApprovalActions v-if="showButtons" :order-id="route.params.id" :update-status-api="updateStoreOrderStatus"
                entity-name="주문" @success="handleProcessSuccess" />
        </template>
    </OrderDetailView>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import OrderDetailView from '@/components/domain/order/OrderDetailView.vue'
import ApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'
import { getStoreOrderById, updateStoreOrderStatus } from '@/components/api/store/StoreService'

const router = useRouter()
const route = useRoute()

const storeOrder = ref({})

const orderData = computed(() => ({
    orderNo: storeOrder.value.storeOrderNo,
    vendorName: storeOrder.value.storeName,
    requesterName: storeOrder.value.requesterName,
    requestedAt: storeOrder.value.orderDate,
    items: (storeOrder.value.orderItems || []).map(item => ({
        sku: item.productId,
        name: item.productName || `상품 #${item.productId}`,
        price: item.unitPrice,
        qty: item.orderQty
    }))
}))

const showApprovalButtons = computed(() => {
    return storeOrder.value.status === 'SUBMITTED'
})

const fetchOrderDetail = async () => {
    try {
        const { data } = await getStoreOrderById(route.params.id)
        storeOrder.value = data
    } catch (error) {
        console.error('주문 정보 조회 실패:', error)
        alert('주문 정보를 불러올 수 없습니다.')
        router.push({ name: 'hq-franchise-approval' })
    }
}

function handleProcessSuccess() {
    router.push({ name: 'hq-franchise-approval' })
}

onMounted(() => {
    fetchOrderDetail()
})
</script>
