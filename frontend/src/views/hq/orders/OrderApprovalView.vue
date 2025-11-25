<template>
  <div class="order-approval">
    <div class="page-inner">
      <div class="left-col">
        <h2 class="title">발주 상세 정보</h2>

        <section class="card">
          <h3 class="card-title">발주 세부 정보</h3>
          <div class="form-row">
            <label>발주 번호</label>
            <input class="input" :value="po.poNo" readonly />
          </div>

          <div class="form-row">
            <label>공급업체 선택</label>
            <input class="input" :value="po.supplierName" readonly />
          </div>

          <div class="form-row">
            <label>요청 담당자</label>
            <input class="input" :value="po.userName" readonly />
          </div>

          <div class="form-row">
            <label>요청일자</label>
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
                <th>금액</th>
                <!-- <th>작업</th> -->
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in po.items" :key="row.sku + idx">
                <td>{{ row.sku }}</td>
                <td>{{ row.name }}</td>
                <td class="numeric">
                  <Money :value="row.price" />
                </td>
                <td class="numeric">{{ row.qty }}</td>
                <td class="numeric">
                  <Money :value="row.price * row.qty" />
                </td>
                <!-- <td> TODO: 일반 발주 수정 기능 
                  <button class="btn-delete" @click.prevent="removeItem(idx)">삭제</button>
                </td> -->
              </tr>
              <tr v-if="po.items.length === 0">
                <td colspan="6" class="empty">품목이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </section>
        <PurchaseApprovalActions v-if="poId" :po-id="poId" @success="handleProcessSuccess" />
        <!-- <div class="actions-bottom">
          <button class="btn-approve" @click="approve">승인</button>
          <button class="btn-reject" @click="reject">반려</button>
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
import { createWebHistory, useRoute, useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date.js'
import { getPurchaseDetail, updatePurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import PurchaseApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'

const route = useRoute()
const router = useRouter()
const poId = route.params.id || 'PO-UNKNOWN'

// sample PO data. In a real app we'd fetch by id.
const po = reactive({
  purchaseId: null,
  poNo: '로딩 중...', // 발주 번호
  supplierName: '', // 공급업체
  userName: '', // 요청 담당자
  requestedAt: '', // 요청일자
  items: [
    {
      sku: 'prod-001',
      name: 'default',
      price: 10,
      qty: 20,
    }
  ],
})

const fetchPurchaseDetail = async () => {

  // 2. await을 사용하여 API 호출이 완료되기를 기다립니다.
  const data = await getPurchaseDetail(poId);
  // 3. API 응답 데이터가 도착한 후 rows에 할당됩니다.
  Object.assign(po, {
    purchaseId: data.purchaseId,
    poNo: data.poNo,
    supplierName: data.supplierName,
    userName: data.requesterName,
    requestedAt: data.requestedAt,
    //items data.purchaseItems.productName,
  });

  po.items = data.purchaseItems.map(item => ({
    sku: item.purchaseId,
    name: item.productName,
    price: item.unitPrice,
    qty: item.orderQty,
  }));
}
onMounted(() => {
  // In a real app, fetch PO details by poId here
  fetchPurchaseDetail()
})


// TODO : 일반 발주 수정 기능
// function removeItem(idx) {
//   po.items.splice(idx, 1)
// }

const subtotal = computed(() => {
  return po.items.reduce((s, r) => s + Number(r.price || 0) * Number(r.qty || 0), 0) || 0
})
//const shipping = 50000
const total = computed(() => subtotal.value)

function approve() {
  console.log("승인 처리 호출 - PO ID:", poId);
  updatePurchaseStatus(poId, 'CONFIRMED');
  alert(`발주 ${poId}를 승인했습니다.`)
  router.back()
}
function reject() {
  updatePurchaseStatus(poId, 'REJECTED');
  alert(`발주 ${poId}를 반려했습니다.`)
  router.back()
}
</script>

<style scoped>
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
</style>
