<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 승인</h1>
    </header>

    <section class="card">
      <h3 class="card-title">발주 요청 목록</h3>

      <!-- 필터 영역: 타입 필터만 사용하도록 PurchaseFilter 구성 (상태/날짜 비표시) -->
      <PurchaseFilter @search="onFilterSearch" :showTypeFilter="true" :showVendorFilter="false" :showStatusFilter="false" :showDateRange="false" />

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
              <td colspan="8" class="no-data">발주 요청이 없습니다.</td>
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
import PurchaseFilter from '@/components/domain/order/PurchaseFilter.vue'

const perPage = ref(10)
const page = ref(1)
const totalPagesFromBackend = ref(0)

// 현재 적용된 필터 (이 페이지는 날짜 범위 및 상태 검색 불필요 — 타입만 사용)
const activeFilters = ref({
  orderType: 'ALL',
  status: null,
  startDate: null,
  endDate: null,
  keyword: ''
})

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

// PurchaseFilter에서 전달되는 검색 필터를 처리합니다.
function onFilterSearch(filters) {
  // 이 페이지는 날짜 범위와 상태 검색을 사용하지 않음 — 타입 필터만 적용
  const orderType = filters.orderType || 'ALL'

  activeFilters.value = {
    orderType,
    status: null,
    startDate: null,
    endDate: null,
    keyword: filters.keyword || ''
  }

  page.value = 1
  searchPurchases()
}

const searchPurchases = async () => {
  // 발주 목록을 다시 불러오는 함수
  // 검색 조건 적용
  const q = activeFilters.value.keyword || ''
  const statusParam = activeFilters.value.status && activeFilters.value.status !== 'ALL' ? activeFilters.value.status : null
  const startDate = activeFilters.value.startDate || null
  const endDate = activeFilters.value.endDate || null

  const data = await getPurchases(
    page.value - 1,
    perPage.value,
    q,
    // 승인 페이지는 SUBMITTED 상태만 조회하도록 기본 고정
    'SUBMITTED',
    null,
    startDate,
    endDate
  );

  // Debug logs: 필터와 서버 응답 확인용
  console.log('[DEBUG] getPurchases called with filters:', {
    page: page.value,
    perPage: perPage.value,
    q,
    statusParam,
    startDate,
    endDate,
    activeFilters: activeFilters.value
  })
  console.log('[DEBUG] getPurchases response (raw):', data)

  // 서버에서 받은 통합 목록(일반+스마트)을 클라이언트에서 타입으로 필터링
  let content = data.content || []
  if (activeFilters.value.orderType && activeFilters.value.orderType !== 'ALL') {
    const wanted = (activeFilters.value.orderType || '').toString().toUpperCase()
    content = content.filter(it => ((it.orderType || '').toString().toUpperCase()) === wanted)
  }

  // 클라이언트 사이드 검색어 필터링: PO 번호, 공급사명, 요청자
  const keyword = (activeFilters.value.keyword || '').toString().trim()
  if (keyword) {
    const k = keyword.toLowerCase()
    content = content.filter(it => {
      const po = (it.poNo || '').toString().toLowerCase()
      const supplier = (it.supplierName || '').toString().toLowerCase()
      const requester = (it.requesterName || '').toString().toLowerCase()
      return po.includes(k) || supplier.includes(k) || requester.includes(k)
    })
  }

  // Debug: 필터 적용 후 내용과 orderType 분포 확인
  console.log('[DEBUG] content after orderType filter - length:', content.length)
  console.log('[DEBUG] content sample orderTypes:', content.slice(0, 10).map(it => ({ id: it.purchaseId, orderType: it.orderType })))

  totalPagesFromBackend.value = data.totalPages || 1

  rows.value = content.map(item => ({
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
  // 항상 발주 상세 페이지로 이동하되, SMART인 경우에는 query로 메타 정보를 전달합니다.
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
