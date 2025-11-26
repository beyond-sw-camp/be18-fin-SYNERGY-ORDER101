<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>배송 조회</h1>
    </header>

    <section class="filters card">
      <div class="filters-row">
        <input 
          placeholder="주문 ID 검색" 
          v-model="filters.q"
        />

        <select v-model="filters.store">
          <option value="all">모든 가맹점</option>
          <option 
            v-for="s in storeOptions" 
            :value="s" 
            :key="s"
          >
            {{ s }}
          </option>
        </select>

        <select v-model="filters.status">
          <option value="all">모든 상태</option>
          <option 
            v-for="st in statusOptions" 
            :value="st.key" 
            :key="st.key"
          >
            {{ st.label }}
          </option>
        </select>

        <button class="btn" @click="applyFilter">필터 적용</button>
        <button class="btn" @click="resetFilter">필터 초기화</button>
      </div>
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="delivery-table">
          <thead>
            <tr>
              <th>주문 ID</th>
              <th>가맹점</th>
              <th>배송 창고</th>
              <th class="numeric">총 수량</th>
              <th>처리 상태</th>
              <th>요청 시간</th>
            </tr>
          </thead>

          <tbody>
            <tr 
              v-for="r in filteredRows" 
              :key="r.id"
            >
              <td class="po">{{ r.id }}</td>
              <td>{{ r.store }}</td>
              <td>{{ r.warehouse }}</td>
              <td class="numeric">{{ r.qty }}</td>

              <td>
                <span 
                  class="chip" 
                  :class="statusClass(r.status)"
                >
                  {{ statusLabel(r.status) }}
                </span>
              </td>

              <td>{{ formatDateTime(r.requestedAt) }}</td>
            </tr>

            <tr v-if="filteredRows.length === 0">
              <td colspan="6" class="no-data">조회 결과가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'



const DELIVERY_STATUS = {
  WAITING: 'WAITING',
  IN_TRANSIT: 'IN_TRANSIT',
  DELIVERED: 'DELIVERED',
}

// 셀렉트 박스용 상태 옵션
const statusOptions = [
  { key: DELIVERY_STATUS.WAITING, label: '배송 대기중' },
  { key: DELIVERY_STATUS.IN_TRANSIT, label: '배송 중' },
  { key: DELIVERY_STATUS.DELIVERED, label: '배송 완료' },
]

// 필터 상태
const filters = ref({
  q: '',
  store: 'all',
  status: 'all',
})

// API에서 받아올 배송 목록
const rows = ref([])

/* ================================
   API 호출: /api/v1/shipments
================================ */
async function fetchDeliveryList() {
  try {
    const res = await axios.get('/api/v1/shipments', {
      params: {
        page: 0,
        size: 20,
      },
    })

    const page = res.data

    rows.value = page.content.map(item => ({
      id: item.orderNo,                 
      store: item.storeName,           
      warehouse: item.warehouseName || '-', 
      qty: item.totalQty,         
      status: item.shipmentStatus,      
      requestedAt: item.orderDatetime,
    }))
  } catch (e) {
    console.error('배송 목록 조회 실패', e)
  }
}

onMounted(() => {
  fetchDeliveryList()
})

/* ================================
   가맹점 셀렉트 옵션 (데이터에서 추출)
================================ */
const storeOptions = computed(() => {
  const set = new Set(rows.value.map(r => r.store))
  return [...set]
})

/* ================================
   필터링된 결과
================================ */
const filteredRows = computed(() => {
  return rows.value.filter((r) => {
    const q = filters.value.q && filters.value.q.toLowerCase()

    // 주문 ID 검색 (orderNo 기준)
    if (q && !r.id.toLowerCase().includes(q)) return false

    // 가맹점 필터
    if (filters.value.store !== 'all' && r.store !== filters.value.store)
      return false

    // 상태 필터 (WAITING/IN_TRANSIT/DELIVERED)
    if (filters.value.status !== 'all' && r.status !== filters.value.status)
      return false

    return true
  })
})

/* ================================
   상태 뱃지 스타일 & 라벨
================================ */
function statusClass(s) {
  if (s === DELIVERY_STATUS.DELIVERED) return 's-delivered'
  if (s === DELIVERY_STATUS.IN_TRANSIT) return 's-intransit'
  return 's-pending' // WAITING 등
}

function statusLabel(s) {
  const opt = statusOptions.find(o => o.key === s)
  return opt ? opt.label : '알 수 없음'
}

/* ================================
   버튼 동작
================================ */
function applyFilter() {
}

function resetFilter() {
  filters.value = { q: '', store: 'all', status: 'all' }
}
function formatDateTime(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ').slice(0, 19)
}
</script>



<style scoped>
.page-shell {
  padding: 24px 32px;
}
.page-header {
  margin-bottom: 18px;
}
.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}
.filters-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filters-row input,
.filters-row select {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
}
.btn {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
}
.table-wrap {
  overflow-x: auto;
}
.delivery-table {
  width: 100%;
  border-collapse: collapse;
}
.delivery-table th,
.delivery-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
}
.delivery-table th.numeric,
.delivery-table td.numeric {
  text-align: right;
  width: 80px; 
}
.po {
  font-weight: 600;
}
.chip {
  padding: 6px 10px;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
}
.s-delivered {
  background: #0ea5a4;
}
.s-intransit {
  background: #2563eb;
}
.s-pending {
  background: #6b7280;
}
.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>
