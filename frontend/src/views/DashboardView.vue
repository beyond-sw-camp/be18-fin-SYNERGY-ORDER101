<template>
  <div class="dashboard">
    <h1 class="eyebrow">대시보드</h1>

    <section class="quick-actions">
      <h2>빠른 작업</h2>
      <div class="action-grid">
        <button
          v-for="link in quickLinks"
          :key="link.title"
          type="button"
          class="action-card"
          :class="{ highlight: link.highlight }"
          @click="goTo(link.route)"
        >
          <div class="icon-wrapper">{{ link.icon }}</div>
          <span class="action-title">{{ link.title }}</span>
          <span class="action-desc">{{ link.description }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const quickLinks = [
  { title: '새 발주 생성', description: '빠르게 생성', icon: '🛒', route: '/hq/orders/create', highlight: true },
  { title: '창고 조회', description: '실시간 재고 수량 확인', icon: '🏬', route: '/hq/inventory/stock' },
  { title: '주문 조회', description: '가맹점 주문 상태 추적', icon: '📦', route: '/hq/franchise/orders/:id' },
  { title: '수요 예측', description: 'AI 기반 수요 계획', icon: '📈', route: '/hq/dashboard/forecast' },
]

function goTo(path) {
  router.push(path)
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

header .eyebrow {
  font-size: 14px;
  color: #6366f1;
  font-weight: 600;
  text-transform: uppercase;
}

header h1 {
  font-size: 28px;
  font-weight: 700;
  margin-top: 4px;
}

.subtitle {
  color: #6b7280;
  margin-top: 4px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.card {
  border: 1px solid #edf0f4;
  border-radius: 16px;
  padding: 20px;
  background-color: #fafbff;
}

.card h2 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.stats {
  display: flex;
  gap: 12px;
}

.stats div {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #eef1f6;
}

.stats.compact {
  flex-direction: column;
}

.label {
  font-size: 13px;
  color: #6b7280;
}

.value {
  font-size: 20px;
  font-weight: 700;
  margin-top: 6px;
}

.muted {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.warning {
  color: #f97316;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.action-card {
  border-radius: 16px;
  border: 1px solid #eef1f6;
  padding: 20px;
  text-align: left;
  background-color: #fff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-card.highlight {
  background-color: #ecfdf5;
  border-color: #a7f3d0;
}

.action-title {
  font-weight: 600;
}

.action-desc {
  color: #6b7280;
  font-size: 13px;
}

.charts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.chart-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 0 8px rgba(0,0,0,0.08);
  height: 280px;
}

.small-chart {
  height: 280px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.small-chart canvas {
  max-width: 220px !important;
  max-height: 220px !important;
}
</style>
