<template>
  <div class="order-approval">
    <div class="page-inner">
      <div class="left-col">
        <h2 class="title">자동 발주 상세 정보</h2>

        <section class="card">
          <div class="form-row">
            <label>발주 번호</label>
            <input class="input" :value="po.poNo" readonly />
          </div>

          <div class="form-row">
            <label>공급업체</label>
            <input class="input" :value="po.supplierName" readonly />
          </div>

          <div class="form-row">
            <label>담당자</label>
            <input class="input" :value="po.userName" readonly />
          </div>

          <div class="form-row">
            <label>생성일자</label>
            <input class="input" :value="formatDateTimeMinute(po.requestedAt)" readonly />
          </div>
        </section>

        <section class="card">
          <h3 class="card-title">품목 세부 정보</h3>

          <table class="order-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>이름</th>
                <th>단가</th>
                <th>주문 수량</th>
                <th>안전 재고</th>
                <th>금액</th>
                <!-- <th>작업</th> -->
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in autoOrderStore.details" :key="row.purchaseDetailId" :class="{ modified: row.isModified }">
                <td>{{ row.productCode }}</td>
                <td>{{ row.productName }}</td>
                <td class="numeric">
                  <Money :value="row.unitPrice" />
                </td>
                  <td class="numeric">
                    <input
                      v-if="isDraft"
                      type="number"
                      v-model="row.orderQty"
                      class="qty-input"
                      @input="markModified(row)"
                    />
                    <span v-else>{{ row.orderQty }}</span>
                  </td>
                <td class="numeric">{{ row.safetyQty }}</td>
                <td class="numeric">
                  <Money :value="row.unitPrice * row.orderQty" />
                </td>
                <!-- <td>
                  <button v-if="isDraft" class="btn-delete" @click="removeItem(row.detailId)">
                    삭제
                  </button>
                </td> -->
              </tr>
              <tr v-if="autoOrderStore.details.length === 0">
                <td colspan="6" class="empty">품목이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </section>

        <div class="approval-actions-wrapper" v-if="showApprovalActions">
          <PurchaseApprovalActions />
        </div>

        <div class="actions-bottom" v-if="showSubmitButton">
          <button class="btn primary" @click="submit">제출</button>
        </div>
<!-- 
        <div class="actions-bottom" v-if="po.status === 'DRAFT_AUTO'">
          <button class="btn primary" @click="submit">제출</button>
        </div> -->
      </div>

      <aside class="right-col">
        <div class="summary card">
          <h4>발주 금액 요약</h4>
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
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date.js'
import PurchaseApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'
import { useAutoOrderStore } from '@/stores/order/autoOrderStore'

const autoOrderStore = useAutoOrderStore()
const route = useRoute()
const router = useRouter()
const poId = route.params.purchaseId

const isDraft = computed(() => po.status === 'DRAFT_AUTO')

const showApprovalActions = computed(() => po.status === 'SUBMITTED')
const showSubmitButton = computed(() => po.status === 'DRAFT_AUTO')


// PO 정보 영역
const po = reactive({
  purchaseId: null,
  poNo: '',
  supplierName: '',
  userName: '',
  requestedAt: '',
  status: '',
})

const fetchPurchaseDetail = async () => {
  await autoOrderStore.fetchAutoOrderDetail(poId) // API 호출

  // store.details는 purchaseItems만 들어있음
  // Header 정보는 받아온 detail의 첫 row에서 추출
  const first = autoOrderStore.details[0]

  Object.assign(po, {
    purchaseId: autoOrderStore.selectedPurchase?.purchaseId,
    poNo: autoOrderStore.selectedPurchase?.poNo,
    supplierName: autoOrderStore.selectedPurchase?.supplierName,
    userName: autoOrderStore.selectedPurchase?.userName,
    requestedAt: autoOrderStore.selectedPurchase?.requestedAt,
    status: autoOrderStore.selectedPurchase?.status,
  })
}

// 요약 금액 계산
const subtotal = computed(() => {
  return autoOrderStore.details.reduce((sum, r) =>
    sum + Number(r.unitPrice || 0) * Number(r.orderQty || 0)
  , 0)
})

// 변경감지용
function markModified(row) {
  row.isModified = true
}

// 품목 삭제
function removeItem(detailId) {
  autoOrderStore.details = autoOrderStore.details.filter(d => d.detailId !== detailId)
}

// 제출 API
async function submit() {
  await autoOrderStore.submitAutoOrder(po.purchaseId, autoOrderStore.details)
  await fetchPurchaseDetail()
}

const total = computed(() => subtotal.value)

function handleProcessSuccess() {
  router.back()
}

onMounted(() => {
  fetchPurchaseDetail()
})

// 담당자 변경
const displayUserName = computed(() => {
  if (po.status === 'DRAFT_AUTO') return 'AUTO'   // 초안 상태일 때
  return po.userName || '-'                       // 제출 이후
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

.btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
}
.btn.primary {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
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
  text-align: center;
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

/* 수정된 행 강조 */
.modified {
  background: #f3e8ff !important; /* 연보라 */
  font-style: italic;
}
.qty-input {
  width: 80px;
  padding: 6px;
  border: 1px solid #d7ccfc;
  border-radius: 6px;
}
</style>