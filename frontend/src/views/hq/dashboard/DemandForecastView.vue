<template>
  <div class="page-shell demand-forecast-page">
    <header class="page-header">
      <h1>수요 예측 보고서</h1>
    </header>

    <section class="chart-row">
      <div class="card chart-card">
        <div class="card-header">
          <h2>예측 vs 실제 수요</h2>
          <p class="subtitle">최근 1년간 월별 예측 수요와 실제 수요 비교</p>
        </div>
        <div class="card-body">
          <canvas ref="lineChartRef"></canvas>
        </div>
      </div>

      <div class="card chart-card">
        <div class="card-header">
          <h2>카테고리별 예측 정확도</h2>
          <p class="subtitle">중분류 기준</p>
        </div>
        <div class="card-body">
          <canvas ref="barChartRef"></canvas>
        </div>
      </div>
    </section>

    <section class="card table-card">
      <div class="card-header">
        <h2>상세 예측 데이터</h2>
      </div>
      <div class="card-body">
        <div v-if="loading" class="state-text">로딩 중...</div>
        <div v-else-if="error" class="state-text error">{{ error }}</div>

        <table v-else class="forecast-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>상품명</th>
              <th>최근 예측</th>
              <th>최근 수요</th>
              <th>오차율(MAPE)</th>
              <th>차트</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="row in pagedRows" :key="row.sku">
              <td>{{ row.sku }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.forecast }}</td>
              <td>{{ row.actual ?? '-' }}</td>
              <td>
                <span class="chip" :class="{ danger: row.metric >= 0.08 }">
                  {{ formatPercent(row.metric) }}
                </span>
              </td>
              <td class="center">
                <button class="detail-button" @click="openSkuChart(row)">
                  상세
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination">
        <button class="page-nav"
                @click="goSkuPage(1)"
                :disabled="skuPage === 1">
          &laquo;
        </button>

        <button class="page-nav"
                @click="goSkuPage(skuPage - 1)"
                :disabled="skuPage === 1">
          &lsaquo;
        </button>

        <div class="pages">
          <button
            v-for="p in visibleSkuPages"
            :key="p"
            :class="{ active: p === skuPage }"
            @click="goSkuPage(p)">
            {{ p }}
          </button>
        </div>

        <button class="page-nav"
                @click="goSkuPage(skuPage + 1)"
                :disabled="skuPage === skuTotalPages">
          &rsaquo;
        </button>

        <button class="page-nav"
                @click="goSkuPage(skuTotalPages)"
                :disabled="skuPage === skuTotalPages">
          &raquo;
        </button>
      </div>

    </section>

    <!-- SKU 상세 차트 모달 -->
    <SkuChartModal
      v-if="modal.show"
      :product="modal.product"
      :targetWeek="TARGET_WEEK"
      @close="modal.show = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import SkuChartModal from '@/components/ai/SkuChartModal.vue'

import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  BarController,
  BarElement,
  Tooltip,
  Legend,
} from 'chart.js'
import apiClient from '@/components/api'

Chart.register(
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  BarController,
  BarElement,
  Tooltip,
  Legend,
)

const TARGET_WEEK = '2025-12-15'

const loading = ref(false)
const error = ref('')

const lineChartRef = ref(null)
const barChartRef = ref(null)
let lineChart = null
let barChart = null

const timeseries = ref([])
const categoryMetrics = ref([])
const detailRows = ref([])

import { computed } from 'vue'

// SKU 페이지네이션 상태
const skuPage = ref(1)
const skuPerPage = 5

const skuTotalPages = computed(() =>
  Math.max(1, Math.ceil(detailRows.value.length / skuPerPage))
)

const visibleSkuPages = computed(() => {
  const total = skuTotalPages.value
  const current = skuPage.value
  const delta = 2 // 현재 페이지 기준 좌우 2개

  const pages = []

  if (total <= 5) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    let start = Math.max(1, current - delta)
    let end = Math.min(total, current + delta)

    if (start === 1) {
      end = 5
    }

    if (end === total) {
      start = total - 4
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
  }

  return pages
})

// 현재 페이지에 보여줄 5개
const pagedRows = computed(() => {
  const start = (skuPage.value - 1) * skuPerPage
  return detailRows.value.slice(start, start + skuPerPage)
})

function goSkuPage(p) {
  if (p < 1 || p > skuTotalPages.value) return
  skuPage.value = p
}


const modal = ref({
  show: false,
  product: null,
})

function openSkuChart(row) {
  modal.value = { show: true, product: row }
}

// 퍼센트 표시
const formatPercent = (v) => (v == null ? '-' : (v * 100).toFixed(1) + '%')

// 카테고리 라벨 줄바꿈 (bar chart용)
function formatCategoryLabel(label) {
  const map = {
    계절가전: '계절\n가전',
    디지털기기: '디지털\n기기',
    생활가전: '생활\n가전',
    영상가전: '영상\n가전',
    주방기기: '주방\n기기',
    청소가전: '청소\n가전',
  }
  return map[label] ?? label
}

async function fetchReport() {
  loading.value = true
  try {
    const res = await apiClient.get('/api/v1/ai/demand-forecast/report', {
      params: { targetWeek: TARGET_WEEK },
    })

    timeseries.value = res.data.timeseries
    categoryMetrics.value = res.data.categoryMetrics
    detailRows.value = res.data.details

    skuPage.value = 1
    renderCharts()
  } catch (err) {
    console.error(err)
    error.value = '수요 예측 데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  const labels = timeseries.value.map((x) => x.date)
  const forecast = timeseries.value.map((x) => x.forecast)
  const actual = timeseries.value.map((x) => x.actual)

  // --- line chart ---
  if (lineChart) lineChart.destroy()
  lineChart = new Chart(lineChartRef.value.getContext('2d'), {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '예측',
          data: forecast,
          borderWidth: 2,
          tension: 0.3,
          borderColor: '#6366F1',
          backgroundColor: 'rgba(99, 102, 241, 0.2)',
          pointRadius: 2,
        },
        {
          label: '실제',
          data: actual,
          borderWidth: 2,
          tension: 0.3,
          borderColor: '#EF4444',
          backgroundColor: 'rgba(239, 68, 68, 0.2)',
          pointRadius: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      tension: 0.4,
      plugins: { legend: { position: 'top' } },
      scales: {
        x: {
          ticks: {
            maxTicksLimit: 12,
            maxRotation: 0,
          },
        },
        y: { beginAtZero: false },
      },
    },
  })

  // --- bar chart ---
  if (barChart) barChart.destroy()
  barChart = new Chart(barChartRef.value.getContext('2d'), {
    type: 'bar',
    data: {
      labels: categoryMetrics.value.map((c) =>
        formatCategoryLabel(c.category)
      ),
      datasets: [
        {
          label: '오차율(%)',
          data: categoryMetrics.value.map((c) => (c.metric ?? 0) * 100),
          backgroundColor: '#34D399',
          borderColor: '#059669',
          borderWidth: 1,

          barThickness: 36,
          maxBarThickness: 40,
        },
      ],
    },

    options: {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        ticks: {
          autoSkip: false,
          maxRotation: 0,
          minRotation: 0,
          padding: 4,      
          align: 'center',
          font: {
            size: 11,      
          },
        },
      },
      y: {
        beginAtZero: true,
      },
    },
  },
  })
}

onMounted(fetchReport)
onBeforeUnmount(() => {
  if (lineChart) lineChart.destroy()
  if (barChart) barChart.destroy()
})
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.chart-row {
  display: flex;
  gap: 20px;
  margin-bottom: 32px;
}

.chart-card {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  max-height: 380px; /* 차트가 너무 커지지 않도록 제한 */
}

.chart-card canvas {
  width: 100% !important;
  height: 260px !important; /* 차트 높이 고정 */
}

.card-header h2 {
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  font-size: 13px;
  color: #999;
  margin-top: -2px;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-top: 28px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.forecast-table {
  width: 100%;
  border-collapse: collapse;
}

.forecast-table th,
.forecast-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  text-align: center;
}

.forecast-table td:first-child,
.forecast-table th:first-child {
  text-align: left;
}

.forecast-table td:nth-child(2),
.forecast-table th:nth-child(2) {
  text-align: left;
}

.center {
  text-align: center;
}

.chip {
  padding: 3px 10px;
  border-radius: 10px;
  background: #e6f3ff;
  color: #0077ff;
  font-size: 12px;
}

.chip.danger {
  background: #ffe5e5;
  color: #ff3a3a;
}

.icon-button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  background: #fff;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  cursor: pointer;
  color: #6b7280;
  transition: all 0.15s ease;
}

.icon-button:hover {
  background: #f3f4f6;
  color: #111827;
}


.icon-button:active {
  transform: scale(0.95);
}
.detail-button {
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;

  background-color: #d599f8; 
  border: none;

  cursor: pointer;
  transition: all 0.15s ease;
}

.detail-button:hover {
  background-color: #4f46e5; 
}

.detail-button:active {
  transform: scale(0.96);
}

/* pagination wrapper */
.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

/* page number group */
.pages {
  display: flex;
  gap: 8px;
}

/* page number button */
.pages button {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;

  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;

  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;

  transition: all 0.2s;
}

/* hover */
.pages button:hover {
  border-color: #6366f1;
  color: #6366f1;
}

/* active page */
.pages button.active {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border-color: transparent;
  font-weight: 600;
}

/* prev / next button */
.page-nav {
  min-width: 36px;
  height: 36px;
  padding: 6px 10px;

  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;

  cursor: pointer;
  font-size: 16px;
  color: #475569;

  transition: all 0.2s;
}

.page-nav:hover:not(:disabled) {
  background: #f3f4f6;
}

.page-nav:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}


</style>
