<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>배송 조회</h1>
    </header>

    <!-- 필터 -->
    <section class="filters card">
      <div class="filters-row">
        <input placeholder="주문 ID 검색" v-model="filters.q" />

        <select v-model="filters.store">
          <option value="all">모든 가맹점</option>
          <option v-for="s in storeOptions" :key="s" :value="s">
            {{ s }}
          </option>
        </select>

        <select v-model="filters.status">
          <option value="all">모든 상태</option>
          <option v-for="st in statusOptions" :key="st.key" :value="st.key">
            {{ st.label }}
          </option>
        </select>

        <button class="btn" @click="applyFilter">필터 적용</button>
        <button class="btn" @click="resetFilter">필터 초기화</button>
      </div>
    </section>

    <!-- 목록 -->
    <section class="card list">
      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>데이터를 불러오는 중...</p>
      </div>

      <div v-else-if="rows.length > 0" class="table-wrap">
        <table class="delivery-table">
          <thead>
            <tr>
              <th>주문 ID</th>
              <th>가맹점</th>
              <th>배송 창고</th>
              <th class="numeric">총 수량</th>
              <th>처리 상태</th>
              <th>요청 시간</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="r in rows" :key="r.id">
              <td class="po">
                <router-link
                  :to="`/hq/franchise/delivery/${r.id}`"
                  class="order-link"
                >
                  {{ r.orderNo }}
                </router-link>
              </td>
              <td>{{ r.store }}</td>
              <td>{{ r.warehouse }}</td>
              <td class="numeric">{{ r.qty }}</td>
              <td class="status-cell">
                <span class="chip" :class="statusClass(r.status)">
                  {{ statusLabel(r.status) }}
                </span>
              </td>
              <td>{{ formatDateTime(r.requestedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-state">
        <p class="empty-text">배송 조회 결과가 없습니다</p>
        <p class="empty-hint">필터 조건을 변경해보세요</p>
      </div>

      <!-- 페이지네이션 -->
      <div class="pagination">
        <button class="pager" :disabled="page === 0" @click="goPrev">
          ‹ Previous
        </button>

        <button
          v-for="p in pageNumbers"
          :key="p"
          class="page"
          :class="{ active: p === page + 1 }"
          @click="changePage(p)"
        >
          {{ p }}
        </button>

        <button
          class="pager"
          :disabled="page + 1 >= totalPages"
          @click="goNext"
        >
          Next ›
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import apiClient from '@/components/api'

/* ===============================
 * 상수
 * =============================== */
const DELIVERY_STATUS = {
  WAITING: 'WAITING',
  IN_TRANSIT: 'IN_TRANSIT',
  DELIVERED: 'DELIVERED',
}

const statusOptions = [
  { key: DELIVERY_STATUS.WAITING, label: '배송 대기중' },
  { key: DELIVERY_STATUS.IN_TRANSIT, label: '배송 중' },
  { key: DELIVERY_STATUS.DELIVERED, label: '배송 완료' },
]

const MAX_VISIBLE_PAGES = 5

/* ===============================
 * 상태
 * =============================== */
const rows = ref([])
const loading = ref(false)

const page = ref(0)
const size = ref(20)
const totalPages = ref(1)

const filters = ref({
  q: '',
  store: 'all',
  status: 'all',
})

/* ===============================
 * 데이터 조회
 * =============================== */
async function fetchDeliveryList() {
  loading.value = true
  try {
    const res = await apiClient.get('/api/v1/shipments', {
      params: {
        page: page.value,
        size: size.value,
        orderNo: filters.value.q || null,
        storeName: filters.value.store === 'all' ? null : filters.value.store,
        status: filters.value.status === 'all' ? null : filters.value.status,
      },
    })

    const p = res.data

    rows.value = p.content.map(item => ({
      id: item.storeOrderId,
      orderNo: item.orderNo,
      store: item.storeName,
      warehouse: item.warehouseName || '-',
      qty: item.totalQty,
      status: item.shipmentStatus,
      requestedAt: item.orderDatetime,
    }))

    totalPages.value = p.totalPages
  } catch (e) {
    console.error('배송 목록 조회 실패', e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

/* ===============================
 * 파생 데이터
 * =============================== */
const storeOptions = computed(() => {
  const names = rows.value.map(r => r.store)
  return [...new Set(names)]
})

const pageNumbers = computed(() => {
  const pages = []
  const current = page.value + 1
  const total = totalPages.value
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)

  let start = Math.max(1, current - half)
  let end = Math.min(total, start + MAX_VISIBLE_PAGES - 1)

  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(1, end - MAX_VISIBLE_PAGES + 1)
  }

  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

/* ===============================
 * 페이지네이션
 * =============================== */
function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p - 1
  fetchDeliveryList()
}

function goPrev() {
  if (page.value > 0) changePage(page.value)
}

function goNext() {
  if (page.value + 1 < totalPages.value) changePage(page.value + 2)
}

/* ===============================
 * 필터
 * =============================== */
function applyFilter() {
  page.value = 0
  fetchDeliveryList()
}

function resetFilter() {
  filters.value = { q: '', store: 'all', status: 'all' }
  page.value = 0
  fetchDeliveryList()
}

watch(
  () => [filters.value.store, filters.value.status],
  applyFilter
)

/* 주문 ID 검색 디바운스 */
let searchDebounce = null
watch(
  () => filters.value.q,
  () => {
    clearTimeout(searchDebounce)
    searchDebounce = setTimeout(applyFilter, 400)
  }
)

/* ===============================
 * UI 헬퍼
 * =============================== */
function statusClass(s) {
  if (s === DELIVERY_STATUS.DELIVERED) return 's-delivered'
  if (s === DELIVERY_STATUS.IN_TRANSIT) return 's-intransit'
  return 's-pending'
}

function statusLabel(s) {
  const opt = statusOptions.find(o => o.key === s)
  return opt ? opt.label : s
}

function formatDateTime(dt) {
  return dt ? dt.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(fetchDeliveryList)
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
  margin-bottom: 20px;
}

/* 필터 영역 */
.filters-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.delivery-table td:nth-child(5),
.delivery-table th:nth-child(5) {
  text-align: center;
}

.filters-row input,
.filters-row select {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
}

.btn {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
}

.delivery-table td.status-cell {
  padding-top: 22px !important;
}

.table-wrap {
  overflow-x: auto;
}

.delivery-table {
  width: 100%;
  border-collapse: collapse;
}

.delivery-table th,
.delivery-table td {
  padding: 16px 14px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
  vertical-align: middle;
}

.delivery-table th.numeric,
.delivery-table td.numeric {
  text-align: right;
  width: 80px;
}

.po {
  font-weight: 600;
}

.chip {
  padding: 6px 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  height: 28px;
  color: #fff;
}

.s-delivered {
  background: #0ea5a4;
}
.s-intransit {
  background: #2563eb;
}
.s-pending {
  background: #6b7280;
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
  border-top: 4px solid #2563eb;
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
  margin-top: 18px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

.pager {
  background: transparent;
  border: none;
  color: #666;
  cursor: pointer;
}

.page {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #fff;
  cursor: pointer;
}

.page.active {
  background: #6b46ff;
  color: #fff;
  border-color: #6b46ff;
}

.order-link {
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
}

.order-link:hover {
  text-decoration: underline;
}

</style>
