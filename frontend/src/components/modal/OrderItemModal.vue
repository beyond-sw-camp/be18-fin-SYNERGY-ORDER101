<template>
  <div class="modal-backdrop">
    <div class="modal">
      <header class="modal-header">
        <h3>품목 추가</h3>
        <button class="close-btn" @click="close">×</button>
      </header>

      <section class="modal-body">
        <div class="filters">
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
                <th class="right">가격</th>
                <th class="right">리드 타임</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in items"
                :key="item.productId"
                @click="toggleSelect(item)"
                :class="['clickable-row', { inactive: !item.isActive }]"
              >
                <td @click.stop>
                  <input type="checkbox" :checked="!!selectedMap[item.productId]" @change="toggleSelect(item)" :disabled="!item.isActive" />
                </td>
                <td>
                  <code class="sku">{{ item.sku }}</code>
                </td>
                <td>{{ item.name }}</td>
                <td class="numeric">
                  <Money :value="usePurchasePrice ? item.purchasePrice : item.price" />
                </td>
                <td class="numeric">{{ item.lead }}일</td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="6" class="empty-state">검색 결과가 없습니다.</td>
              </tr>
            </tbody>
          </table>
          <!--  페이지네이션 -->
          <div v-if="totalPages > 1" class="pagination">
            <button
              class="pager"
              :disabled="currentPage === 1"
              @click="changePage(currentPage - 1)"
            >
              ‹ 이전
            </button>

            <button
              v-for="page in pageNumbers"
              :key="page"
              class="page"
              :class="{ active: page === currentPage }"
              @click="changePage(page)"
            >
              {{ page }}
            </button>

            <button
              class="pager"
              :disabled="currentPage === totalPages"
              @click="changePage(currentPage + 1)"
            >
              다음 ›
            </button>
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
import apiClient from '@/components/api'
import { getSupplierDetail } from '@/components/api/supplier/supplierService.js'
import { useAuthStore } from '@/stores/authStore'
import Money from '@/components/global/Money.vue'

const emit = defineEmits(['close', 'add'])
const authStore = useAuthStore()

const props = defineProps({
  initialSupplierId: {
    type: [String, Number],
    default: null,
  },
  selectedProductIds: {
    type: Array,
    default: () => [],
  },
  // 공급가(구매가) 기준으로 표시/선택할지 여부 (HQ 발주 화면에서 사용)
  usePurchasePrice: {
    type: Boolean,
    default: false,
  },
})

// --- State (반응형 데이터) ---
const items = ref([])
const selectedMap = reactive({}) // SKU를 키로 사용하여 선택 상태 관리
const storeInventoryMap = ref({}) // 가맹점 재고 Map (productId -> stock)
const loading = ref(false)
const error = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const MAX_VISIBLE_PAGES = 5

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
  keyword: '',
})

// 검색 디바운스 타이머
let searchTimeout = null

// --- Computed (계산된 속성) ---
const selectedCount = computed(() => {
  return Object.keys(selectedMap).length
})

const isAllSelected = computed(() => {
  const ids = items.value.map((item) => item.productId)
  if (!ids.length) return false
  return ids.every((id) => !!selectedMap[id])
})

const totalPages = computed(() =>
  totalCount.value > 0 ? Math.ceil(totalCount.value / pageSize.value) : 1,
)

const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)

  let start = Math.max(1, current - half)
  let end = Math.min(total, start + MAX_VISIBLE_PAGES - 1)

  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(1, end - MAX_VISIBLE_PAGES + 1)
  }

  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// --- Utilities (유틸리티) ---

// API 응답 객체를 컴포넌트에서 사용할 형식으로 정규화 (normalize)
function normalizeProduct(p) {
  // 백엔드에서 전달되는 여러 형태의 상태 필드를 확인하여 isActive로 정규화
  const rawStatus = p.status ?? p.product?.status ?? p.isActive ?? p.active
  let isActive = true
  if (rawStatus !== undefined && rawStatus !== null) {
    if (typeof rawStatus === 'boolean') {
      isActive = rawStatus
    } else if (typeof rawStatus === 'number') {
      isActive = rawStatus !== 0
    } else {
      const s = String(rawStatus).toLowerCase()
      isActive = s !== '0' && s !== 'false'
    }
  }

  const productId = p.productId || p.id
  // 이미 선택된 상품인지 확인
  const isAlreadySelected = props.selectedProductIds.includes(productId)

  const normalized = {
    productId: productId,
    sku: p.productCode || p.sku || p.code,
    name: p.productName || p.name || p.product_name,
    price: Number(p.price || p.unitPrice || 0), // 납품가 (판매가)
    purchasePrice: Number(p.purchasePrice || p.supplyPrice || p.price || 0), // 공급가
    stock: p.stockQuantity ?? p.stock ?? null,
    lead: Number(p.leadTimeDays || p.lead_time_days || 1),
    _raw: p,
    isActive: isActive && !isAlreadySelected, // 이미 선택된 상품은 비활성화
  }

  return normalized
}

// --- API Calls (데이터 로드 함수) ---

/**
 * 상품 목록을 API로부터 로드합니다.
 */
async function fetchProducts(page = 1) {
  loading.value = true
  error.value = null

  try {
    let productlist = []
    let pageInfo = { page, pageSize: pageSize.value, totalCount: 0 }

    if (filters.supplierId) {
      // 공급사 상세 API + 페이징 사용
      const detail = await getSupplierDetail(
        filters.supplierId,
        page, // 1 기반
        pageSize.value,
        filters.keyword,
      )

      productlist = detail.products ?? []
      pageInfo.page = detail.page ?? page
      pageInfo.pageSize = detail.pageSize ?? pageSize.value
      pageInfo.totalCount = detail.totalCount ?? productlist.length
    } else {
      // 공급사 미지정 → 기존 products API 페이징 사용
      const data = await apiClient
        .get('/api/v1/products', {
          params: {
            page,
            numOfRows: pageSize.value,
            largeCategoryId: filters.largeCategoryId,
            mediumCategoryId: filters.mediumCategoryId,
            smallCategoryId: filters.smallCategoryId,
            keyword: filters.keyword.trim() || undefined,
          },
        })
        .then((r) => r.data)

      productlist = data.items ?? data.products ?? data.content ?? []

      pageInfo.page = data.page ?? page
      pageInfo.pageSize = data.numOfRows ?? pageSize.value
      pageInfo.totalCount = data.totalCount ?? productlist.length
    }

    // 선택 상태 초기화
    // 페이지 변경 시 선택 상태 유지하기 위해 초기화 로직 제거
    // Object.keys(selectedMap).forEach((k) => delete selectedMap[k])

    // 모든 상품을 목록에 노출하되, isActive 플래그로 선택 가능 여부를 제어
    items.value = (productlist || []).map(normalizeProduct)
    // 디버그: 로드된 항목의 isActive 상태 요약을 콘솔에 출력
    currentPage.value = pageInfo.page
    pageSize.value = pageInfo.pageSize
    totalCount.value = pageInfo.totalCount
  } catch (e) {
    error.value = e.message || '상품 목록을 불러오는 데 실패했습니다.'
    items.value = []
    totalCount.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  fetchProducts(page)
}

/**
 * 대/중/소 카테고리를 로드하는 함수들 (유지)
 */
async function loadLargeCategories() {
  try {
    const res = await apiClient.get('/api/v1/categories/top').then((r) => r.data)
    largeCategories.value = res || []
  } catch (e) {
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

// 전체 선택/해제 토글 (비활성 항목은 선택하지 않음)
function toggleSelectAll(e) {
  const checked = e.target.checked
  items.value.forEach((i) => {
    if (!i.isActive) {
      return
    }
    if (checked) {
      selectedMap[i.productId] = i
    } else {
      delete selectedMap[i.productId]
    }
  })
}

// 개별 항목 선택 토글 (행 클릭 시) — 비활성 항목은 선택 불가
function toggleSelect(item) {
  // 비활성화된 항목은 선택할 수 없음
  if (!item.isActive) return

  if (selectedMap[item.productId]) {
    delete selectedMap[item.productId]
  } else {
    selectedMap[item.productId] = item
  }
}

// 선택된 품목 추가 및 모달 닫기
function addSelected() {
  // selectedMap을 기반으로 실제 선택된 품목 객체만 필터링
  const selected = Object.values(selectedMap)

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
.pagination {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

.pager {
  background: transparent;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 6px 8px;
  font-size: 13px;
}
.pager:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.page {
  min-width: 32px;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}
.page.active {
  background: #6b46ff;
  color: #fff;
  border-color: #6b46ff;
  font-weight: 700;
}

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

.items-table tbody tr.inactive {
  opacity: 0.5;
  background: #fafafa;
  cursor: default;
}

.items-table tbody tr.inactive:hover {
  background: #fafafa;
}

.items-table tbody tr.inactive .sku {
  background: transparent;
  color: #94a3b8;
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
