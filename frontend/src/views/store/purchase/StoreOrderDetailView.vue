<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>주문 상세 내역</h1>
      <button class="btn-back" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M19 12H5M12 19l-7-7 7-7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        목록으로
      </button>
    </header>

    <section class="info-cards">
      <div class="card info">
        <label>주문 번호</label>
        <div class="value">{{ detail.orderNo }}</div>
      </div>
      <div class="card info">
        <label>매장</label>
        <div class="value">{{ detail.storeName }}</div>
      </div>
      <div class="card info">
        <label>주문 일시</label>
        <div class="value">{{ detail.createdAt }}</div>
      </div>
      <div class="card info">
        <label>배송 상태</label>
        <div class="value">
          <span :class="['status-chip', statusClass(displayStatus)]">{{ statusLabel(displayStatus) }}</span>
        </div>
      </div>
    </section>

    <section class="card items">
      <h3 class="card-title">주문 아이템</h3>
      <table class="items-table">
        <thead>
          <tr>
            <th>SKU</th>
            <th>상품 이름</th>
            <th class="numeric">현재 재고</th>
            <th class="numeric">주문 수량</th>
            <th class="numeric">단가</th>
            <th class="numeric">총액</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in detail.items" :key="it.productCode">
            <td>{{ it.productCode }}</td>
            <td>{{ it.productName }}</td>
            <td class="numeric">{{ it.stock }}</td>
            <td class="numeric">{{ it.orderQty }}</td>
            <td class="numeric"><Money :value="it.unitPrice" /></td>
            <td class="numeric"><Money :value="it.unitPrice * it.orderQty" /></td>
          </tr>

          <tr v-if="detail.items.length === 0">
            <td colspan="6" class="no-data">주문 아이템이 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <div class="items-summary">
        총 수량: {{ totalQty }} | 총 금액:
        <strong>{{ formatMoney(totalAmount) }}</strong>
      </div>
    </section>

    <section class="card progress">
      <h3 class="card-title">주문 진행 상황</h3>

      <div class="timeline">
        <div class="steps-icons">
          <div
            v-for="(s, idx) in progressSteps"
            :key="s.key + '-icon'"
            class="step-icon"
            :class="{ active: idx <= currentStepIndex }"
          >
            <div class="icon">
              <i :class="idx <= currentStepIndex ? 'pi pi-check' : 'pi pi-circle'" />
            </div>
          </div>
        </div>
        <div class="track" ref="trackRef">
          <div class="track-fill" :style="{ width: filledPercent + '%' }"></div>
        </div>

        <div class="steps-labels">
          <div v-for="s in progressSteps" :key="s.key + '-label'" class="step-label">
            <div class="label">
              {{ s.label }}
              <div class="sub">{{ s.time }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="statuses">
        <div v-for="s in progressSteps" :key="s.key" class="status-row">
          <div class="status-title">{{ s.label }}</div>
          <div class="status-time">{{ s.time }}</div>

          <div v-if="s.key === 'SHIPPED'">
            <div class="status-desc">{{ trackingNumber }}</div>
          </div>

          <div v-else-if="s.note" class="status-desc">
            {{ s.note }}
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date.js'

const route = useRoute()
const router = useRouter()
const orderId = route.params.id

const detail = reactive({
  orderNo: '',
  storeName: '',
  createdAt: '',
  status: '',
  shipmentStatus: '',
  items: [],
  logs: []
})

const findTime = (st) => {
  const log = detail.logs.find((l) => l.status === st)
  return log ? formatDateTimeMinute(log.changedAt) : ''
}

const displayStatus = computed(() => {
  const ship = detail.shipmentStatus

  if (ship === 'DELIVERED') return 'DELIVERED'
  if (ship === 'IN_TRANSIT' || ship === 'SHIPPED') return 'SHIPPED'
  if (ship === 'WAITING') return 'WAITING'
  return '-'
})

const progressSteps = computed(() => [
  { key: 'SUBMITTED', label: '제출됨', time: findTime('SUBMITTED') },
  { key: 'WAITING', label: '배송대기', time: findTime('WAITING') },
  { key: 'SHIPPED', label: '배송중', time: findTime('SHIPPED') },
  { key: 'DELIVERED', label: '배송완료', time: findTime('DELIVERED') }
])

const currentStepIndex = computed(() => {
  if (displayStatus.value === 'DELIVERED') return 3
  if (displayStatus.value === 'SHIPPED') return 2
  if (displayStatus.value === 'WAITING') return 1
  return 0
})

const trackRef = ref(null)

const filledPercent = computed(() => {
  switch (displayStatus.value) {
    case 'WAITING':
      return 37.5
    case 'SHIPPED':
      return 62.5
    case 'DELIVERED':
      return 100
    default:
      return 15
  }
})

const trackingNumber = computed(() => {
  if (!(displayStatus.value === 'SHIPPED' || displayStatus.value === 'DELIVERED')) {
    return '아직 배송이 진행되기 전이라 송장번호가 없습니다'
  }

  const shipped = detail.logs.find((l) => l.status === 'SHIPPED')
  return shipped?.note ? `송장번호 : ${shipped.note}` : '-'
})

/**
 * 주문 상세 조회
 * GET /api/v1/store-orders/detail/{orderId}
 */
async function fetchDetail() {
  try {
    const res = await axios.get(`/api/v1/store-orders/detail/${orderId}`)
    const data = res.data

    detail.orderNo = data.orderNo
    detail.storeName = data.storeName

    detail.logs =
      data.progress?.map((p) => ({
        status: p.key.toUpperCase(),
        changedAt: p.time,
        note: p.note
      })) || []

    detail.createdAt = findTime('SUBMITTED')

    detail.status = data.orderStatus
    detail.shipmentStatus = data.shipmentStatus

    detail.items =
      data.items?.map((it) => ({
        productCode: it.sku,
        productName: it.name,
        stock: it.stock,
        orderQty: it.qty,
        unitPrice: it.price
      })) || []

    console.log('📦 주문 상세 조회 완료:', detail)
  } catch (error) {
    console.error('주문 상세 조회 실패:', error)
    alert('주문 상세 정보를 불러오는데 실패했습니다.')
  }
}

onMounted(fetchDetail)

const totalQty = computed(() =>
  detail.items.reduce((s, it) => s + (it.orderQty || 0), 0)
)

const totalAmount = computed(() =>
  detail.items.reduce((s, it) => s + (it.orderQty || 0) * (it.unitPrice || 0), 0)
)

const formatMoney = (v) => Number(v).toLocaleString() + '원'

const statusLabel = (s) =>
  ({
    WAITING: '배송대기',
    SHIPPED: '배송중',
    DELIVERED: '배송완료'
  }[s] || '-')

const statusClass = (s) =>
  ({
    WAITING: 's-waiting',
    SHIPPED: 's-shipping',
    DELIVERED: 's-delivered'
  }[s] || '')

function goBack() {
  router.push({ name: 'store-purchase-list' })
}
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #e5e7eb;
  border-color: #d1d5db;
}

.info-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.card {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #eef2f7;
  background: #fff;
  margin-bottom: 16px;
}

.card.info {
  flex: 1;
  margin-bottom: 0;
}

.card.info label {
  font-size: 13px;
  color: #6b7280;
}

.card.info .value {
  font-size: 1.2rem;
  font-weight: 700;
  margin-top: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.status-chip {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
}

.s-waiting {
  background: #d97706;
}

.s-shipping {
  background: #3b82f6;
}

.s-delivered {
  background: #16a34a;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.items-table th,
.items-table td {
  padding: 12px 8px;
  border-top: 1px solid #f3f4f6;
}

.items-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
}

.numeric {
  text-align: right;
}

.no-data {
  text-align: center;
  color: #9ca3af;
  padding: 24px;
}

.items-summary {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  text-align: right;
  font-size: 15px;
  color: #374151;
}

.items-summary strong {
  color: #4f46e5;
  font-size: 18px;
}

.timeline {
  display: grid;
  grid-template-rows: auto 6px auto;
  row-gap: 14px;
  padding: 28px 0;
}

.steps-icons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
}

.step-icon .icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 2px solid #ddd;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  color: #9ca3af;
}

.step-icon.active .icon {
  background: #6b46ff;
  border-color: #6b46ff;
  color: white;
}

.track {
  position: relative;
  height: 6px;
  background: #eee;
  border-radius: 6px;
}

.track-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: #6b46ff;
  border-radius: 6px;
  transition: width 0.3s ease;
}

.steps-labels {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
}

.steps-labels .label {
  font-weight: 500;
  color: #374151;
}

.steps-labels .sub {
  margin-top: 4px;
  font-size: 0.85rem;
  color: #666;
}

.statuses {
  margin-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.status-row {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.status-title {
  font-weight: 600;
  color: #374151;
}

.status-time {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.status-desc {
  font-size: 13px;
  color: #4f46e5;
  margin-top: 4px;
}
</style>
