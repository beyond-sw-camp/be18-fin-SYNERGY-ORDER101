<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 승인</h1>
    </header>

    <section class="card">
      <h3 class="card-title">발주 요청 목록</h3>

      <div class="table-wrap">
        <table class="approval-table">
          <thead>
            <tr>
              <th>PO 번호</th>
              <th>공급업체</th>
              <th>품목 수</th>
              <th class="numeric">금액</th>
              <th>요청 날짜</th>
              <th>상태</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="clickable-row" @click="openDetail(row)">
              <td class="po">{{ row.No }}</td>
              <td>{{ row.vendor }}</td>
              <td class="numeric">{{ row.items }}</td>
              <td class="numeric">
                <Money :value="row.amount" />
              </td>
              <td>{{ formatDateTimeMinute(row.requestedAt) }}</td>
              <td>
                <span :class="['chip', statusClass(row.status)]">{{ row.status }}</span>
              </td>

              <td @click.stop>
                <PurchaseApprovalActions :po-id="row.id" @success="handleProcessSuccess" />
              </td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="7" class="no-data">발주 요청이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { getPurchases, updatePurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import { formatDateTimeMinute } from '@/components/global/Date';
import PurchaseApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'

const perPage = ref(10)
const page = ref(1)

onMounted(() => {
  // 초기 데이터 로드 등 필요한 작업 수행
  searchPurchases()
})

function handleProcessSuccess() {
  // 목록을 다시 불러와서 상태 변경 반영
  searchPurchases()
}

const searchPurchases = async () => {
  // // 발주 목록을 다시 불러오는 함수
  const data = await getPurchases(
    (page.value - 1) * perPage.value,
    perPage.value,
    '',
    'SUBMITTED'
  );

  rows.value = (data.content || []).map(item => ({
    id: item.purchaseId,
    No: item.poNo,
    vendor: item.supplierName,
    items: item.totalQty,
    amount: item.totalAmount,
    requestedAt: item.requestedAt,
    status: item.status,
  }));
}

const router = useRouter()

const rows = ref([])

function openDetail(row) {
  // navigate to approval detail page for this PO
  router.push({ name: 'hq-orders-approval-detail', params: { id: row.id } })
}

function statusClass(s) {
  if (!s) return ''
  if (s.includes('CON')) return 's-accepted'
  if (s.includes('SU')) return 's-waiting'
  if (s.includes('RE')) return 's-rejected'
  return ''
}
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  margin-bottom: 18px;
}

.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 16px;
}

.card-title {
  font-size: 16px;
  margin-bottom: 12px;
}

.table-wrap {
  margin-top: 12px;
}

.approval-table {
  width: 100%;
  border-collapse: collapse;
}

.approval-table th,
.approval-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
}

.approval-table td.numeric {
  text-align: right;
}

.po {
  font-weight: 600;
}

.chip {
  padding: 6px 10px;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
}

.s-accepted {
  background: #16a34a;
}

.s-waiting {
  background: #6b46ff;
}

.s-rejected {
  background: #ef4444;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn-accept {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-reject {
  background: #fff;
  color: #6b46ff;
  border: 1px solid #c7b8ff;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.clickable-row {
  cursor: pointer;
}

.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>
