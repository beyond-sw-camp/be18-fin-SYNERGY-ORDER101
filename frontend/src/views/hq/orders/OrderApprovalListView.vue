<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 승인</h1>
    </header>

    <section class="card">
      <h3 class="card-title">발주 요청 목록</h3>

      <!-- 필터 영역: 타입 필터만 사용하도록 PurchaseFilter 구성 (상태/날짜 비표시) -->
      <PurchaseFilter @search="onFilterSearch" :showTypeFilter="true" :showVendorFilter="false" :showStatusFilter="false" :showDateRange="false" />

      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>데이터를 불러오는 중...</p>
      </div>
      <div v-else-if="rows.length > 0" class="table-wrap">
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

              <td class="center">
                <span :class="['type-badge', typeClass(row.orderType)]">{{ row.orderType }}</span>
              </td>

              <td class="center" @click.stop>
                <PurchaseApprovalActions :po-id="row.id" :source-type="row.sourceType"
                  :smart-order-ids="row.smartOrderIds" @success="handleProcessSuccess" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1">
          <rect x="3" y="3" width="18" height="18" rx="2" stroke-width="2" />
          <path d="M3 9h18M9 21V9" stroke-width="2" />
        </svg>
        <p class="empty-text">발주 요청이 없습니다</p>
        <p class="empty-hint">필터 조건을 변경해보세요</p>
      </div>

      <div class="pagination">
        <button class="page-nav" @click="goPage(1)" :disabled="page === 0">
          &laquo;
        </button>
        <button class="page-nav" @click="goPage(page)" :disabled="page === 0">
          &lsaquo;
        </button>

        <div class="pages">
          <button v-for="p in visiblePages" :key="p" :class="{ active: p === page + 1 }" @click="goPage(p)">
            {{ p }}
          </button>
        </div>

        <button class="page-nav" @click="goPage(page + 2)" :disabled="page + 1 >= totalPages">
          &rsaquo;
        </button>
        <button class="page-nav" @click="goPage(totalPages)" :disabled="page + 1 >= totalPages">
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
import { getPurchasesForApproval, mapPurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import { formatDateTimeMinute } from '@/components/global/Date';
import PurchaseApprovalActions from '@/views/hq/orders/PurchaseApproveButton.vue'
import PurchaseFilter from '@/components/domain/order/PurchaseFilter.vue'

const perPage = ref(10)
const page = ref(0) // 0-based for backend
const totalPagesFromBackend = ref(0)
const loading = ref(false)

// 현재 적용된 필터
const activeFilters = ref({
  keyword: ''
})

const totalPages = computed(() => totalPagesFromBackend.value || 1)

// 표시할 페이지 번호 계산 (1-based)
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value + 1
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
  searchPurchases()
})

function handleProcessSuccess() {
  searchPurchases()
}

// 필터 검색
function onFilterSearch(filters) {
  activeFilters.value = {
    keyword: filters.keyword || '',
    orderType: filters.orderType || 'ALL'
  }
  page.value = 0
  searchPurchases()
}

const searchPurchases = async () => {
  loading.value = true
  try {
    // orderType 필터: 'ALL'이면 null (필터 안 함), 아니면 해당 타입 (SMART/AUTO)
    const orderTypeFilter = activeFilters.value.orderType === 'ALL' ? null : activeFilters.value.orderType
    
    const data = await getPurchasesForApproval(
      page.value,
      perPage.value,
      activeFilters.value.keyword || null,
      orderTypeFilter
    )

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
      supplierId: item.supplierId,
      targetWeek: item.targetWeek,
      smartOrderIds: item.smartOrderIds
    }))
  } catch (error) {
    console.error('발주 조회 오류:', error)
    alert('발주 목록을 불러오는데 실패했습니다.')
    rows.value = []
  } finally {
    loading.value = false
  }
}

const router = useRouter()
const rows = ref([])

function goPage(p) {
  page.value = p - 1 // 1-based to 0-based
  searchPurchases()
}

function openDetail(row) {
  if (row.sourceType === 'SMART') {
    router.push({ name: 'hq-orders-approval-detail', params: { id: row.id }, query: { sourceType: 'SMART', supplierId: row.supplierId, targetWeek: row.targetWeek } })
  } else {
    router.push({ name: 'hq-orders-approval-detail', params: { id: row.id } })
  }
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

function typeClass(type) {
  if (!type) return ''
  if (type === '스마트') return 'type-smart'
  if (type === '자동') return 'type-auto'
  return 'type-manual'
}
</script>

<style scoped>
.s-accepted {
  background: #16a34a;
}

.s-waiting {
.type-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.type-smart {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(139, 92, 246, 0.3);
  animation: pulse-smart 2s ease-in-out infinite;
}

.type-auto {
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(6, 182, 212, 0.3);
  animation: pulse-auto 2s ease-in-out infinite;
}

.type-manual {
  background: #e2e8f0;
  color: #475569;
}

@keyframes pulse-smart {
  0%, 100% {
    box-shadow: 0 2px 4px rgba(139, 92, 246, 0.3);
  }
  50% {
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.6);
  }
}

@keyframes pulse-auto {
  0%, 100% {
    box-shadow: 0 2px 4px rgba(6, 182, 212, 0.3);
  }
  50% {
    box-shadow: 0 4px 12px rgba(6, 182, 212, 0.6);
  }
}
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

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f0f0f3;
  border-top: 4px solid #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-state svg {
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: #94a3b8;
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
