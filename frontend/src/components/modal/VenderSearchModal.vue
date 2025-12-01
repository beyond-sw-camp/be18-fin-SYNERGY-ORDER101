<script setup>
import { ref, computed, watch } from 'vue'
import { getSupplierList } from '@/components/api/supplier/supplierService'
import { getFranchiseList } from '@/components/api/store/StoreService'

const props = defineProps({
  currentValue: {
    type: String,
    default: 'ALL',
  },
  currentType: {
    type: String,
    default: 'ALL', // 'ALL' | 'FRANCHISE' | 'SUPPLIER'
    validator: (value) => ['ALL', 'FRANCHISE', 'SUPPLIER'].includes(value),
  },
})

const emit = defineEmits(['close', 'select'])

// 모달 내부에서 타입 선택
const selectedType = ref(props.currentType === 'ALL' ? 'FRANCHISE' : props.currentType)
const searchQuery = ref('')
const selectedVendor = ref(null)
const vendors = ref([])
const isLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

// 타입 옵션 - currentType에 따라 동적으로 결정
const typeOptions = computed(() => {
    if (props.currentType === 'ALL') {
        return [
            { label: '가맹점', value: 'FRANCHISE' },
            { label: '공급업체', value: 'SUPPLIER' }
        ]
    } else if (props.currentType === 'FRANCHISE') {
        return [{ label: '가맹점', value: 'FRANCHISE' }]
    } else {
        return [{ label: '공급업체', value: 'SUPPLIER' }]
    }
})

// 탭 표시 여부
const showTabs = computed(() => props.currentType === 'ALL')

// 동적 타이틀 및 플레이스홀더
const modalTitle = computed(() =>
  selectedType.value === 'FRANCHISE' ? '가맹점 검색' : '공급업체 검색',
)

const searchPlaceholder = computed(() =>
  selectedType.value === 'FRANCHISE'
    ? '가맹점명 또는 코드 검색...'
    : '공급업체명 또는 코드 검색...',
)

// 필터링된 목록 — 이름, 코드, 주소, 연락처로 검색
const filteredVendors = computed(() => {
  if (!searchQuery.value.trim()) return vendors.value

  const query = searchQuery.value.toLowerCase()
  return vendors.value.filter((v) => {
    return (
      (v.text || '').toLowerCase().includes(query) ||
      (v.code || '').toLowerCase().includes(query) ||
      (v.address || '').toLowerCase().includes(query) ||
      (v.contactNumber || '').toLowerCase().includes(query)
    )
  })
})

// 총 페이지 수
const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value))

/**
 * 선택된 타입에 따라 적절한 API 호출
 */
async function fetchVendors(page = 1) {
  isLoading.value = true
  vendors.value = []

  try {
    let data

    if (selectedType.value === 'FRANCHISE') {
      // 가맹점 조회 - API may return store fields (storeId, storeName, storeCode...)
      data = await getFranchiseList(page, pageSize.value, searchQuery.value)

      vendors.value = (data.franchises || []).map((f) => ({
        // prefer store-specific keys, fallback to older names
        text: f.storeName || f.franchiseName || f.name,
        value: f.storeId || f.franchiseId || f.id,
        code: f.storeCode || f.franchiseCode || f.code,
        address: f.address || null,
        contactNumber: f.contactNumber || f.phone || null,
        isActive: f.isActive !== undefined ? f.isActive : true,
        type: 'FRANCHISE',
      }))
    } else {
      // 공급업체 조회
      data = await getSupplierList(page, pageSize.value, searchQuery.value)

      vendors.value = (data.suppliers || []).map((s) => ({
        text: s.supplierName || s.name,
        value: s.supplierId || s.id,
        code: s.supplierCode || s.code,
        type: 'SUPPLIER',
      }))
    }

    totalCount.value = data.totalCount || 0
    currentPage.value = data.currentPage || page
  } catch (error) {
    console.error('목록 조회 실패:', error)
    alert(
      `${selectedType.value === 'FRANCHISE' ? '가맹점' : '공급업체'} 목록을 불러오는 데 실패했습니다.`,
    )
    vendors.value = []
    totalCount.value = 0
  } finally {
    isLoading.value = false
  }
}

/**
 * 타입 변경 핸들러
 */
function handleTypeChange(type) {
  if (selectedType.value === type) return

  selectedType.value = type
  searchQuery.value = ''
  selectedVendor.value = null
  currentPage.value = 1
  fetchVendors(1)
}

/**
 * 검색 실행
 */
function handleSearch() {
  currentPage.value = 1
  fetchVendors(1)
}

/**
 * 페이지 변경
 */
function handlePageChange(page) {
  if (page < 1 || page > totalPages.value) return
  fetchVendors(page)
}

/**
 * 목록에서 항목 선택
 */
function selectVendor(vendor) {
  selectedVendor.value = vendor
}

/**
 * 선택 완료 - 타입과 ID를 함께 전달
 */
function confirm() {
  if (!selectedVendor.value) {
    alert('항목을 선택해주세요.')
    return
  }

  // 부모에게 전달할 데이터 구조
  const result = {
    type: selectedVendor.value.type, // 'FRANCHISE' | 'SUPPLIER'
    id: selectedVendor.value.value, // ID
    name: selectedVendor.value.text, // 이름
    code: selectedVendor.value.code, // 코드
    address: selectedVendor.value.address || null,
    contactNumber: selectedVendor.value.contactNumber || null,
    isActive: selectedVendor.value.isActive,
  }

  console.log('✅ 선택 완료:', result)
  emit('select', result)
  close()
}

/**
 * 모달 닫기
 */
function close() {
  emit('close')
}

// 모달이 열릴 때 초기 데이터 로드
watch(
  () => props.currentValue,
  (newVal) => {
    if (newVal && newVal !== 'ALL') {
      const current = vendors.value.find((v) => v.value === newVal)
      if (current) {
        selectedVendor.value = current
      }
    }
  },
  { immediate: true },
)

// 컴포넌트 마운트 시 초기 로드
watch(
  () => selectedType.value,
  () => {
    fetchVendors(1)
  },
  { immediate: true },
)
</script>

<template>
  <div class="modal-backdrop" @click.self="close">
    <div class="modal-container">
      <header class="modal-header">
        <h3>{{ modalTitle }}</h3>
        <button class="close-btn" @click="close">×</button>
      </header>

      <section class="modal-body">
        <!-- 타입 선택 탭 (ALL일 때만 표시) -->
        <div v-if="showTabs" class="type-tabs">
          <button
            v-for="option in typeOptions"
            :key="option.value"
            class="tab-btn"
            :class="{ active: selectedType === option.value }"
            @click="handleTypeChange(option.value)"
          >
            {{ option.label }}
          </button>
        </div>

        <!-- 검색 입력 -->
        <div class="search-area">
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="searchPlaceholder"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="btn-search" @click="handleSearch">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
            조회
          </button>
        </div>

        <!-- 로딩 상태 -->
        <div v-if="isLoading" class="loading-spinner">
          <div class="spinner"></div>
          <p>목록을 불러오는 중...</p>
        </div>

        <!-- 목록 테이블 -->
        <div v-else class="vendor-list">
          <div v-if="filteredVendors.length === 0" class="empty-state">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db">
              <circle cx="12" cy="12" r="10" stroke-width="2" />
              <path d="M12 6v6l4 2" stroke-width="2" stroke-linecap="round" />
            </svg>
            <p>조회된 {{ selectedType === 'FRANCHISE' ? '가맹점' : '공급업체' }}이 없습니다.</p>
          </div>

          <div
            v-for="vendor in filteredVendors"
            :key="vendor.value"
            class="vendor-item"
            :class="{ active: selectedVendor?.value === vendor.value, inactive: vendor.type === 'FRANCHISE' && !vendor.isActive }"
            @click="(vendor.type === 'SUPPLIER' || vendor.isActive) && selectVendor(vendor)"
          >
            <div class="vendor-info">
              <strong>{{ vendor.text }}</strong>
              <div style="display: flex; gap: 8px; align-items: center; margin-top: 6px">
                <span class="vendor-code">{{ vendor.code || vendor.value }}</span>
                <span style="color: #6b7280; font-size: 13px">{{ vendor.address }}</span>
                <span style="color: #6b7280; font-size: 13px">{{ vendor.contactNumber }}</span>
              </div>
            </div>
            <div style="display: flex; align-items: center; gap: 8px">
              <svg
                v-if="selectedVendor?.value === vendor.value"
                class="check-icon"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
              >
                <path d="M5 13l4 4L19 7" stroke="#6b72f9" stroke-width="2" stroke-linecap="round" />
              </svg>
              <span v-if="vendor.type === 'FRANCHISE' && !vendor.isActive" style="color: #ef4444; font-size: 12px">비활성</span>
            </div>
          </div>
        </div>

        <!-- 페이지네이션 -->
        <div v-if="totalPages > 1" class="pagination">
          <button
            class="page-btn"
            :disabled="currentPage === 1"
            @click="handlePageChange(currentPage - 1)"
          >
            ‹
          </button>

          <button
            v-for="page in totalPages"
            :key="page"
            class="page-btn"
            :class="{ active: page === currentPage }"
            @click="handlePageChange(page)"
          >
            {{ page }}
          </button>

          <button
            class="page-btn"
            :disabled="currentPage === totalPages"
            @click="handlePageChange(currentPage + 1)"
          >
            ›
          </button>
        </div>
      </section>

      <footer class="modal-footer">
        <button class="btn-cancel" @click="close">취소</button>
        <button class="btn-confirm" :disabled="!selectedVendor" @click="confirm">선택 완료</button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-container {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #9ca3af;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 20px 24px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 타입 선택 탭 */
.type-tabs {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: #f3f4f6;
  border-radius: 8px;
}

.tab-btn {
  flex: 1;
  padding: 10px 16px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #374151;
}

.tab-btn.active {
  background: white;
  color: #6b72f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 검색 영역 */
.search-area {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #6b72f9;
  box-shadow: 0 0 0 3px rgba(107, 114, 249, 0.1);
}

.btn-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #6b72f9;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-search:hover {
  background: #5a61e0;
}

/* 로딩 */
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #9ca3af;
  gap: 12px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f4f6;
  border-top-color: #6b72f9;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-spinner p {
  margin: 0;
  font-size: 14px;
}

/* 목록 */
.vendor-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vendor-item {
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vendor-item:hover {
  border-color: #6b72f9;
  background: #f8f9ff;
}

.vendor-item.active {
  border-color: #6b72f9;
  background: #f0f2ff;
}

.vendor-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vendor-code {
  font-size: 12px;
  color: #6b7280;
  font-family: monospace;
}

.check-icon {
  flex-shrink: 0;
}

/* 빈 상태 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #9ca3af;
  gap: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 페이지네이션 */
.pagination {
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  border-color: #6b72f9;
  color: #6b72f9;
}

.page-btn.active {
  background: #6b72f9;
  color: white;
  border-color: #6b72f9;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 푸터 */
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-cancel,
.btn-confirm {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cancel {
  background: #f3f4f6;
  color: #374151;
}

.btn-cancel:hover {
  background: #e5e7eb;
}

.btn-confirm {
  background: #6b72f9;
  color: white;
}

.btn-confirm:hover:not(:disabled) {
  background: #5a61e0;
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
