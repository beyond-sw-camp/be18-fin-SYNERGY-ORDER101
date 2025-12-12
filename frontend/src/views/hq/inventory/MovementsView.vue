<template>
  <div class="movements">
    <h2 class="page-title">입/출고 목록 조회</h2>

    <div class="tab-row">
      <button class="tab" :class="{ active: activeTab === 'INBOUND' }" @click="switchTab('INBOUND')">
        입고
      </button>

      <button class="tab" :class="{ active: activeTab === 'OUTBOUND' }" @click="switchTab('OUTBOUND')">
        출고
      </button>
    </div>

    <div class="filter-card">
      <div class="filter-row">

        <!-- 입고일 때: 공급사 선택 -->
        <div class="filter-item" v-if="activeTab === 'INBOUND'">
          <label>공급사</label>
          <input
            readonly
            :value="selectedSupplierName ? selectedSupplierName : '공급사 선택'"
            :style="{ color: selectedSupplierName ? '#111' : '#9ca3af' }"
            class="supplier-select"
            @click="openSupplierModal"
          />
        </div>

        <!-- 출고일 때: 가맹점 선택 -->
        <div class="filter-item" v-else>
          <label>가맹점</label>
          <div class="supplier-select" @click="openStoreModal">
            <span v-if="selectedStoreName">{{ selectedStoreName }}</span>
            <span v-else class="placeholder">가맹점 선택</span>
          </div>
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

    <!-- 공급사 선택 모달 -->
    <SupplierSearchModal
      :isOpen="supplierModalOpen"
      :selectedSupplier="selectedSupplier"
      @update:isOpen="supplierModalOpen = $event"
      @select="handleSupplierSelect"
    />

    <!-- 가맹점 선택 모달 -->
    <StoreSearchModal
      v-if="activeTab === 'OUTBOUND'"
      :isOpen="storeModalOpen"
      :selectedStore="selectedStore"
      @update:isOpen="storeModalOpen = $event"
      @select="handleStoreSelect"
    />

    <inbound-table 
      v-if="activeTab === 'INBOUND'"
      :items="inboundStore.items"
      :page="inboundStore.page"
      :totalPages="inboundStore.totalPages"
      @change-page="(p) => handlePage(p)"
      @open-modal="(data) => openDetailModal('INBOUND', data)"
    />
    
    <outbound-table 
      v-else
      :items="outboundStore.items"
      :page="outboundStore.page"
      :totalPages="outboundStore.totalPages"
      @change-page="(p) => outboundStore.fetchOutbound({ page: p })"
      @open-modal="(data) => openDetailModal('OUTBOUND', data)"
    />

    <InboundItemModal
      v-if="showModal && currentType === 'INBOUND'"
      :inbound-no="selectedNo"
      :details="inboundStore.details"
      @close="closeModal"
    />

    <OutboundItemModal
      v-if="showModal && currentType === 'OUTBOUND'"
      :outbound-no="selectedNo"
      :details="outboundStore.details"
      @close="closeModal"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useInboundStore } from '@/stores/inventory/inboundStore'
import { useOutboundStore } from '@/stores/inventory/outboundStore'
import flatPickr from 'vue-flatpickr-component'
import "flatpickr/dist/flatpickr.css"

import InboundTable from './tables/InboundTable.vue'
import OutboundTable from './tables/OutboundTable.vue'
import InboundItemModal from './modal/InboundItemModal.vue'
import OutboundItemModal from './modal/OutboundItemModal.vue'

import SupplierSearchModal from '@/components/modal/SupplierSearchModal.vue'
import StoreSearchModal from '@/components/modal/StoreSearchModal.vue'

const inboundStore = useInboundStore()
const outboundStore = useOutboundStore()

const activeTab = ref('INBOUND')

const showModal = ref(false)
const currentType = ref('')
const selectedNo = ref('')

const supplierModalOpen = ref(false)
const storeModalOpen = ref(false)

const selectedSupplier = ref(null)
const selectedSupplierName = ref('')

const selectedStore = ref(null)
const selectedStoreName = ref('')

const dateRange = ref(null)
const isSearchMode = ref(false)

// 검색 필터
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

function openSupplierModal() {
  supplierModalOpen.value = true
}

function openStoreModal() {
  storeModalOpen.value = true
}

function handleSupplierSelect(supplier) {
  selectedSupplier.value = supplier
  selectedSupplierName.value = supplier.supplierName
}

function handleStoreSelect(store) {
  selectedStore.value = store
  selectedStoreName.value = store.storeName
}

onMounted(() => {
  inboundStore.fetchInbound({ page: 1 })
})

function switchTab(tab) {
  activeTab.value = tab
  
  // 탭 전환 시 필터 초기화
  selectedSupplier.value = null
  selectedSupplierName.value = ''
  selectedStore.value = null
  selectedStoreName.value = ''
  dateRange.value = null
  searchParams.value = { supplierId: null, storeId: null, startDate: null, endDate: null }
  isSearchMode.value = false

  if (tab === 'INBOUND') {
    inboundStore.fetchInbound({ page: 1 })
  } else {
    outboundStore.fetchOutbound({ page: 1 })
  }
}

async function openDetailModal(type, { id, no }) {
  currentType.value = type
  selectedNo.value = no

  if (type === 'INBOUND') {
    await inboundStore.fetchInboundDetail(id)
  } else {
    await outboundStore.fetchOutboundDetail(id)
  }

  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function search() {
  const [startDate, endDate] = dateRange.value?.split(' ~ ') ?? [null, null]

  isSearchMode.value = true

  if (activeTab.value === 'INBOUND') {
    searchParams.value = {
      supplierId: selectedSupplier.value?.supplierId ?? null,
      startDate,
      endDate
    }

    await inboundStore.searchInbound({
      ...searchParams.value,
      page: 1
    })
  }

  if (activeTab.value === 'OUTBOUND') {
    searchParams.value = {
      storeId: selectedStore.value?.storeId ?? null,
      startDate,
      endDate
    }

    await outboundStore.searchOutbound({
      ...searchParams.value,
      page: 1
    })
  }
}

async function handlePage(p) {
  if (!isSearchMode.value) {
    activeTab.value === 'INBOUND'
      ? await inboundStore.fetchInbound({ page: p })
      : await outboundStore.fetchOutbound({ page: p })
    return
  }

  if (activeTab.value === 'INBOUND') {
    await inboundStore.searchInbound({ ...searchParams.value, page: p })
  } else {
    await outboundStore.searchOutbound({ ...searchParams.value, page: p })
  }
}


async function resetFilters() {
  selectedSupplier.value = null
  selectedSupplierName.value = ''

  selectedStore.value = null
  selectedStoreName.value = ''

  dateRange.value = null
  searchParams.value = { supplierId: null, storeId: null, startDate: null, endDate: null }
  isSearchMode.value = false

  activeTab.value === 'INBOUND'
    ? await inboundStore.fetchInbound({ page: 1 })
    : await outboundStore.fetchOutbound({ page: 1 })
}
</script>

<style scoped>
.page-title {
  font-size: 20px;
  margin: 8px 0 16px;
}
.tab-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tab {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
}
.tab.active {
  background: #eef2ff;
  border-color: #c7d2fe;
  font-weight: 600;
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
.btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
}
.btn.primary {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}
.table-card {
  background: #fff;
  border: 1px solid #e9eef6;
  padding: 0;
  border-radius: 8px;
  overflow: hidden;
}
.movements-table {
  width: 100%;
  border-collapse: collapse;
}
.movements-table thead th {
  background: #f3f4f6;
  text-align: left;
  padding: 16px;
}
.movements-table tbody td {
  padding: 18px 16px;
  border-top: 1px solid #f3f4f6;
  text-align: left;
}
.numeric {
  text-align: right;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 18px;
}
.page {
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  border-radius: 6px;
  background: #fff;
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

</style>
