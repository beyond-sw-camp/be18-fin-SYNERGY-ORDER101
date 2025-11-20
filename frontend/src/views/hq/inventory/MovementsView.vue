<template>
  <div class="movements">
    <h2 class="page-title">입/출고 목록 조회</h2>

    <div class="tab-row">
      <button
        class="tab"
        :class="{ active: activeTab === 'INBOUND' }"
        @click="switchTab('INBOUND')"
      >
        입고
      </button>

      <button
        class="tab"
        :class="{ active: activeTab === 'OUTBOUND' }"
        @click="switchTab('OUTBOUND')"
      >
        출고
      </button>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <label>날짜 범위</label>
          <input placeholder="날짜 범위 선택" />
        </div>
        <div class="filter-actions">
          <button class="btn primary">검색</button>
          <button class="btn">초기화</button>
        </div>
      </div>
    </div>

    <inbound-table 
      v-if="activeTab === 'INBOUND'"
      :items="inboundStore.items"
      @open-modal="(data) => openDetailModal('INBOUND', data)"
    />
    <outbound-table 
      v-else
      :items="outboundStore.items"
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
import { useInboundStore } from '@/stores/inventory/inbound'
import { useOutboundStore } from '@/stores/inventory/outbound'
import InboundTable from './tables/InboundTable.vue'
import OutboundTable from './tables/OutboundTable.vue'
import InboundItemModal from './modal/InboundItemModal.vue'
import OutboundItemModal from './modal/OutboundItemModal.vue'

const inboundStore = useInboundStore()
const outboundStore = useOutboundStore()

const activeTab = ref('INBOUND')

const showModal = ref(false)
const currentType = ref('')
const selectedNo = ref('')

// 기본 파라미터 (추후 pagination 구현 시 ref 로 변경 가능)
const page = 1
const numOfRows = 20
const totalCount = 0

onMounted(() => {
  inboundStore.fetchInbound({page, numOfRows, totalCount})
})

function switchTab(tab) {
  activeTab.value = tab

  if (tab === 'OUTBOUND' && outboundStore.items.length === 0) {
    outboundStore.fetchOutbound({ page, numOfRows, totalCount })
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
</style>
