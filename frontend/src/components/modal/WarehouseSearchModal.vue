<template>
  <div class="modal-backdrop" @click.self="close">
    <div class="modal">
      <header class="modal-header">
        <h3>창고 선택</h3>
        <button class="close-btn" @click="close">×</button>
      </header>

      <section class="modal-body">
        <div class="filters">
          <input
            v-model="keyword"
            placeholder="창고명 검색..."
            class="search"
            @input="onSearchInput"
          />
        </div>

        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>창고 목록을 불러오는 중...</p>
        </div>

        <div v-else-if="error" class="error-state">
          <p class="error-message">{{ error }}</p>
          <button class="btn btn-secondary" @click="fetchWarehouses">다시 시도</button>
        </div>

        <div v-else class="table-wrap">
          <table class="items-table">
            <thead>
              <tr>
                <th>선택</th>
                <th>창고 ID</th>
                <th>창고명</th>
                <th>위치</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="warehouse in filteredWarehouses"
                :key="warehouse.warehouseId"
                :class="{ selected: selectedWarehouse?.warehouseId === warehouse.warehouseId }"
                @click="selectWarehouse(warehouse)"
              >
                <td>
                  <input
                    type="radio"
                    :checked="selectedWarehouse?.warehouseId === warehouse.warehouseId"
                    @change="selectWarehouse(warehouse)"
                  />
                </td>
                <td>{{ warehouse.warehouseId }}</td>
                <td>{{ warehouse.warehouseName || warehouse.name }}</td>
                <td>{{ warehouse.address || warehouse.location || '-' }}</td>
              </tr>
              <tr v-if="!filteredWarehouses.length">
                <td colspan="4" class="empty-state">검색 결과가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <footer class="modal-footer">
        <div class="actions">
          <button class="btn btn-secondary" @click="close">취소</button>
          <button
            class="btn btn-primary"
            :disabled="!selectedWarehouse"
            @click="confirmSelection"
          >
            선택 완료
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWarehouseList } from '@/components/api/warehouse/WarehouseService.js'

const emit = defineEmits(['close', 'select'])

const props = defineProps({
  currentWarehouse: {
    type: Object,
    default: null
  }
})

// --- State ---
const warehouses = ref([])
const selectedWarehouse = ref(props.currentWarehouse)
const keyword = ref('')
const loading = ref(false)
const error = ref(null)

let searchTimeout = null

// --- Computed ---
const filteredWarehouses = computed(() => {
  if (!keyword.value.trim()) return warehouses.value

  const lowerKeyword = keyword.value.toLowerCase()
  return warehouses.value.filter(w => 
    w.name?.toLowerCase().includes(lowerKeyword) ||
    w.warehouseName?.toLowerCase().includes(lowerKeyword) ||
    w.warehouseCode?.toLowerCase().includes(lowerKeyword) ||
    w.address?.toLowerCase().includes(lowerKeyword)
  )
})

// --- Methods ---
async function fetchWarehouses() {
  loading.value = true
  error.value = null

  try {
    // WarehouseService를 통해 창고 목록 API 호출
    const result = await getWarehouseList(1, 100)
    warehouses.value = result.warehouses || []
  } catch (e) {
    console.error('창고 목록 로드 실패:', e)
    error.value = e.message || '창고 목록을 불러오는 데 실패했습니다.'
    warehouses.value = []
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    // 클라이언트 사이드 필터링이므로 별도 API 호출 불필요
  }, 300)
}

function selectWarehouse(warehouse) {
  selectedWarehouse.value = warehouse
}

function confirmSelection() {
  if (selectedWarehouse.value) {
    emit('select', selectedWarehouse.value)
    close()
  }
}

function close() {
  emit('close')
}

// --- Lifecycle ---
onMounted(() => {
  fetchWarehouses()
})
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal {
  width: 600px;
  max-width: calc(100% - 40px);
  max-height: calc(100vh - 40px);
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #94a3b8;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.modal-body {
  padding: 20px 24px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.filters {
  margin-bottom: 16px;
}

.search {
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  font-size: 14px;
}

.search:focus {
  outline: none;
  border-color: #6b46ff;
  box-shadow: 0 0 0 3px rgba(107, 70, 255, 0.1);
}

.loading-state,
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #64748b;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #6b46ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-message {
  color: #ef4444;
  font-weight: 500;
}

.table-wrap {
  flex: 1;
  overflow: auto;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  color: #475569;
  border-bottom: 2px solid #e2e8f0;
}

.items-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
}

.items-table tbody tr {
  cursor: pointer;
  transition: background 0.2s;
}

.items-table tbody tr:hover {
  background: #f8fafc;
}

.items-table tbody tr.selected {
  background: #eef2ff;
}

.empty-state {
  text-align: center;
  padding: 48px 16px !important;
  color: #94a3b8;
}

.modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-top: 1px solid #eef2f7;
  background: #f8fafc;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: #6b46ff;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #5a38d6;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.btn-secondary:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}
</style>
