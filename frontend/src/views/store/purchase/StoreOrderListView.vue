<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 목록</h1>
    </header>

    <section class="filters card">
      <PurchaseFilter @search="handleSearch" />
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="orders-table">
          <thead>
            <tr>
              <th>주문 번호</th>
              <th>매장명</th>
              <th class="center">품목 수</th>
              <th class="center">총 수량</th>
              <th class="center">금액</th>
              <th class="center">주문일</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="clickable-row" @click="openDetail(row)">
              <td class="po">{{ row.orderNo }}</td>
              <td>{{ row.storeName }}</td>
              <td class="center">{{ row.itemCount }}</td>
              <td class="center">{{ row.totalQty }}</td>
              <td class="center">
                <Money :value="row.totalAmount"></Money>
              </td>
              <td class="center">{{ formatDateTimeMinute(row.orderDate) }}</td>
              <td>
                <span :class="['chip', statusClass(row.orderStatus)]">{{ row.statusText }}</span>
              </td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="7" class="no-data">검색 조건에 맞는 발주가 없습니다.</td>
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
import { useAuthStore } from '@/stores/authStore'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute, getPastDateString } from '@/components/global/Date'
import PurchaseFilter from '@/components/domain/order/PurchaseFilter.vue'
import axios from 'axios'

// Auth Store (storeId 가져오기)
const authStore = useAuthStore()

// 주문 상태 매핑 (OrderStatus enum -> 한글)
const ORDER_STATUS_MAP = {
  SUBMITTED: { text: '제출', class: 's-waiting' },
  PENDING: { text: '대기', class: 's-waiting' },
  CONFIRMED: { text: '승인', class: 's-accepted' },
  REJECTED: { text: '반려', class: 's-rejected' },
  CANCELLED: { text: '취소', class: 's-rejected' },
  COMPLETED: { text: '완료', class: 's-accepted' },
  DRAFT: { text: '초안', class: 's-waiting' }
}

const filters = ref({
  statuses: null,
  searchText: null,
  startDate: getPastDateString(30),
  endDate: new Date().toISOString().slice(0, 10)
})

const page = ref(1)
const perPage = ref(10)
const rows = ref([])
const totalElements = ref(0)
const totalPagesFromBackend = ref(0)
const loading = ref(false)
const error = ref(null)

const router = useRouter()

const totalPages = computed(() => totalPagesFromBackend.value || 1)

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
  search()
})

function handleSearch(filterData) {
  console.log('🔍 발주 필터 검색:', filterData)
  filters.value = {
    statuses: filterData.status !== 'ALL' ? filterData.status : null,
    searchText: filterData.keyword || null,
    startDate: filterData.startDate,
    endDate: filterData.endDate
  }
  page.value = 1
  search()
}

async function search() {
  loading.value = true
  error.value = null

  const apiPage = page.value - 1

  try {
    // TradeSearchCondition 기반 파라미터 구성
    const params = {
      page: apiPage,
      size: perPage.value,
      sort: 'createdAt,desc'
    }

    // Store 계정의 storeId를 vendorId로 전달 (백엔드에서 storeId 필터로 사용)
    const storeId = authStore.userInfo?.storeId
    if (storeId) {
      params.vendorId = storeId
    }

    // 검색 조건 추가 (null이 아닌 경우만)
    if (filters.value.statuses) params.statuses = filters.value.statuses
    if (filters.value.searchText) params.searchText = filters.value.searchText
    if (filters.value.startDate) params.fromDate = filters.value.startDate
    if (filters.value.endDate) params.toDate = filters.value.endDate

    console.log('📤 API 요청 파라미터:', params)

    // GET /api/v1/store-orders (StoreOrderController.findStoreOrders)
    const response = await axios.get('/api/v1/store-orders', { params })
    const data = response.data

    console.log('📥 API 응답 데이터:', data)

    totalElements.value = data.totalElements || 0
    totalPagesFromBackend.value = data.totalPages || 1

    // StoreOrderSummaryResponseDto 매핑
    rows.value = (data.content || []).map(item => {
      const statusInfo = ORDER_STATUS_MAP[item.orderStatus] || { text: item.orderStatus, class: '' }
      return {
        id: item.storeOrderId,
        orderNo: item.orderNo || `SO-${item.storeOrderId}`,
        storeName: item.storeName || '-',
        itemCount: item.itemCount || 0,
        totalQty: item.totalQTY || 0,
        totalAmount: item.totalAmount || 0,
        orderDate: item.orderDate,
        orderStatus: item.orderStatus,
        statusText: statusInfo.text
      }
    })

  } catch (err) {
    console.error('발주 목록을 가져오는 중 오류 발생:', err)
    error.value = err.message || '발주 목록을 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  page.value = p
  search()
}

function openDetail(row) {
  // Store용 발주 상세 페이지로 이동
  router.push({ name: 'store-purchase-detail', params: { id: row.id } })
}

function statusClass(orderStatus) {
  const statusInfo = ORDER_STATUS_MAP[orderStatus]
  return statusInfo ? statusInfo.class : ''
}
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.table-wrap {
  margin-top: 12px;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
}

.orders-table th,
.orders-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
}

.orders-table th.center,
.orders-table td.center {
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

.s-accepted {
  background: #16a34a;
}

.s-waiting {
  background: #d97706;
}

.s-rejected {
  background: #ef4444;
}

.no-data {
  text-align: center;
  padding: 26px;
  color: #999;
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

.clickable-row {
  cursor: pointer;
}
</style>
