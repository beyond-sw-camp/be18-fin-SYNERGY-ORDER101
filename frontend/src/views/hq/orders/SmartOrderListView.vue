<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>스마트 발주 현황</h1>
    </header>

    <!-- 필터 영역 -->
    <section class="filter-row">
      <div class="filter-item">
        <label>시작일</label>
        <input type="date" v-model="startDate" class="input" />
      </div>

      <div class="filter-item">
        <label>종료일</label>
        <input type="date" v-model="endDate" class="input" />
      </div>

      <div class="filter-item">
        <label>상태</label>
        <select v-model="selectedStatus" class="input">
          <option value="">전체</option>
          <option value="DRAFT_AUTO">초안</option>
          <option value="SUBMITTED">제출</option>
          <option value="CONFIRMED">승인</option>
          <option value="REJECTED">반려</option>
          <option value="CANCELLED">취소</option>
        </select>
      </div>

      <div class="filter-actions">
        <button class="btn-primary" :disabled="loading" @click="fetchSmartOrders">
          {{ loading ? '조회 중...' : '조회' }}
        </button>
      </div>
    </section>

    <!-- 요약 카드 -->
    <section class="summary-row">
      <div class="summary-card">
        <p class="summary-label">AI 추천 총 발주량</p>
        <p class="summary-value">{{ totalRecommendedQty.toLocaleString() }}</p>
      </div>
      <div class="summary-card">
        <p class="summary-label">예측 총 발주량</p>
        <p class="summary-value">{{ totalForecastQty.toLocaleString() }}</p>
      </div>
      <div class="summary-card">
        <p class="summary-label">초안</p>
        <p class="summary-value">{{ draftCount }} 건</p>
      </div>
      <div class="summary-card">
        <p class="summary-label">총 추천 발주 금액</p>
        <p class="summary-value">{{ formatCurrency(totalRecommendedAmount) }}</p>
      </div>
    </section>

    <!-- 리스트 -->
    <section class="card">
      <h3 class="card-title">스마트 발주 목록</h3>

      <!-- 테이블 영역 -->
      <div class="table-wrap">
        <div v-if="loading" class="loading-box">
          데이터를 가져오는 중입니다...
        </div>

        <table v-else class="smart-table">
          <thead>
            <tr>
              <th>PO 번호</th>
              <th>공급사명</th>
              <th class="numeric">품목 수</th>
              <th class="numeric">총 예측량</th>
              <th class="numeric">총 추천 발주량</th>
              <th class="numeric">총 발주 금액</th>
              <th class="center">상태</th>
              <th class="center">생성 주차</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in pagedRows"
              :key="row.key"
              class="clickable-row"
              @click="openDetail(row)"
            >
              <td>{{ row.poNumber }}</td>
              <td>{{ row.supplierName }}</td>
              <td class="numeric">{{ row.itemCount }}</td>
              <td class="numeric">{{ row.totalForecastQty.toLocaleString() }}</td>
              <td class="numeric">{{ row.totalRecommendedQty.toLocaleString() }}</td>
              <td class="numeric">{{ formatCurrency(row.totalAmount) }}</td>
              <td class="center">
                <span :class="['chip', statusClass(row.status)]">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td class="center">{{ row.targetWeek }}</td>
            </tr>

            <tr v-if="!loading && pagedRows.length === 0">
              <td colspan="8" class="no-data">
                스마트 발주가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="page-nav" @click="goPage(1)" :disabled="page === 1">
          &laquo;
        </button>
        <button class="page-nav" @click="goPage(page - 1)" :disabled="page === 1">
          &lsaquo;
        </button>

        <div class="pages">
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="{ active: p === page }"
            @click="goPage(p)"
          >
            {{ p }}
          </button>
        </div>

        <button
          class="page-nav"
          @click="goPage(page + 1)"
          :disabled="page === totalPages"
        >
          &rsaquo;
        </button>
        <button
          class="page-nav"
          @click="goPage(totalPages)"
          :disabled="page === totalPages"
        >
          &raquo;
        </button>
      </div>
    </section>
  </div>
</template>


<script setup>
import apiClient from '@/components/api'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/* 필터 */
const startDate = ref('')
const endDate = ref('')
const selectedStatus = ref('')

/* 데이터 */
const rawRows = ref([])
const groupedRows = ref([])

/* 페이지네이션 */
const page = ref(1)
const perPage = ref(5)

/* 상태 */
const loading = ref(false)

/* 요약 */
const totalRecommendedAmount = computed(() =>
  groupedRows.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0),
)
const totalForecastQty = computed(() =>
  groupedRows.value.reduce((sum, r) => sum + r.totalForecastQty, 0),
)
const totalRecommendedQty = computed(() =>
  groupedRows.value.reduce((sum, r) => sum + r.totalRecommendedQty, 0),
)
const draftCount = computed(
  () => groupedRows.value.filter((r) => r.status === 'DRAFT_AUTO').length,
)

/* 페이지 계산 */
const totalPages = computed(() =>
  Math.max(1, Math.ceil(groupedRows.value.length / perPage.value)),
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * perPage.value
  return groupedRows.value.slice(start, start + perPage.value)
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const delta = 2
  const pages = []

  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    let start = Math.max(1, current - delta)
    let end = Math.min(total, current + delta)

    if (start === 1) end = 5
    if (end === total) start = total - 4

    for (let i = start; i <= end; i++) pages.push(i)
  }
  return pages
})

onMounted(() => {
  const today = new Date()
  const start = new Date(today)
  start.setMonth(start.getMonth() - 11)
  start.setDate(1)
  startDate.value = start.toISOString().slice(0, 10)

  const end = new Date(today)
  end.setMonth(end.getMonth() + 1)
  end.setDate(0)
  endDate.value = end.toISOString().slice(0, 10)

  fetchSmartOrders()
})

async function fetchSmartOrders() {
  try {
    const params = {}

    if (selectedStatus.value) params.status = selectedStatus.value
    if (startDate.value && endDate.value) {
      params.from = startDate.value
      params.to = endDate.value
    }

    loading.value = true
    const res = await apiClient.get('/api/v1/smart-orders', { params })
    rawRows.value = res.data || []

    groupBySupplierAndWeek()
    page.value = 1
  } catch (e) {
    console.error(e)
    alert('스마트 발주 조회 실패')
  } finally {
    loading.value = false
  }
}

function groupBySupplierAndWeek() {
  const map = new Map()

  for (const row of rawRows.value) {
    const key = `${row.supplierId}_${row.targetWeek}`
    if (!map.has(key)) {
      map.set(key, {
        key,
        supplierId: row.supplierId,
        supplierName: row.supplierName,
        targetWeek: row.targetWeek,
        poNumber: row.poNumber,
        itemCount: 0,
        totalForecastQty: 0,
        totalRecommendedQty: 0,
        totalAmount: 0,
        status: row.smartOrderStatus,
      })
    }

    const g = map.get(key)
    g.itemCount++
    g.totalForecastQty += row.forecastQty
    g.totalRecommendedQty += row.recommendedOrderQty
    g.totalAmount += Number(row.unitPrice) * Number(row.recommendedOrderQty)
  }

  groupedRows.value = [...map.values()]
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
}

function openDetail(row) {
  router.push({
    name: 'hq-smart-order-detail',
    params: { supplierId: row.supplierId, targetWeek: row.targetWeek },
  })
}

function formatCurrency(v) {
  return Number(v || 0).toLocaleString('ko-KR') + '원'
}

function statusLabel(s) {
  if (s === 'CONFIRMED') return '승인'
  if (s === 'SUBMITTED') return '제출'
  if (s === 'DRAFT_AUTO') return '초안'
  if (s === 'REJECTED') return '반려'
  return s || '-'
}

function statusClass(s) {
  if (s === 'CONFIRMED') return 's-confirmed'
  if (s === 'SUBMITTED') return 's-submitted'
  if (s === 'DRAFT_AUTO') return 's-draft'
  if (s === 'REJECTED') return 's-rejected'
  return ''
}
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.page-nav,
.pages button {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;

  color: #111827;
  font-size: 14px;
  font-weight: 500;
}
.page-nav:disabled {
  color: #9ca3af;
  cursor: not-allowed;
}
.pages button.active {
  background: #111827;
  color: white;
}

.filter-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 200px;
}

.input {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.filter-actions {
  margin-left: auto;
}

.btn-primary {
  background: #6b46ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 14px;
  cursor: pointer;
}
.s-rejected {
  background: #ef4444;
}


.summary-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f3;
  padding: 12px 16px;
}

.summary-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
}

.card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f3;
  padding: 16px;
}

.card-title {
  font-size: 16px;
  margin-bottom: 12px;
}

.table-wrap {
  margin-top: 8px;
  min-height: 80px;
}

/* 로딩 박스 */
.loading-box {
  padding: 20px;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}

.smart-table {
  width: 100%;
  border-collapse: collapse;
}

.smart-table th,
.smart-table td {
  padding: 14px 10px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 13px;
}

/* 기본은 왼쪽 정렬 */
.smart-table th,
.smart-table td {
  text-align: left;
}

/* 숫자 컬럼 */
.smart-table th.numeric,
.smart-table td.numeric {
  text-align: right;
}

/* 가운데 정렬 컬럼 (상태, 주차) */
.smart-table th.center,
.smart-table td.center {
  text-align: center;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: #f9fafb;
}

.no-data {
  text-align: center;
  padding: 20px;
  color: #9ca3af;
}

.chip {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
}

.s-draft {
  background: #6b46ff;
}

.s-submitted {
  background: #16a34a;
}
.s-confirmed {
  background: #3b82f6;
}
</style>
