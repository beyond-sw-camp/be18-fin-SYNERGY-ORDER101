<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>주문 상세 내역</h1>
    </header>

    <section class="info-cards">
      <div class="card info">
        <label>주문 ID</label>
        <div class="value">{{ order.id }}</div>
      </div>
      <div class="card info">
        <label>상점</label>
        <div class="value">{{ order.store }}</div>
      </div>
      <div class="card info">
        <label>생성 시간</label>
        <div class="value">{{ order.createdAt }}</div>
      </div>
      <div class="card info">
        <div><label>상태</label></div>
        <div class="value status-chip">{{ statusLabel(order.status) }}</div>
      </div>
    </section>

    <section class="card items">
      <h3 class="card-title">주문 아이템</h3>
      <table class="items-table">
        <thead>
          <tr>
            <th>SKU</th>
            <th>상품 이름</th>
            <th>현재 재고</th>
            <th>주문 수량</th>
            <th class="numeric">단가</th>
            <th class="numeric">총액</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in order.items" :key="it.sku">
            <td>{{ it.sku }}</td>
            <td>{{ it.name }}</td>
            <td class="numeric">{{ it.stock }}</td>
            <td class="numeric">{{ it.qty }}</td>
            <td class="numeric"><Money :value="it.price" /></td>
            <td class="numeric"><Money :value="it.price * it.qty" /></td>
          </tr>
        </tbody>
      </table>

      <div class="items-summary">
        총 수량: {{ totalQty }} | 총 예상 가격: <strong>{{ formatMoney(totalPrice) }}</strong>
      </div>
    </section>

    <section class="card progress">
      <h3 class="card-title">주문 진행 상황</h3>
      <div class="timeline">
        <!-- icons row -->
        <div class="steps-icons" role="list">
          <div
            v-for="(s, idx) in orderProgress"
            :key="s.key + '-icon'"
            class="step-icon"
            :class="{
              active: s.done,
              current: !s.done && orderProgress[idx - 1] && orderProgress[idx - 1].done,
            }"
          >
            <div class="icon" aria-hidden>
              <i :class="s.done ? 'pi pi-check' : 'pi pi-circle'" />
            </div>
          </div>
        </div>

        <!-- track sits under icons -->
        <div class="track" aria-hidden>
          <div class="track-fill" :style="{ width: filledPercent + '%' }"></div>
        </div>

        <!-- labels row (aligned under icons) -->
        <div class="steps-labels" role="list">
          <div v-for="(s, idx) in orderProgress" :key="s.key + '-label'" class="step-label">
            <div class="label">
              {{ s.label }}
              <div class="sub">{{ s.time }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="statuses">
        <div v-for="s in orderProgress" :key="s.key" class="status-row">
          <div class="status-title">{{ s.label }}</div>
          <div class="status-time">{{ s.time }}</div>
          <div class="status-desc" v-if="s.note">{{ s.note }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import Money from '@/components/global/Money.vue'

const route = useRoute()
const id = route.params.id || 'ORD-UNKNOWN'

// sample order data; replace with API fetch when available
const order = reactive({
  id: id,
  store: '도매상점 본사점',
  createdAt: '2023-10-26 10:30',
  status: 'delivered',
  items: [
    { sku: 'SKU001', name: 'dd', stock: 1200, qty: 100, price: 5000 },
    { sku: 'SKU002', name: 'ss', stock: 800, qty: 70, price: 7500 },
    { sku: 'SKU003', name: 'dd', stock: 2500, qty: 150, price: 4000 },
    { sku: 'SKU004', name: 'ss', stock: 500, qty: 30, price: 1500 },
    { sku: 'SKU005', name: 'dd', stock: 900, qty: 0, price: 6000 },
  ],
})

const totalQty = computed(() => order.items.reduce((s, it) => s + (it.qty || 0), 0))
const totalPrice = computed(() =>
  order.items.reduce((s, it) => s + (it.price || 0) * (it.qty || 0), 0),
)

function formatMoney(v) {
  return v == null ? '-' : Number(v).toLocaleString() + '원'
}

function statusLabel(s) {
  switch (s) {
    case 'submitted':
      return 'SUBMITTED'
    case 'confirmed':
      return 'CONFIRMED'
    case 'waiting':
      return 'WAITING'
    case 'shipped':
      return 'SHIPPED'
    case 'delivered':
      return 'DELIVERED'
    default:
      return s
  }
}

const orderProgress = [
  { key: 'submitted', label: 'SUBMITTED', time: '2023-10-26 10:30', done: true },
  { key: 'confirmed', label: 'CONFIRMED', time: '2023-10-26 14:15', done: true },
  { key: 'waiting', label: 'WAITING', time: '2023-10-27 09:00', done: true },
  {
    key: 'shipped',
    label: 'SHIPPED',
    time: '2023-10-27 16:45',
    done: true,
    note: '배송번호: DPD123456789',
  },
  { key: 'delivered', label: 'DELIVERED', time: '', done: false },
]

const completedCount = computed(() => orderProgress.filter((s) => s.done).length)

const filledPercent = computed(() => {
  const steps = orderProgress.length
  if (!steps || completedCount.value === 0) return 0
  // When first step is completed, fill to its position (0%).
  // Fill to the last completed step's position across the track (0..100).
  const lastIndex = completedCount.value - 1
  const percent = (lastIndex / (steps - 1)) * 100
  return Math.max(0, Math.min(100, percent))
})
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}
.page-header {
  margin-bottom: 18px;
}
.info-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  width: 100%;
  box-sizing: border-box;
  /* allow cards to share available width evenly */
  align-items: stretch;
}
.card.info {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #eef2f7;
  background: #fff;
  /* make each info card take equal portion of the row */
  flex: 1 1 0;
  min-width: 0;
}
.card.info label {
  font-size: 1rem;
  color: #6b7280;
}
.card.info .value {
  font-size: 1.4rem;
  font-weight: 700;
  margin-top: 8px;
}
.status-chip {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 12px;
  background: #e5e7eb;
}
.items .card-title,
.progress .card-title {
  margin-bottom: 8px;
}
.items-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}
.items-table th,
.items-table td {
  padding: 12px;
  border-top: 1px solid #f3f4f6;
  text-align: left;
}
.items-table td.numeric {
  text-align: right;
}
.items-summary {
  text-align: right;
  margin-top: 8px;
  font-weight: 600;
}
.timeline {
  position: relative;
  padding: 28px 0 12px;
}

.timeline::before {
  /* horizontal track */
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  top: 32px; /* center of icon row */
  height: 6px;
  background: linear-gradient(90deg, #eef2f7 0%, #eef2f7 100%);
  border-radius: 6px;
  z-index: 1;
}
.steps-icons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 3;
}

.track {
  position: relative;
  margin-top: 8px;
  height: 6px;
  background: #eef2f7;
  border-radius: 6px;
  overflow: hidden;
}
.track-fill {
  height: 100%;
  background: linear-gradient(90deg, #6b63f6, #6b46ff);
  width: 0%;
  transition: width 360ms ease;
}

.step-icon {
  flex: 1 1 0;
  display: flex;
  justify-content: center;
  align-items: center;
}
.step-icon .icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 2px solid #e6e9ee;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
  font-size: 16px;
  color: #9ca3af;
}
.step-icon.active .icon {
  background: linear-gradient(180deg, #6b63f6, #6b46ff);
  color: #fff;
  border-color: transparent;
}
.step-icon.current .icon {
  background: #fff;
  border-color: #6b63f6;
  color: #6b63f6;
}

.steps-labels {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 10px;
}
.step-label {
  flex: 1 1 0;
  display: flex;
  justify-content: center;
}
.step-label .label {
  font-size: 12px;
  text-align: center;
}
.step-label .sub {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}
.statuses {
  margin-top: 12px;
}
.status-row {
  border-top: 1px solid #f3f4f6;
  padding: 12px 0;
}
.status-title {
  font-weight: 700;
}
.status-time {
  color: #6b7280;
  font-size: 12px;
  margin-top: 6px;
}
.status-desc {
  margin-top: 8px;
  color: #374151;
}
</style>
