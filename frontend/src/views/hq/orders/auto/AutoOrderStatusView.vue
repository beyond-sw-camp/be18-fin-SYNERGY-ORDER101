<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 관리</h1>
    </header>

    <section class="card controls">
      <div class="controls-row">
        <input class="search" v-model="filters.q" placeholder="PO 번호, 공급업체 등으로 검색..." />

        <div class="controls-right">
          <OrderStatusSelect v-model="filters.status" />
          <button class="btn" @click="search">조회</button>
        </div>
      </div>
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="orders-table">
          <thead>
            <tr>
              <th>PO 번호</th>
              <th>공급업체</th>
              <th>품목 수</th>
              <th class="numeric">총액</th>
              <th>생성일</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in autoOrderStore.items" :key="row.purchaseId" class="clickable-row" @click="openApproval(row)">
              <td class="po">{{ row.poNo }}</td>
              <td>{{ row.supplierName }}</td>
              <td class="numeric">{{ row.itemQty }}</td>
              <td class="numeric">
                <Money :value="row.totalAmount"></Money>
              </td>
              <td>{{ formatDateTimeMinute(row.requestedAt) }}</td>
              <td>
                <span :class="['chip', statusClass(row.status)]">{{ row.status }}</span>
              </td>
            </tr>
            <tr v-if="autoOrderStore.totalCount === 0">
              <td colspan="8" class="no-data">검색 조건에 맞는 발주가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <div class="pages">
          <button v-for="p in totalPages" :key="p" :class="{ active: p === page }" @click="goPage(p)">
            {{ p }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date';
import OrderStatusSelect from '@/components/OrderStatusSelect.vue';
import { useAutoOrderStore } from '@/stores/order/autoOrderStore';

const autoOrderStore = useAutoOrderStore()
const router = useRouter()

const filters = ref({ q: '', status: '전체' })

const page = ref(1)
const rows = ref([])

// 시작시
onMounted(() => {
  autoOrderStore.fetchAutoOrders({ page:1 })
});

function statusClass(s) {
  if (!s) return ''
  if (s.includes('CONFIRMED')) return 's-accepted'
  if (s.includes('SUBMITTED')) return 's-waiting'
  if (s.includes('REJECTED')) return 's-rejected'
  return ''
}

function openApproval(row) {
  router.push({ name: 'hq-orders-auto-detail', params: { purchaseId: row.purchaseId } })
}
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search {
  flex: 1;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e6e6e9;
}

.controls-right {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
}

.table-wrap {
  margin-top: 12px;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
}

.orders-table th,
.orders-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: center;
}

.orders-table td.numeric {
  text-align: center;
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

.s-accepted {
  background: #16a34a;
}

.s-waiting {
  background: #d97706;
}

.s-rejected {
  background: #ef4444;
}

.actions {
  text-align: center;
}

.no-data {
  text-align: center;
  padding: 26px;
  color: #999;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 12px;
}

.pages {
  display: flex;
  gap: 8px;
}

.pages button {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
}

.pages button.active {
  background: #111827;
  color: white;
}

.clickable-row {
  cursor: pointer;
}
</style>
