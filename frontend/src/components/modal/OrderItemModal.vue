<template>
  <div class="modal-backdrop">
    <div class="modal">
      <header class="modal-header">
        <h3>품목 추가</h3>
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

          <select v-model="filters.mediumCategoryId" class="select" :disabled="!filters.largeCategoryId"
            @change="onMediumCategoryChange">
            <option :value="null">전체 중분류</option>
            <option v-for="cat in mediumCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>

          <select v-model="filters.smallCategoryId" class="select" :disabled="!filters.mediumCategoryId"
            @change="onSmallCategoryChange">
            <option :value="null">전체 소분류</option>
            <option v-for="cat in smallCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>

          <input v-model="filters.keyword" placeholder="제품 SKU 또는 이름 검색..." class="search" @input="onSearchInput" />
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
                <th>가격</th>
                <th>재고</th>
                <th>리드 타임</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.productId" @click="toggleSelect(item.productId)" class="clickable-row">
                <td @click.stop><input type="checkbox" v-model="selectedMap[item.productId]" /></td>
                <td><code class="sku">{{ item.sku }}</code></td>
                <td>{{ item.name }}</td>
                <td class="numeric">
                  <Money :value="item.price" />
                </td>
                <td class="numeric">{{ item.stock?.toLocaleString() ?? '-' }}</td>
                <td class="numeric">{{ item.lead }}일</td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="6" class="empty-state">검색 결과가 없습니다.</td>
              </tr>
            </tbody>
          </table>

          <!-- 페이지네이션 -->
          <div class="pagination" v-if="totalPages > 1">
            <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(1)">«</button>
            <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">‹</button>
            <button 
              v-for="page in visiblePages" 
              :key="page" 
              class="page-btn" 
              :class="{ active: page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
            <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">›</button>
            <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(totalPages)">»</button>
            <span class="page-info">{{ currentPage }} / {{ totalPages }} (총 {{ totalCount }}개)</span>
          </div>
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
import axios from 'axios'
import { getSupplierDetail } from '@/components/api/supplier/supplierService.js'
import { getInventoryList } from '@/components/api/warehouse/WarehouseService.js'
import { useAuthStore } from '@/stores/authStore.js'
import Money from '@/components/global/Money.vue' 

const emit = defineEmits(['close', 'add'])
const authStore = useAuthStore()

const props = defineProps({
  initialSupplierId: {
    type: [String, Number],
    default: null
  },
  storeId: {
    type: [String, Number],
    default: null
  }
})

// --- State (반응형 데이터) ---
const items = ref([])
const selectedMap = reactive({}) // SKU를 키로 사용하여 선택 상태 관리
const storeInventoryMap = ref({}) // 가맹점 재고 Map (productId -> stock)
const loading = ref(false)
const error = ref(null)

// 페이지네이션 상태
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)
const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value) || 1)

// 카테고리 데이터
const largeCategories = ref([])
const mediumCategories = ref([])
const smallCategories = ref([])

// 필터 상태 (props에서 받은 공급사 ID 초기값으로 사용)
const filters = reactive({
  supplierId: props.initialSupplierId,
  largeCategoryId: null,
  mediumCategoryId: null,
  smallCategoryId: null,
  keyword: ''
})

// 검색 디바운스 타이머
let searchTimeout = null

// --- Computed (계산된 속성) ---
const selectedCount = computed(() => {
  return Object.values(selectedMap).filter(Boolean).length
})

const isAllSelected = computed(() => {
  const productIds = items.value.map(item => item.productId)
  if (productIds.length === 0) return false // 품목이 없으면 전체 선택도 아님

  return productIds.every(id => selectedMap[id])
})

// 페이지 번호 목록 (5개씩 표시)
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// --- Utilities (유틸리티) ---

// API 응답 객체를 컴포넌트에서 사용할 형식으로 정규화 (normalize)
function normalizeProduct(p) {
  return {
    productId: p.productId || p.id,
    sku: p.productCode || p.sku || p.code,
    name: p.productName || p.name || p.product_name,
    price: Number(p.price || p.unitPrice || 0), // 납품가 (판매가)
    purchasePrice: Number(p.purchasePrice || p.supplyPrice || p.price || 0), // 공급가
    stock: p.stockQuantity ?? p.stock ?? null,
    lead: Number(p.leadTimeDays || p.lead_time_days || 1),
    _raw: p
  }
}

// --- API Calls (데이터 로드 함수) ---

/**
 * 상품 목록을 API로부터 로드합니다.
 */
async function fetchProducts() {
  loading.value = true
  error.value = null

  try {
    let productlist = []

    // 본사(HQ)인지 가맹점(STORE)인지 확인
    const isHQ = authStore.userInfo.role === 'HQ_ADMIN' || authStore.userInfo.role === 'HQ_USER'
    const isStore = authStore.userInfo.role === 'STORE_ADMIN' || authStore.userInfo.role === 'STORE_USER'

    // 재고 Map 초기화
    storeInventoryMap.value = {}

    if (isStore) {
      // 가맹점 재고 조회 (storeId가 유효한 경우에만)
      const storeIdValue = props.storeId ? Number(props.storeId) : null
      if (storeIdValue && !isNaN(storeIdValue)) {
        try {
          const invRes = await axios.get(`/api/v1/stores/${storeIdValue}/inventory`)
          const invItems = invRes.data?.items || []
          // productId를 키로 재고 Map 생성
          invItems.forEach(inv => {
            if (inv.productId) {
              storeInventoryMap.value[inv.productId] = inv.onHandQty ?? 0
            }
            if (inv.productCode) {
              storeInventoryMap.value[inv.productCode] = inv.onHandQty ?? 0
            }
          })
        } catch (invErr) {
          console.warn('가맹점 재고 조회 실패:', invErr)
        }
      }
    } else if (isHQ) {
      // 본사일 경우 창고 재고 조회
      try {
        const invRes = await getInventoryList(1, 9999) // 전체 재고 조회
        const invItems = invRes.inventories || []
        // productCode를 키로 재고 Map 생성
        invItems.forEach(inv => {
          if (inv.productCode) {
            storeInventoryMap.value[inv.productCode] = inv.onHandQty ?? 0
          }
          if (inv.productId) {
            storeInventoryMap.value[inv.productId] = inv.onHandQty ?? 0
          }
        })
      } catch (invErr) {
        console.warn('창고 재고 조회 실패:', invErr)
      }
    }

    // 공급사 ID가 있으면 공급사 상세 API 사용 (공급가 포함)
    if (filters.supplierId) {
      const detail = await getSupplierDetail(filters.supplierId, currentPage.value, pageSize.value, filters.keyword.trim() || '')
      productlist = detail.products || []
      totalCount.value = detail.totalCount || 0
    } else {
      // 공급사 미지정 시 기존 제품 목록 API 사용
      const params = {
        page: currentPage.value,
        numOfRows: pageSize.value,
        keyword: filters.keyword.trim() || undefined
      }
      
      // 카테고리 ID가 있을 때만 추가
      if (filters.largeCategoryId) params.largeCategoryId = filters.largeCategoryId
      if (filters.mediumCategoryId) params.mediumCategoryId = filters.mediumCategoryId
      if (filters.smallCategoryId) params.smallCategoryId = filters.smallCategoryId
      
      const res = await axios.get('/api/v1/products', { params }).then(r => r.data)
      
      // API 응답: { items: [...상품배열], totalCount, page }
      productlist = res.items || []
      totalCount.value = res.totalCount || 0
    }

    // items.value에 정규화된 상품 목록 저장 (가맹점 재고 반영)
    items.value = productlist.map(p => {
      const normalized = normalizeProduct(p)
      // 가맹점 재고 매핑 (productId 또는 productCode로)
      const stockFromStore = storeInventoryMap.value[normalized.productId] ?? storeInventoryMap.value[normalized.sku] ?? 0
      return {
        ...normalized,
        stock: stockFromStore
      }
    })

  } catch (e) {
    console.error('상품 로드 실패:', e)
    error.value = e.message || '상품 목록을 불러오는 데 실패했습니다.'
    items.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 대/중/소 카테고리를 로드하는 함수들 (유지)
 */
async function loadLargeCategories() {
  try {
    const res = await axios.get('/api/v1/categories/top').then(r => r.data)
    largeCategories.value = res || []
  } catch (e) {
    largeCategories.value = []
  }
}

async function loadMediumCategories(id) {
  if (!id) { mediumCategories.value = []; return }
  try {
    const res = await axios.get(`/api/v1/categories/${id}/children`).then(r => r.data)
    mediumCategories.value = res || []
  } catch (e) {
    mediumCategories.value = []
  }
}

async function loadSmallCategories(id) {
  if (!id) { smallCategories.value = []; return }
  try {
    const res = await axios.get(`/api/v1/categories/${id}/children`).then(r => r.data)
    smallCategories.value = res || []
  } catch (e) {
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
  currentPage.value = 1
  if (filters.largeCategoryId) await loadMediumCategories(filters.largeCategoryId)
  fetchProducts()
}

// 중분류 변경 핸들러
async function onMediumCategoryChange() {
  filters.smallCategoryId = null
  smallCategories.value = []
  currentPage.value = 1
  if (filters.mediumCategoryId) await loadSmallCategories(filters.mediumCategoryId)
  fetchProducts()
}

// 소분류 변경 핸들러
function onSmallCategoryChange() {
  currentPage.value = 1
  fetchProducts()
}

// 검색어 입력 핸들러 (디바운스 적용)
function onSearchInput() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    fetchProducts()
  }, 400)
}

// 페이지 이동
function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchProducts()
}

// 전체 선택/해제 토글
function toggleSelectAll(e) {
  const checked = e.target.checked
  items.value.forEach(i => { selectedMap[i.productId] = checked })
}

// 개별 항목 선택 토글 (행 클릭 시)
function toggleSelect(productId) {
  selectedMap[productId] = !selectedMap[productId]
}

// 선택된 품목 추가 및 모달 닫기
function addSelected() {
  // selectedMap을 기반으로 실제 선택된 품목 객체만 필터링
  const selected = items.value.filter(i => selectedMap[i.productId])

  if (!selected.length) {
    alert('품목을 선택하세요.')
    return
  }

  // 부모 컴포넌트에 선택된 품목 배열을 전달
  emit('add', selected)

  // 선택 상태 초기화 (옵션) 및 모달 닫기
  // Object.keys(selectedMap).forEach(k => { selectedMap[k] = false }) // 필요한 경우 주석 해제
  close()
}

// 모달 닫기
function close() {
  emit('close')
}

// --- Lifecycle Hook ---
onMounted(async () => {
  // 카테고리 로드 및 상품 목록 로드는 비동기로 병렬 처리 가능
  await loadLargeCategories()

  // 초기 상품 목록 로드
  fetchProducts()
})
</script>

<style scoped>
/* CSS 부분은 변경 없이 원본 스타일을 유지합니다. */
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

.items-table tbody tr.clickable-row {
  cursor: pointer;
}

.items-table tbody tr.clickable-row:hover {
  background: #eef2ff;
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

/* 페이지네이션 스타일 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #6366f1;
  color: #6366f1;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-btn.active {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border-color: transparent;
  font-weight: 600;
}

.page-info {
  margin-left: 12px;
  font-size: 13px;
  color: #94a3b8;
}
</style>