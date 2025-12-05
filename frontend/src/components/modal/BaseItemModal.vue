<template>
  <div class="modal-backdrop" @click.self="close">
    <div class="modal">
      <header class="modal-header">
        <h3>{{ title }}</h3>
        <button class="close-btn" @click="close">×</button>
      </header>

      <section class="modal-body">
        <div class="filters">
          <select v-model="filters.largeCategoryId" class="select" @change="onLargeCategoryChange">
            <option :value="null">전체 대분류</option>
            <option v-for="cat in largeCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>

          <select
            v-model="filters.mediumCategoryId"
            class="select"
            :disabled="!filters.largeCategoryId"
            @change="onMediumCategoryChange"
          >
            <option :value="null">전체 중분류</option>
            <option v-for="cat in mediumCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>

          <select
            v-model="filters.smallCategoryId"
            class="select"
            :disabled="!filters.mediumCategoryId"
            @change="fetchProducts"
          >
            <option :value="null">전체 소분류</option>
            <option v-for="cat in smallCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>

          <input
            v-model="filters.keyword"
            placeholder="제품 SKU 또는 이름 검색..."
            class="search"
            @input="onSearchInput"
          />
        </div>

        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>상품을 불러오는 중...</p>
        </div>

        <div v-else-if="error" class="error-state">
          <p class="error-message">{{ error }}</p>
          <button class="btn btn-secondary" @click="fetchProducts">다시 시도</button>
        </div>

        <div v-else class="table-wrap">
          <table class="items-table">
            <thead>
              <tr>
                <th>
                  <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
                </th>
                <th>SKU</th>
                <th>제품명</th>
                <th v-if="showPrice">{{ priceLabel }}</th>
                <th v-if="showStock">재고</th>
                <th v-if="showLeadTime">리드 타임</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.sku">
                <td><input type="checkbox" v-model="selectedMap[item.sku]" /></td>
                <td>
                  <code class="sku">{{ item.sku }}</code>
                </td>
                <td>{{ item.name }}</td>
                <td v-if="showPrice" class="numeric">
                  <Money :value="item.price" />
                </td>
                <td v-if="showStock" class="numeric">{{ item.stock?.toLocaleString() ?? '-' }}</td>
                <td v-if="showLeadTime" class="numeric">{{ item.lead }}일</td>
              </tr>
              <tr v-if="!items.length">
                <td
                  :colspan="3 + (showPrice ? 1 : 0) + (showStock ? 1 : 0) + (showLeadTime ? 1 : 0)"
                  class="empty-state"
                >
                  검색 결과가 없습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <footer class="modal-footer">
        <div class="selected-count">{{ selectedCount }}개 선택됨</div>
        <div class="actions">
          <button class="btn btn-secondary" @click="close">취소</button>
          <button class="btn btn-primary" :disabled="selectedCount === 0" @click="addSelected">
            선택된 품목 추가 ({{ selectedCount }})
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import Money from '@/components/global/Money.vue'
import apiClient from '../api'

const emit = defineEmits(['close', 'add'])

const props = defineProps({
  title: {
    type: String,
    default: '품목 추가',
  },
  // 상품 목록을 로드하는 함수 (외부에서 주입)
  fetchProductsFn: {
    type: Function,
    required: true,
  },
  // 추가적인 필터 파라미터 (공급사 ID 등)
  additionalFilters: {
    type: Object,
    default: () => ({}),
  },
  // 단가 칸럼 표시 여부
  showPrice: {
    type: Boolean,
    default: true,
  },
  // 가격 칸럼 라벨 (단가 또는 가격)
  priceLabel: {
    type: String,
    default: '단가',
  },
  // 재고 칸럼 표시 여부
  showStock: {
    type: Boolean,
    default: true,
  },
  // 리드타임 칸럼 표시 여부
  showLeadTime: {
    type: Boolean,
    default: true,
  },
})

// --- State (반응형 데이터) ---
const items = ref([])
const selectedMap = reactive({}) // SKU를 키로 사용하여 선택 상태 관리
const loading = ref(false)
const error = ref(null)

// 카테고리 데이터
const largeCategories = ref([])
const mediumCategories = ref([])
const smallCategories = ref([])

// 필터 상태
const filters = reactive({
  largeCategoryId: null,
  mediumCategoryId: null,
  smallCategoryId: null,
  keyword: '',
})

// 검색 디바운스 타이머
let searchTimeout = null

// --- Computed (계산된 속성) ---
const selectedCount = computed(() => {
  return Object.values(selectedMap).filter(Boolean).length
})

const isAllSelected = computed(() => {
  const productSkus = items.value.map((item) => item.sku)
  if (productSkus.length === 0) return false

  return productSkus.every((sku) => selectedMap[sku])
})

// --- Utilities (유틸리티) ---

// API 응답 객체를 컴포넌트에서 사용할 형식으로 정규화 (normalize)
function normalizeProduct(p) {
  return {
    productId: p.productId || p.id,
    sku: p.productCode || p.sku || p.code,
    name: p.productName || p.name || p.product_name,
    price: Number(p.price || p.unitPrice || 0),
    stock: p.stockQuantity ?? p.stock ?? null,
    lead: Number(p.leadTimeDays || p.lead_time_days || 1),
    _raw: p,
  }
}

// --- API Calls (데이터 로드 함수) ---

/**
 * 상품 목록을 외부 주입 함수로부터 로드합니다.
 */
async function fetchProducts() {
  loading.value = true
  error.value = null

  try {
    // 필터와 추가 파라미터를 합쳐서 전달
    const params = {
      ...props.additionalFilters,
      largeCategoryId: filters.largeCategoryId,
      mediumCategoryId: filters.mediumCategoryId,
      smallCategoryId: filters.smallCategoryId,
      keyword: filters.keyword.trim() || undefined,
    }

    const productlist = await props.fetchProductsFn(params)

    // items.value에 정규화된 상품 목록 저장
    items.value = productlist.map(normalizeProduct)
  } catch (e) {
    console.error('상품 로드 실패:', e)
    error.value = e.message || '상품 목록을 불러오는 데 실패했습니다.'
    items.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 대/중/소 카테고리를 로드하는 함수들
 */
async function loadLargeCategories() {
  try {
    const res = await apiClient.get('/api/v1/categories/top').then((r) => r.data)
    largeCategories.value = res || []
  } catch (e) {
    console.warn('대분류 로드 실패:', e)
    largeCategories.value = []
  }
}

async function loadMediumCategories(id) {
  if (!id) {
    mediumCategories.value = []
    return
  }
  try {
    const res = await apiClient.get(`/api/v1/categories/${id}/children`).then((r) => r.data)
    mediumCategories.value = res || []
  } catch (e) {
    console.warn('중분류 로드 실패:', e)
    mediumCategories.value = []
  }
}

async function loadSmallCategories(id) {
  if (!id) {
    smallCategories.value = []
    return
  }
  try {
    const res = await apiClient.get(`/api/v1/categories/${id}/children`).then((r) => r.data)
    smallCategories.value = res || []
  } catch (e) {
    console.warn('소분류 로드 실패:', e)
    smallCategories.value = []
  }
}

// --- Handlers (이벤트 핸들러) ---

// 대분류 변경 핸들러
async function onLargeCategoryChange() {
  filters.mediumCategoryId = null
  filters.smallCategoryId = null
  mediumCategories.value = []
  smallCategories.value = []
  if (filters.largeCategoryId) await loadMediumCategories(filters.largeCategoryId)
  fetchProducts()
}

// 중분류 변경 핸들러
async function onMediumCategoryChange() {
  filters.smallCategoryId = null
  smallCategories.value = []
  if (filters.mediumCategoryId) await loadSmallCategories(filters.mediumCategoryId)
  fetchProducts()
}

// 검색어 입력 핸들러 (디바운스 적용)
function onSearchInput() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => fetchProducts(), 400)
}

// 전체 선택/해제 토글
function toggleSelectAll(e) {
  const checked = e.target.checked
  items.value.forEach((i) => {
    selectedMap[i.sku] = checked
  })
}

// 선택된 품목 추가 및 모달 닫기
function addSelected() {
  const selected = items.value.filter((i) => selectedMap[i.sku])

  if (!selected.length) {
    alert('품목을 선택하세요.')
    return
  }

  emit('add', selected)
  close()
}

// 모달 닫기
function close() {
  emit('close')
}

// --- Lifecycle Hook ---
onMounted(async () => {
  await loadLargeCategories()
  fetchProducts()
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
  width: 960px;
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
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.select {
  min-width: 140px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  font-size: 14px;
  transition: border-color 0.2s;
}

.select:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f8fafc;
}

.select:focus {
  outline: none;
  border-color: #6b46ff;
  box-shadow: 0 0 0 3px rgba(107, 70, 255, 0.1);
}

.search {
  flex: 1;
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

.items-table tbody tr:hover {
  background: #f8fafc;
}

.numeric {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.sku {
  font-family: 'Consolas', monospace;
  font-size: 13px;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.empty-state {
  text-align: center;
  padding: 48px 16px !important;
  color: #94a3b8;
}

.modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #eef2f7;
  background: #f8fafc;
}

.selected-count {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
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
