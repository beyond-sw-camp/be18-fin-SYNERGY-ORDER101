<template>
  <div class="order-dashboard">
    <h1 class="page-title">주문 대시보드</h1>

    <!-- 필터 영역 -->
    <section class="filter-section">
      <div class="filter-row">
        <span class="filter-label">필터:</span>
        <div class="date-range">
          <input type="date" v-model="filters.fromDate" class="date-input" />
          <span class="date-separator">~</span>
          <input type="date" v-model="filters.toDate" class="date-input" />
        </div>
        <button class="store-select-btn" @click="openStoreModal">
          {{ selectedStoreName }}
          <span class="dropdown-arrow">▼</span>
        </button>
        <button class="btn-apply" @click="fetchDashboardData">적용</button>
      </div>
    </section>

    <!-- 주요 성과 지표 -->
    <section class="metrics-section">
      <h2 class="section-title">주요 성과 지표</h2>
      <div class="metrics-grid">
        <div class="metric-card">
          <p class="metric-label">총 주문 수</p>
          <p class="metric-value">{{ formatNumber(metrics.totalOrders) }}건</p>
          <p class="metric-change" :class="metrics.totalOrdersChange >= 0 ? 'positive' : 'negative'">
            {{ metrics.totalOrdersChange >= 0 ? '↑' : '↓' }} {{ Math.abs(metrics.totalOrdersChange) }}% (전월 대비)
          </p>
        </div>
        <div class="metric-card">
          <p class="metric-label">승인율</p>
          <p class="metric-value">{{ metrics.approvalRate }}%</p>
          <p class="metric-change" :class="metrics.approvalRateChange >= 0 ? 'positive' : 'negative'">
            {{ metrics.approvalRateChange >= 0 ? '▲' : '▼' }} {{ Math.abs(metrics.approvalRateChange) }}% (전월 대비)
          </p>
        </div>
        <div class="metric-card">
          <p class="metric-label">취소율</p>
          <p class="metric-value">{{ metrics.cancellationRate }}%</p>
          <p class="metric-change" :class="metrics.cancellationRateChange <= 0 ? 'positive' : 'negative'">
            {{ metrics.cancellationRateChange <= 0 ? '▼' : '▲' }} {{ Math.abs(metrics.cancellationRateChange) }}% (전월 대비)
          </p>
        </div>
      </div>
    </section>

    <!-- 기간별 주문량 차트 -->
    <section class="chart-section">
      <h2 class="section-title">기간별 주문량</h2>
      <p class="chart-subtitle">지난 6개월간의 확정 및 보류 주문량 추이</p>
      <div class="chart-container">
        <canvas ref="orderChartRef"></canvas>
      </div>
      <div class="chart-legend">
        <span class="legend-item">
          <span class="legend-color confirmed"></span> 확정 주문
        </span>
        <span class="legend-item">
          <span class="legend-color pending"></span> 보류 주문
        </span>
      </div>
    </section>

    <!-- 매장 성과 분석 -->
    <section class="store-analysis-section">
      <h2 class="section-title">매장 성과 분석</h2>
      <div class="store-tables">
        <!-- 상위 5개 매장 -->
        <div class="store-table-wrapper">
          <h3 class="table-title">상위 5개 매장</h3>
          <table class="store-table">
            <thead>
              <tr>
                <th>점</th>
                <th>주문량</th>
                <th>매출</th>
                <th>승인율</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="store in topStores" :key="store.storeId">
                <td>{{ store.name }}</td>
                <td class="numeric">{{ store.orderCount }}</td>
                <td class="numeric"><Money :value="store.revenue" /></td>
                <td class="numeric">{{ store.approvalRate }}%</td>
              </tr>
              <tr v-if="topStores.length === 0">
                <td colspan="4" class="empty">데이터가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 하위 5개 매장 -->
        <div class="store-table-wrapper">
          <h3 class="table-title">하위 5개 매장</h3>
          <table class="store-table">
            <thead>
              <tr>
                <th>매장</th>
                <th>주문량</th>
                <th>매출</th>
                <th>승인율</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="store in bottomStores" :key="store.storeId">
                <td>{{ store.name }}</td>
                <td class="numeric">{{ store.orderCount }}</td>
                <td class="numeric"><Money :value="store.revenue" /></td>
                <td class="numeric">{{ store.approvalRate }}%</td>
              </tr>
              <tr v-if="bottomStores.length === 0">
                <td colspan="4" class="empty">데이터가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- 가맹점 검색 모달 -->
    <VenderSearchModal
      v-if="showStoreModal"
      :currentType="'FRANCHISE'"
      @close="showStoreModal = false"
      @select="onStoreSelect"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import Money from '@/components/global/Money.vue'
import VenderSearchModal from '@/components/modal/VenderSearchModal.vue'
import { getFranchiseOrderList, getFranchiseList } from '@/components/api/store/StoreService.js'
import Chart from 'chart.js/auto'

// --- 상태 정의 ---
const orderChartRef = ref(null)
let chartInstance = null
const showStoreModal = ref(false)
const selectedStore = ref(null)

const filters = reactive({
  fromDate: getDefaultFromDate(),
  toDate: getDefaultToDate(),
  storeId: null
})

// 선택된 매장 이름 표시
const selectedStoreName = computed(() => {
  if (selectedStore.value) {
    return selectedStore.value.name
  }
  return '모든 매장'
})

const stores = ref([])

const metrics = reactive({
  totalOrders: 0,
  totalOrdersChange: 0,
  approvalRate: 0,
  approvalRateChange: 0,
  cancellationRate: 0,
  cancellationRateChange: 0
})

const topStores = ref([])
const bottomStores = ref([])

const chartData = reactive({
  labels: ['1월', '2월', '3월', '4월', '5월', '6월'],
  confirmed: [0, 0, 0, 0, 0, 0],
  pending: [0, 0, 0, 0, 0, 0]
})

// --- 유틸 함수 ---
function getDefaultFromDate() {
  const date = new Date()
  date.setMonth(date.getMonth() - 6)
  return date.toISOString().split('T')[0]
}

function getDefaultToDate() {
  return new Date().toISOString().split('T')[0]
}

function formatNumber(num) {
  return num?.toLocaleString() || '0'
}

// --- 가맹점 모달 함수 ---
function openStoreModal() {
  showStoreModal.value = true
}

function onStoreSelect(store) {
  if (store) {
    selectedStore.value = store
    filters.storeId = store.id
  } else {
    selectedStore.value = null
    filters.storeId = null
  }
  showStoreModal.value = false
}

// --- 데이터 조회 ---
async function fetchDashboardData() {
  await Promise.all([
    fetchOrderMetrics(),
    fetchStorePerformance(),
    fetchChartData()
  ])
}

async function fetchStores() {
  try {
    const result = await getFranchiseList(1, 100, '')
    stores.value = result.franchises || []
  } catch (error) {
    console.error('매장 목록 조회 실패:', error)
  }
}

async function fetchOrderMetrics() {
  try {
    const searchParams = {
      fromDate: filters.fromDate,
      toDate: filters.toDate,
      storeId: filters.storeId,
      statuses: null
    }

    const result = await getFranchiseOrderList(1, 1000, searchParams)
    const orders = result.content || []

    // 총 주문 수
    metrics.totalOrders = orders.length

    // 상태별 집계
    const confirmedCount = orders.filter(o => o.orderStatus === 'CONFIRMED' || o.orderStatus === 'COMPLETED').length
    const cancelledCount = orders.filter(o => o.orderStatus === 'CANCELLED' || o.orderStatus === 'REJECTED').length
    const totalProcessed = confirmedCount + cancelledCount

    // 승인율, 취소율 계산
    metrics.approvalRate = totalProcessed > 0 ? ((confirmedCount / totalProcessed) * 100).toFixed(1) : 0
    metrics.cancellationRate = totalProcessed > 0 ? ((cancelledCount / totalProcessed) * 100).toFixed(1) : 0

    // 전월 대비 변화율 (임시 데이터 - 실제로는 이전 기간 데이터 조회 필요)
    metrics.totalOrdersChange = 5.2
    metrics.approvalRateChange = 0.3
    metrics.cancellationRateChange = -0.1

  } catch (error) {
    console.error('주문 지표 조회 실패:', error)
  }
}

async function fetchStorePerformance() {
  try {
    const searchParams = {
      fromDate: filters.fromDate,
      toDate: filters.toDate,
      storeId: null,
      statuses: null
    }

    const result = await getFranchiseOrderList(1, 1000, searchParams)
    const orders = result.content || []

    // 매장별 집계
    const storeStats = {}
    orders.forEach(order => {
      const storeId = order.storeId
      const storeName = order.storeName || `매장 ${storeId}`

      if (!storeStats[storeId]) {
        storeStats[storeId] = {
          storeId,
          name: storeName,
          orderCount: 0,
          revenue: 0,
          confirmedCount: 0,
          totalProcessed: 0
        }
      }

      storeStats[storeId].orderCount++
      storeStats[storeId].revenue += order.totalAmount || 0

      if (order.orderStatus === 'CONFIRMED' || order.orderStatus === 'COMPLETED') {
        storeStats[storeId].confirmedCount++
        storeStats[storeId].totalProcessed++
      } else if (order.orderStatus === 'CANCELLED' || order.orderStatus === 'REJECTED') {
        storeStats[storeId].totalProcessed++
      }
    })

    // 승인율 계산 및 정렬
    const storeList = Object.values(storeStats).map(store => ({
      ...store,
      approvalRate: store.totalProcessed > 0
        ? ((store.confirmedCount / store.totalProcessed) * 100).toFixed(1)
        : 0
    }))

    // 주문량 기준 정렬
    storeList.sort((a, b) => b.orderCount - a.orderCount)

    topStores.value = storeList.slice(0, 5)
    bottomStores.value = storeList.slice(-5).reverse()

    // 데이터가 5개 미만인 경우 처리
    if (storeList.length < 5) {
      bottomStores.value = []
    }

  } catch (error) {
    console.error('매장 성과 조회 실패:', error)
  }
}

async function fetchChartData() {
  try {
    const searchParams = {
      fromDate: filters.fromDate,
      toDate: filters.toDate,
      storeId: filters.storeId,
      statuses: null
    }

    const result = await getFranchiseOrderList(1, 1000, searchParams)
    const orders = result.content || []

    // 월별 집계
    const monthlyData = {}
    const months = []
    const now = new Date()

    // 최근 6개월 레이블 생성
    for (let i = 5; i >= 0; i--) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
      const label = `${date.getMonth() + 1}월`
      months.push({ key, label })
      monthlyData[key] = { confirmed: 0, pending: 0 }
    }

    // 주문 데이터 월별 분류
    orders.forEach(order => {
      const orderDate = new Date(order.createdAt || order.orderDate)
      const key = `${orderDate.getFullYear()}-${String(orderDate.getMonth() + 1).padStart(2, '0')}`

      if (monthlyData[key]) {
        if (order.orderStatus === 'CONFIRMED' || order.orderStatus === 'COMPLETED') {
          monthlyData[key].confirmed++
        } else if (order.orderStatus === 'PENDING' || order.orderStatus === 'SUBMITTED') {
          monthlyData[key].pending++
        }
      }
    })

    // 차트 데이터 업데이트
    chartData.labels = months.map(m => m.label)
    chartData.confirmed = months.map(m => monthlyData[m.key].confirmed)
    chartData.pending = months.map(m => monthlyData[m.key].pending)

    await nextTick()
    renderChart()

  } catch (error) {
    console.error('차트 데이터 조회 실패:', error)
  }
}

function renderChart() {
  if (chartInstance) {
    chartInstance.destroy()
  }

  if (!orderChartRef.value) return

  const ctx = orderChartRef.value.getContext('2d')

  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: chartData.labels,
      datasets: [
        {
          label: '확정 주문',
          data: chartData.confirmed,
          borderColor: '#6366f1',
          backgroundColor: 'rgba(99, 102, 241, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointBackgroundColor: '#6366f1'
        },
        {
          label: '보류 주문',
          data: chartData.pending,
          borderColor: '#f97316',
          backgroundColor: 'rgba(249, 115, 22, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointBackgroundColor: '#f97316'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          grid: {
            color: '#f3f4f6'
          }
        },
        x: {
          grid: {
            display: false
          }
        }
      }
    }
  })
}

// --- 라이프사이클 ---
onMounted(async () => {
  await fetchStores()
  await fetchDashboardData()
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.destroy()
  }
})
</script>

<style scoped>
.order-dashboard {
  padding: 24px;
  max-width: 1400px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #1f2937;
}

/* 필터 영역 */
.filter-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-label {
  font-weight: 600;
  color: #374151;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
}

.date-separator {
  color: #9ca3af;
}

.store-select-btn {
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  min-width: 150px;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  transition: all 0.2s;
}

.store-select-btn:hover {
  border-color: #6366f1;
  background: #f8f9ff;
}

.dropdown-arrow {
  font-size: 10px;
  color: #9ca3af;
}

.btn-apply {
  padding: 8px 20px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-apply:hover {
  background: #4f46e5;
}

/* 섹션 공통 */
.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #1f2937;
}

/* 주요 성과 지표 */
.metrics-section {
  margin-bottom: 32px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.metric-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px 24px;
}

.metric-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
}

.metric-change {
  font-size: 13px;
}

.metric-change.positive {
  color: #10b981;
}

.metric-change.negative {
  color: #ef4444;
}

/* 차트 영역 */
.chart-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 32px;
}

.chart-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
  margin-bottom: 16px;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-color.confirmed {
  background: #6366f1;
}

.legend-color.pending {
  background: #f97316;
}

/* 매장 성과 분석 */
.store-analysis-section {
  margin-bottom: 32px;
}

.store-tables {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.store-table-wrapper {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #374151;
}

.store-table {
  width: 100%;
  border-collapse: collapse;
}

.store-table th,
.store-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
}

.store-table th {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  background: #f9fafb;
}

.store-table td {
  font-size: 14px;
  color: #374151;
}

.store-table .numeric {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.store-table .empty {
  text-align: center;
  color: #9ca3af;
  padding: 24px;
}

/* 반응형 */
@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .store-tables {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
