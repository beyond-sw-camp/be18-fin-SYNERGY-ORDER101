<template>
  <div class="dashboard-page">
    <div class="dashboard">

      <!-- ===== Overview ===== -->
      <section class="overview-card">
        <div class="dashboard-header">
          <h1>대시보드</h1>
          <p class="subtitle">매장 업무를 한 화면에서 관리하세요</p>
        </div>

        <!-- ===== KPI ===== -->
        <div class="kpi-grid">
          <div class="kpi-card">
            <span class="kpi-label">승인 대기 발주</span>
            <strong class="kpi-value">
              {{ summary.pendingOrderCount ?? '–' }}
            </strong>
          </div>

          <div class="kpi-card warning">
            <span class="kpi-label">입고 예정 수량</span>
            <strong class="kpi-value">
              {{ summary.inTransitQtySum ?? '–' }}
            </strong>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">배송 중 발주</span>
            <strong class="kpi-value">
              {{ summary.inTransitShipmentCount ?? '–' }}
            </strong>
          </div>

          <div class="kpi-card success">
            <span class="kpi-label">이번 달 반려 발주</span>
            <strong class="kpi-value">
              {{ summary.canceledThisMonthCount ?? '–' }}
            </strong>
          </div>
        </div>
      </section>

      <!-- ===== Quick Actions ===== -->
      <section class="quick-actions">
        <h2>빠른 작업</h2>

        <div class="action-grid">
          <button
            v-for="link in quickLinks"
            :key="link.title"
            class="action-card"
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
import axios from 'axios'
import {
  ShoppingCart,
  Warehouse,
  ClipboardList
} from 'lucide-vue-next'

const router = useRouter()

/* ======================
   KPI Summary State
====================== */
const summary = ref({
  pendingOrderCount: null,
  inTransitQtySum: null,
  inTransitShipmentCount: null,
  canceledThisMonthCount: null,
})

/* ======================
   API Call
====================== */
const fetchDashboardSummary = async () => {
  try {
    const res = await axios.get('/api/v1/store/dashboard/summary')
    summary.value = res.data
  } catch (e) {
    console.error('Store Dashboard KPI 조회 실패', e)
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
    route: '/store/purchase/create',
  },
  {
    title: '창고 조회',
    description: '실시간 재고 수량 확인',
    icon: Warehouse,
    route: '/store/inventory/stock',
  },
  {
    title: '발주 조회',
    description: '발주 내역 조회 및 관리',
    icon: ClipboardList,
    route: '/store/purchase/list',
  },
]

function goTo(path) {
  router.push(path)
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
  background: #ecfdf5;
  color: #047857;
}

.kpi-card.success {
  background: #fff7ed;
  color: #c2410c;
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
