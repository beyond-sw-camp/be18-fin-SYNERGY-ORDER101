<template>
  <OrderDetailView title="발주 상세 정보" detailTitle="발주 세부 정보" orderNumberLabel="발주 번호" vendorLabel="공급업체"
    summaryTitle="발주 금액 요약" :orderData="orderData" :showApprovalButtons="showApprovalButtons">
    <template #actions="{ showButtons }">
      <PurchaseApprovalActions 
        v-if="showButtons"
        :poId="po.purchaseId"
        sourceType="REGULAR"
        entityName="발주"
        approveLabel="승인"
        rejectLabel="반려"
        @success="handleProcessSuccess"
      />
    </template>
  </OrderDetailView>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { getPurchaseDetail, updatePurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import PurchaseApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'
import OrderDetailView from '@/components/domain/order/OrderDetailView.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const poId = route.params.id || 'PO-UNKNOWN'

const po = reactive({
  purchaseId: null,
  poNo: '로딩 중...',
  supplierName: '',
  userName: '',
  requestedAt: '',
  status: '',
  items: []
})

const orderData = computed(() => ({
  orderNo: po.poNo,
  vendorName: po.supplierName,
  requesterName: po.userName,
  requestedAt: po.requestedAt,
  items: po.items
}))

const fetchPurchaseDetail = async () => {
  const data = await getPurchaseDetail(poId);
  Object.assign(po, {
    purchaseId: data.purchaseId,
    poNo: data.poNo,
    supplierName: data.supplierName,
    userName: data.requesterName,
    requestedAt: data.requestedAt,
    status: data.orderStatus,
  });

  po.items = data.purchaseItems.map(item => ({
    sku: item.productCode,
    name: item.productName,
    purchasePrice: item.purchasePrice,  // 공급가 (product_supplier)
    price: item.unitPrice,              // 판매가 (product)
    qty: item.orderQty,
  }));
}

// HQ_ADMIN이고, 상태가 SUBMITTED 또는 DRAFT_AUTO인 경우에만 승인/반려 버튼 표시
const isHqAdmin = computed(() => {
  const role = authStore.userInfo?.role
  return role === 'HQ_ADMIN'
})

const showApprovalButtons = computed(() => {
  const validStatus = po.status === 'SUBMITTED' || po.status === 'DRAFT_AUTO'
  return isHqAdmin.value && validStatus
})

function handleProcessSuccess() {
  router.push({ name: 'hq-orders-approval' })
}

onMounted(() => {
  fetchPurchaseDetail()
})
</script>

<style scoped>
.s-accepted {
  background: #16a34a;
}

.s-waiting {
  background: #d97706;
}

.s-rejected {
  background: #ef4444;
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

.input {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #f9fafb;
}

.actions-bottom {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-approve {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 10px 14px;
  border-radius: 8px;
}

.btn-reject {
  background: #fff;
  color: #6b46ff;
  border: 1px solid #c7b8ff;
  padding: 10px 14px;
  border-radius: 8px;
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

.numeric {
  text-align: right;
}

.btn-delete {
  background: transparent;
  border: none;
  color: #ef4444;
  cursor: pointer;
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
}

.approval-actions-wrapper {
  display: flex;
  justify-content: flex-end;
}
</style>
