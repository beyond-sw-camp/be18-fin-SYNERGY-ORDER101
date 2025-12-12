<template>
  <div class="order-dashboard">
    <h1 class="page-title">주문 대시보드</h1>

    <!-- 필터 영역 -->
    <section class="filter-section">
      <div class="filter-row">
        <span class="filter-label">필터:</span>
        <select v-model="filters.year" class="year-select">
          <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
        </select>
        <select v-model="filters.month" class="month-select">
          <option value="all">전체</option>
          <option v-for="month in 12" :key="month" :value="month">{{ month }}월</option>
        </select>
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
          
        </div>
        <div class="metric-card">
          <p class="metric-label">승인율</p>
          <p class="metric-value">{{ metrics.approvalRate }}%</p>
          
        </div>
        <div class="metric-card">
          <p class="metric-label">취소율</p>
          <p class="metric-value">{{ metrics.cancellationRate }}%</p>
          
        </div>
      </div>
    </section>

    <!-- 상태별 주문 현황 차트 -->
    <section class="chart-section">
      <h2 class="section-title">상태별 주문 현황</h2>
      <p class="chart-subtitle">선택한 기간의 주문 상태 비율</p>
      <div class="chart-container">
        <canvas ref="orderChartRef"></canvas>
      </div>
      <div class="chart-legend">
        <span class="legend-item"><span class="legend-color" style="background: #6366f1;"></span> 초안</span>
        <span class="legend-item"><span class="legend-color" style="background: #f59e0b;"></span> 제출</span>
        <span class="legend-item"><span class="legend-color" style="background: #10b981;"></span> 승인</span>
        <span class="legend-item"><span class="legend-color" style="background: #ef4444;"></span> 반려</span>
        <span class="legend-item"><span class="legend-color" style="background: #6b7280;"></span> 취소</span>
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
import VenderSearchModal from '@/components/modal/VenderSearchModal.vue'
import { getFranchiseOrderList, getFranchiseList } from '@/components/api/store/StoreService.js'
import Chart from 'chart.js/auto'

// --- 상태 정의 ---
const orderChartRef = ref(null)
let chartInstance = null
const showStoreModal = ref(false)
const selectedStore = ref(null)

const filters = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  storeId: null
})

const yearOptions = ref([])

// 년도 옵션 생성 (최근 3년)
function generateYearOptions() {
  const currentYear = new Date().getFullYear()
  return [currentYear, currentYear - 1, currentYear - 2]
}

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
  labels: [],
  datasets: [],
  counts: []
})

// 최근 조회 결과 재사용 (필요 시), 기본은 카운트만 계산해 메모리 사용을 줄임
const cachedOrders = ref([])

// --- 유틸 함수 ---
function formatNumber(num) {
  return num?.toLocaleString() || '0'
}

// 날짜 범위 계산 함수
function getDateRange() {
  const year = filters.year
  const month = filters.month

  if (month === 'all') {
    return {
      fromDate: `${year}-01-01`,
      toDate: `${year}-12-31`
    }
  } else {
    const startDate = new Date(year, month - 1, 1)
    const endDate = new Date(year, month, 0)
    return {
      fromDate: startDate.toISOString().split('T')[0],
      toDate: endDate.toISOString().split('T')[0]
    }
  }
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
  // 한 번만 호출해서 지표와 차트 모두 계산
  await fetchOrdersAndCompute()
}

async function fetchOrdersAndCompute() {
  try {
    const { fromDate, toDate } = getDateRange()
    const searchParams = {
      fromDate,
      toDate,
      storeId: filters.storeId,
      statuses: null
    }

    const pageSize = 1000
    let page = 1
    let totalPages = null

    // 카운트만 누적하여 메모리 사용 최소화
    const statusCount = {
      DRAFT_AUTO: 0,
      SUBMITTED: 0,
      CONFIRMED: 0,
      COMPLETED: 0,
      REJECTED: 0,
      CANCELLED: 0
    }
    let totalOrders = 0

    while (true) {
      const result = await getFranchiseOrderList(page, pageSize, searchParams)
      const content = result.content || []

      // 총 주문 수
      totalOrders += content.length

      // 상태 누적
      content.forEach(order => {
        const status = order.orderStatus || order.status
        if (statusCount.hasOwnProperty(status)) {
          statusCount[status]++
        }
      })

      // 총 페이지 수가 제공되면 활용, 아니면 응답 길이 기반 종료
      if (totalPages === null) {
        totalPages = result.totalPages || null
      }

      if (content.length < pageSize || (totalPages && page >= totalPages)) {
        break
      }
      page += 1
    }

    // 옵션: 최근 결과를 필요 시 위해 일부 저장 (첫 페이지만 캐시)
    cachedOrders.value = []

    computeMetricsFromCounts(totalOrders, statusCount)
    computeChartFromCounts(statusCount)

  } catch (error) {
    console.error('주문 데이터 조회 실패:', error)
  }
}

async function fetchStores() {
  try {
    const result = await getFranchiseList(1, 100, '')
    stores.value = result.franchises || []
  } catch (error) {
    console.error('매장 목록 조회 실패:', error)
  }
}

function computeMetricsFromCounts(totalOrders, statusCount) {
  metrics.totalOrders = totalOrders

  const confirmedCount = (statusCount.CONFIRMED || 0) + (statusCount.COMPLETED || 0)
  const cancelledCount = (statusCount.CANCELLED || 0) + (statusCount.REJECTED || 0)

  metrics.approvalRate = totalOrders > 0 ? ((confirmedCount / totalOrders) * 100).toFixed(1) : 0
  metrics.cancellationRate = totalOrders > 0 ? ((cancelledCount / totalOrders) * 100).toFixed(1) : 0

  // 전월 대비 변화율 (임시 데이터 - 실제로는 이전 기간 데이터 조회 필요)
  metrics.totalOrdersChange = 5.2
  metrics.approvalRateChange = 0.3
  metrics.cancellationRateChange = -0.1
}

async function fetchChartData() {
  // 이전 API를 유지하던 호출 경로를 위해 남겨둠 (호환용)
  await fetchOrdersAndCompute()
}

function computeChartFromCounts(statusCount) {
  const confirmed = (statusCount.CONFIRMED || 0) + (statusCount.COMPLETED || 0)

  const counts = [
    statusCount.DRAFT_AUTO || 0,
    statusCount.SUBMITTED || 0,
    confirmed,
    statusCount.REJECTED || 0,
    statusCount.CANCELLED || 0
  ]

  const total = counts.reduce((sum, n) => sum + n, 0)
  const percentages = total > 0 ? counts.map(n => Number(((n / total) * 100).toFixed(1))) : counts.map(() => 0)

  chartData.labels = ['초안', '제출', '승인', '반려', '취소']
  chartData.datasets = percentages
  chartData.counts = counts

  nextTick().then(() => renderChart())
}

function renderChart() {
  if (chartInstance) {
    chartInstance.destroy()
  }

  if (!orderChartRef.value) return

  const ctx = orderChartRef.value.getContext('2d')

  const backgroundColors = [
    '#6366f1', // 초안
    '#f59e0b', // 제출
    '#10b981', // 승인
    '#ef4444', // 반려
    '#6b7280'  // 취소
  ]

  const counts = chartData.counts || []

  chartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: chartData.labels,
      datasets: [{
        label: '주문 비율(%)',
        data: chartData.datasets,
        backgroundColor: backgroundColors,
        borderColor: backgroundColors,
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const idx = context.dataIndex
              const count = counts[idx] ?? 0
              return `${context.label}: ${context.parsed.y}% (${count}건)`
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          max: 100,
          ticks: {
            stepSize: 20,
            callback: (value) => `${value}%`
          },
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
  
  // 년도 옵션 초기화
  yearOptions.value = generateYearOptions()
  
  // 첫 번째 매장을 기본값으로 설정
  if (stores.value && stores.value.length > 0) {
    const firstStore = stores.value[0]
    selectedStore.value = {
      id: firstStore.storeId,
      name: firstStore.storeName
    }
    filters.storeId = firstStore.storeId
  } else {
    selectedStore.value = null
    filters.storeId = null
  }
  
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

.year-select,
.month-select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
  min-width: 100px;
}

.year-select:hover,
.month-select:hover {
  border-color: #6366f1;
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
  flex-wrap: wrap;
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
  display: inline-block;
}

/* 반응형 */
@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
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
