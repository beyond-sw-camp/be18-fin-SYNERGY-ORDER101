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
              <th class="center">품목 수</th>
              <th class="center">금액</th>
              <th class="center">요청 날짜</th>
              <th class="center">상태</th>
              <th class="center">타입</th>
              <th class="center">작업</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="clickable-row" @click="openDetail(row)">
              <td class="po">{{ row.No }}</td>
              <td>{{ row.vendor }}</td>
              <td class="center">{{ row.items }}</td>
              <td class="center">
                <Money :value="row.amount" />
              </td>
              <td class="center">{{ formatDateTimeMinute(row.requestedAt) }}</td>
              <td class="center">
                <span :class="['chip', statusClass(row.status)]">{{ row.status }}</span>
              </td>

              <td class="center">{{ row.orderType }}</td>

              <td class="center" @click.stop>
                <PurchaseApprovalActions :po-id="row.id" :source-type="row.sourceType"
                  :smart-order-ids="row.smartOrderIds" @success="handleProcessSuccess" />
              </td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="7" class="no-data">발주 요청이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="page-nav" @click="goPage(1)" :disabled="page === 1">
          &laquo;
        </button>
        <button class="page-nav" @click="goPage(page - 1)" :disabled="page === 1">
          &lsaquo;
        </button>

        <div class="pages">
          <button v-for="p in visiblePages" :key="p" :class="{ active: p === page }" @click="goPage(p)">
            {{ p }}
          </button>
        </div>

        <button class="page-nav" @click="goPage(page + 1)" :disabled="page === totalPages">
          &rsaquo;
        </button>
        <button class="page-nav" @click="goPage(totalPages)" :disabled="page === totalPages">
          &raquo;
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { getPurchases, updatePurchaseStatus, updateSmartOrderStatus, mapPurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import { formatDateTimeMinute } from '@/components/global/Date';
import PurchaseApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'

const perPage = ref(10)
const page = ref(1)
const totalPagesFromBackend = ref(0)

// 총 페이지 수
const totalPages = computed(() => totalPagesFromBackend.value || 1)

// 표시할 페이지 번호 계산 (최대 5개)
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const delta = 2
  const pages = []

  if (total <= 5) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    let start = Math.max(1, current - delta)
    let end = Math.min(total, current + delta)

    if (start === 1) {
      end = Math.min(5, total)
    }
    if (end === total) {
      start = Math.max(1, total - 4)
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
  }

  return pages
})

onMounted(() => {
  // 초기 데이터 로드 등 필요한 작업 수행
  searchPurchases()
})

function handleProcessSuccess() {
  // 목록을 다시 불러와서 상태 변경 반영
  searchPurchases()
}

const searchPurchases = async () => {
  // 발주 목록을 다시 불러오는 함수
  const data = await getPurchases(
    page.value - 1,
    perPage.value,
    '',
    'SUBMITTED',
    null,
    null,
    null
  );

  totalPagesFromBackend.value = data.totalPages || 1

  rows.value = (data.content || []).map(item => ({
    id: item.purchaseId,
    No: item.poNo,
    vendor: item.supplierName,
    items: item.totalQty,
    amount: item.totalAmount,
    requestedAt: item.requestedAt,
    status: mapPurchaseStatus(item.status),
    orderType: mapPurchaseStatus(item.orderType),
    sourceType: item.sourceType,
    smartOrderIds: item.smartOrderIds // 스마트 발주인 경우 ID 배열
  }));
}

const router = useRouter()

const rows = ref([])

function goPage(p) {
  page.value = p
  searchPurchases()
}

function openDetail(row) {
  // navigate to approval detail page for this PO
  router.push({ name: 'hq-orders-approval-detail', params: { id: row.id } })
}

function statusClass(s) {
  if (!s) return ''
  if (s === '승인') return 's-accepted'
  if (s === '제출' || s === '대기') return 's-waiting'
  if (s === '반려') return 's-rejected'
  if (s === '취소') return 's-rejected'
  if (s === '초안') return 's-waiting'
  return ''
}
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

.approval-table th.center,
.approval-table td.center {
  text-align: center;
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

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding-top: 20px;
  margin-top: 16px;
}

.page-nav {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.page-nav:hover:not(:disabled) {
  background: #f3f4f6;
}

.page-nav:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pages {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.pages button {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 36px;
}

.pages button:hover:not(.active) {
  background: #f3f4f6;
}

.pages button.active {
  background: #111827;
  color: white;
}
</style>
