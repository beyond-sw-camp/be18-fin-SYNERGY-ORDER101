<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 관리</h1>
    </header>

    <div class="filter-card">
      <div class="filter-row">

        <div class="filter-item">
          <label>공급사</label>
          <input
            readonly
            :value="selectedSupplierName ? selectedSupplierName : '공급사 선택'"
            :style="{ color: selectedSupplierName ? '#111' : '#9ca3af' }"
            class="supplier-select"
            @click="openSupplierModal"
          />
        </div>

        <div class="filter-item">
          <label>날짜 선택</label>
          <flat-pickr
            v-model="dateRange"
            :config="dateConfig"
            class="date-input"
            placeholder="기간을 선택하세요"
          />
        </div>
        <div class="filter-actions">
          <button class="btn primary" @click="search">검색</button>
          <button class="btn" @click="resetFilters">초기화</button>
        </div>
      </div>
    </div>

    <SupplierSearchModal
      :isOpen="supplierModalOpen"
      :selectedSupplier="selectedSupplier"
      @update:isOpen="supplierModalOpen = $event"
      @select="handleSupplierSelect"
    />

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
                <span :class="['chip', statusClass(row.status)]">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
            </tr>
            <tr v-if="autoOrderStore.totalCount === 0">
              <td colspan="8" class="no-data">검색 조건에 맞는 발주가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="page-btn" @click="changePage(page - 1)" :disabled="page <= 1">
          ‹ Previous
        </button>

        <div class="page-numbers">
          <button v-for="p in visiblePages" :key="p" class="page"
            :class="['page-button', { active: page === p }]" @click="changePage(p)">
            {{ p }}
          </button>
        </div>

        <button class="page-btn" @click="changePage(page + 1)" :disabled="page >= totalPages">
          Next ›
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date';
import { useAutoOrderStore } from '@/stores/order/autoOrderStore';

import SupplierSearchModal from '@/components/modal/SupplierSearchModal.vue'

import flatPickr from 'vue-flatpickr-component'
import "flatpickr/dist/flatpickr.css"

const router = useRouter()
const autoOrderStore = useAutoOrderStore()

const page = computed(() => autoOrderStore.page)
const totalPages = computed(() => autoOrderStore.totalPages)
const isSearchMode = ref(false)

const supplierModalOpen = ref(false)
const selectedSupplier = ref(null)
const selectedSupplierName = ref('')

const dateRange = ref(null)

const searchParams = ref({
  supplierId: null,
  startDate: null,
  endDate: null
})

const dateConfig = {
  mode: 'range',
  dateFormat: 'Y-m-d',
  locale: {
    rangeSeparator: ' ~ ',
  },
}

onMounted(() => {
  autoOrderStore.fetchAutoOrders({ page: 1 })
})

function openSupplierModal() {
  supplierModalOpen.value = true
}

function handleSupplierSelect(supplier) {
  selectedSupplier.value = supplier
  selectedSupplierName.value = supplier.supplierName
}

async function search() {
  const [startDate, endDate] = dateRange.value?.split(' ~ ') ?? [null, null]
  isSearchMode.value = true

  searchParams.value = {
    supplierId: selectedSupplier.value?.supplierId ?? null,
    startDate,
    endDate
  }

  await autoOrderStore.searchAutoOrders({
    supplierId: selectedSupplier.value?.supplierId ?? null,
    startDate,
    endDate,
    page: 1,
    numOfRows: autoOrderStore.numOfRows
  })
}

async function resetFilters() {
  selectedSupplier.value = null
  selectedSupplierName.value = ''
  dateRange.value = null
  isSearchMode.value = false
  searchParams.value = { supplierId: null, startDate: null, endDate: null }

  await autoOrderStore.fetchAutoOrders({ page: 1 })
}

function openApproval(row) {
  router.push({ name: 'hq-orders-auto-detail', params: { purchaseId: row.purchaseId } })
}

function statusClass(s) {
  if (!s) return ''
  if (s === 'CONFIRMED') return 's-accepted'
  if (s === 'SUBMITTED') return 's-waiting'
  if (s === 'DRAFT_AUTO') return 's-draft'
  if (s === 'REJECTED') return 's-rejected'
  return ''
}

function statusLabel(status) {
  switch (status) {
    case 'SUBMITTED': return '제출'
    case 'CONFIRMED': return '승인'
    case 'REJECTED': return '반려'
    case 'CANCELLED': return '취소'
    case 'DRAFT_AUTO': return '초안'
    default: return status
  }
}

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const delta = 2 // 현재 페이지 양옆으로 보여줄 페이지 수
  const pages = []

  if (total <= 5) {
    // 전체 페이지가 5개 이하면 모두 표시
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 5개보다 많으면 현재 페이지 기준으로 표시
    let start = Math.max(1, current - delta)
    let end = Math.min(total, current + delta)

    // 시작이 1이면 끝을 늘림
    if (start === 1) {
      end = Math.min(5, total)
    }
    // 끝이 마지막이면 시작을 줄임
    if (end === total) {
      start = Math.max(1, total - 4)
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
  }

  return pages
})

const changePage = async (p) => {
  if (p < 1 || p > totalPages.value) return;

  if (isSearchMode.value) {
    // 검색 모드: 이전 검색 조건을 유지한 채 페이징
    await autoOrderStore.searchAutoOrders({
      supplierId: searchParams.value.supplierId,
      startDate: searchParams.value.startDate,
      endDate: searchParams.value.endDate,
      page: p,
      numOfRows: autoOrderStore.numOfRows
    });
  } else {
    // 전체 조회 모드
    await autoOrderStore.fetchAutoOrders({
      page: p,
      numOfRows: autoOrderStore.numOfRows
    });
  }
};

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

.btn.primary {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
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

.s-draft {
  background: #7c3aed;
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

.filter-card {
  background: #fff;
  border: 1px solid #e9eef6;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}
.filter-row {
  display: flex;
  gap: 16px;
  align-items: end;
}
.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.filter-item input {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.filter-actions {
  display: flex;
  gap: 8px;
}
.supplier-select {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  cursor: pointer;
}
.supplier-select .placeholder {
  color: #9ca3af;
}
.date-input {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  width: 100%;
}
.date-input:focus {
  border-color: #6366f1;
}
.page {
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  border-radius: 6px;
  background: #fff;
}
.page.active {
  border-color: #2563eb;
  color: #2563eb;
  font-weight: 600;
  background: #eff6ff;
}
.page.current {
  background: #f3f4f6;
}
.page-btn {
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
}
</style>
