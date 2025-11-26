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
        </select>
      </div>

      <div class="filter-actions">
        <button class="btn-primary" :disabled="loading" @click="fetchSmartOrders">
          {{ loading ? '조회 중...' : '조회' }}
        </button>
      </div>
    </section>

    <!-- 상단 요약 카드 -->
    <section class="summary-row">
      <div class="summary-card">
        <p class="summary-label">AI 추천 총 발주량</p>
        <p class="summary-value">{{ totalRecommendedQty.toLocaleString() }} 건</p>
      </div>
      <div class="summary-card">
        <p class="summary-label">예측 총 발주량</p>
        <p class="summary-value">{{ totalForecastQty.toLocaleString() }} 건</p>
      </div>
      <div class="summary-card">
        <p class="summary-label">초안</p>
        <p class="summary-value">{{ draftCount }} 건</p>
      </div>
      <div class="summary-card">
        <p class="summary-label">제출</p>
        <p class="summary-value">{{ submittedCount }} 건</p>
      </div>
    </section>

    <!-- 리스트 테이블 -->
    <section class="card">
      <h3 class="card-title">스마트 발주 목록</h3>

      <div class="table-wrap">
        <div v-if="loading" class="loading-box">
          데이터를 가져오는 중입니다...
        </div>

        <table v-else class="smart-table">
          <thead>
            <tr>
              <th>공급사 ID</th>
              <th>공급사명</th>
              <th class="numeric">품목 수</th>
              <th class="numeric">총 예측량</th>
              <th class="numeric">총 추천 발주량</th>
              <th class="center">상태</th>
              <th class="center">주차</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in groupedRows"
              :key="row.key"
              class="clickable-row"
              @click="openDetail(row)"
            >
              <td>{{ row.supplierId }}</td>
              <td>{{ row.supplierName }}</td>
              <td class="numeric">{{ row.itemCount }}</td>
              <td class="numeric">{{ row.totalForecastQty.toLocaleString() }}</td>
              <td class="numeric">{{ row.totalRecommendedQty.toLocaleString() }}</td>
              <td class="center">
                <span :class="['chip', statusClass(row.status)]">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td class="center">{{ row.targetWeek }}</td>
            </tr>
            <tr v-if="!loading && groupedRows.length === 0">
              <td colspan="7" class="no-data">스마트 발주가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const startDate = ref('')
const endDate = ref('')
const selectedStatus = ref('')

const rawRows = ref([])
const groupedRows = ref([])

const loading = ref(false)

const totalForecastQty = computed(() =>
  groupedRows.value.reduce((sum, r) => sum + r.totalForecastQty, 0)
)
const totalRecommendedQty = computed(() =>
  groupedRows.value.reduce((sum, r) => sum + r.totalRecommendedQty, 0)
)
const draftCount = computed(() =>
  groupedRows.value.filter(r => r.status === 'DRAFT_AUTO').length
)
const submittedCount = computed(() =>
  groupedRows.value.filter(r => r.status === 'SUBMITTED').length
)

onMounted(() => {
  const today = new Date()
  endDate.value = today.toISOString().slice(0, 10)

  const start = new Date(today)
  start.setDate(start.getDate() - 6)
  startDate.value = start.toISOString().slice(0, 10)

  fetchSmartOrders()
})

async function fetchSmartOrders () {
  try {
    const params = {}

    if (selectedStatus.value) {
      params.status = selectedStatus.value
    }

    if (startDate.value && endDate.value) {
      if (startDate.value > endDate.value) {
        alert('시작일이 종료일보다 늦을 수 없습니다.')
        return
      }
      params.from = startDate.value
      params.to = endDate.value
    }

    loading.value = true
    const res = await axios.get('/api/v1/smart-orders', { params })

    rawRows.value = res.data || []
    groupBySupplierAndWeek()
  } catch (e) {
    console.error('스마트 발주 조회 실패:', e)
    alert('스마트 발주 목록을 불러오는 중 오류가 발생했습니다.')
  } finally {
    loading.value = false
  }
}

function groupBySupplierAndWeek () {
  const map = new Map()

  for (const row of rawRows.value) {
    const supplierId = row.supplierId
    const supplierName = row.supplierName || '-'
    const targetWeek = row.targetWeek

    const key = `${supplierId}_${targetWeek}`

    if (!map.has(key)) {
      map.set(key, {
        key,
        supplierId,
        supplierName,
        targetWeek,
        itemCount: 0,
        totalForecastQty: 0,
        totalRecommendedQty: 0,
        status: row.smartOrderStatus,
      })
    }

    const g = map.get(key)
    g.itemCount += 1
    g.totalForecastQty += row.forecastQty || 0
    g.totalRecommendedQty += row.recommendedOrderQty || 0

    if (row.smartOrderStatus === 'SUBMITTED') {
      g.status = 'SUBMITTED'
    }
  }

  groupedRows.value = Array.from(map.values()).sort((a, b) => {
    if (a.targetWeek === b.targetWeek) {
      return (a.supplierId || 0) - (b.supplierId || 0)
    }
    return a.targetWeek < b.targetWeek ? -1 : 1
  })
}

function openDetail (row) {
  router.push({
    name: 'hq-smart-order-detail',
    params: {
      supplierId: row.supplierId,
      targetWeek: row.targetWeek,
    },
  })
}

function statusLabel (s) {
  if (s === 'SUBMITTED') return '제출'
  if (s === 'DRAFT_AUTO') return '초안'
  return s || '-'
}

function statusClass (s) {
  if (s === 'SUBMITTED') return 's-submitted'
  if (s === 'DRAFT_AUTO') return 's-draft'
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
</style>
