<template>
  <div class="dashboard-page">
    <div class="dashboard">


      <section class="overview-card">
        <div class="dashboard-header">
          <h1>대시보드</h1>
          <p class="subtitle">주요 업무를 한 화면에서 관리하세요</p>
        </div>


        <div class="kpi-grid">
          <div class="kpi-card">
            <span class="kpi-label">승인 대기 발주</span>
            <strong class="kpi-value">
              {{ kpi.pendingPurchaseCount }}건
            </strong>
          </div>

          <div class="kpi-card warning">
            <span class="kpi-label">재고 위험 SKU</span>
            <strong class="kpi-value">
              {{ kpi.lowStockSkuCount }}개
            </strong>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">AI 생성 스마트 발주</span>
            <strong class="kpi-value">
              {{ kpi.draftAutoSmartOrderCount }}건
            </strong>
          </div>

          <div class="kpi-card success">
            <span class="kpi-label">최근 예측 정확도</span>
            <strong class="kpi-value">
              {{ kpi.recentForecastAccuracy.toFixed(1) }}%
            </strong>
          </div>
        </div>

      </section>


      <section class="quick-actions">
        <h2>빠른 작업</h2>

        <div class="action-grid">
          <button
            v-for="link in quickLinks"
            :key="link.title"
            class="action-card"
            :class="{ highlight: link.highlight }"
            @click="goTo(link.route)"
          >
            <div class="icon-wrapper">
              <component :is="link.icon" :size="26" />
            </div>

            <div class="text-wrap">
              <span class="action-title">{{ link.title }}</span>
              <span class="action-desc">{{ link.description }}</span>
            </div>
          </button>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ShoppingCart,
  Warehouse,
  Receipt,
  TrendingUp
} from 'lucide-vue-next'
import axios from 'axios'

const router = useRouter()

/* ======================
   KPI 상태
====================== */
const kpi = ref({
  pendingPurchaseCount: 0,
  lowStockSkuCount: 0,
  draftAutoSmartOrderCount: 0,
  recentForecastAccuracy: 0,
})

/* ======================
   API 호출
====================== */
const fetchDashboardSummary = async () => {
  try {
    const res = await axios.get('/api/v1/hq/dashboard/summary')
    kpi.value = res.data
  } catch (e) {
    console.error('대시보드 KPI 조회 실패', e)
  }
}

onMounted(fetchDashboardSummary)

/* ======================
   Quick Links
====================== */
const quickLinks = [
  {
    title: '새 발주 생성',
    description: '빠르게 발주를 생성합니다',
    icon: ShoppingCart,
    route: '/hq/orders/create',
  },
  {
    title: '창고 조회',
    description: '실시간 재고 수량 확인',
    icon: Warehouse,
    route: '/hq/inventory/stock',
  },
  {
    title: '정산 조회',
    description: '정산 내역 조회 및 관리',
    icon: Receipt,
    route: '/hq/settlement/list',
  },
  {
    title: '수요 예측',
    description: 'AI 기반 수요 계획',
    icon: TrendingUp,
    route: '/hq/dashboard/forecast',
  },
]

function goTo(path) {
  router.push(path)
}

/* ======================
   포맷 유틸
====================== */
const formatCurrency = (value) => {
  if (!value) return '₩0'
  return '₩' + Number(value).toLocaleString()
}
</script>


<style scoped>
/* ===== Page ===== */
.dashboard-page {
  background-color: #f5f7fb;
  min-height: calc(100vh - 64px);
  padding: 32px 40px;
}

.dashboard {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* ===== Overview Card ===== */
.overview-card {
  background: white;
  border-radius: 24px;
  padding: 28px;
  border: 1px solid #e5e7eb;

  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== Header ===== */
.dashboard-header h1 {
  font-size: 28px;
  font-weight: 800;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin-top: 6px;
}

/* ===== KPI ===== */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.kpi-card {
  padding: 20px;
  border-radius: 16px;
  background: #f9fafb;

  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-label {
  font-size: 13px;
  color: #6b7280;
}

.kpi-value {
  font-size: 22px;
  font-weight: 800;
}

.kpi-card.warning {
  background: #fff7ed;
  color: #c2410c;
}

.kpi-card.success {
  background: #ecfdf5;
  color: #047857;
}

/* ===== Quick Actions ===== */
.quick-actions {
  background: white;
  border-radius: 24px;
  padding: 28px;
  border: 1px solid #e5e7eb;

  display: flex;
  flex-direction: column;
  gap: 24px;
}

.quick-actions h2 {
  font-size: 18px;
  font-weight: 700;
}

/* ===== Action Grid ===== */
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 20px;

  padding: 28px;
  min-height: 120px;

  border-radius: 20px;
  border: 1px solid #e5e7eb;
  background: white;

  cursor: pointer;
  transition: all 0.2s ease;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.08);
}

.action-card.highlight {
  border-color: #e5e7eb;
  background: white;
}

/* ===== Icon ===== */
.icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;

  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
}



.highlight .icon-wrapper {
  background: #f3f4f6;
  color: #374151;
}

/* ===== Text ===== */
.text-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-title {
  font-size: 17px;
  font-weight: 700;
}

.action-desc {
  font-size: 14px;
  color: #6b7280;
}
</style>
