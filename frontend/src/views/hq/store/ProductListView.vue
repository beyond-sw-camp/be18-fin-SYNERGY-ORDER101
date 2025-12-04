<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>상품 목록</h1>
      <div class="header-actions">
        <button class="btn-primary" @click="goCreate">상품 등록</button>
      </div>
    </header>

    <section class="card">
      <div class="filters">
        <select v-model="largeCategoryId">
          <option value="">대분류 전체</option>
          <option v-for="c in largeCategories" :key="c.id" :value="String(c.id)">
            {{ c.name }}
          </option>
        </select>
        <select v-model="mediumCategoryId" :disabled="mediumCategories.length === 0">
          <option value="">중분류 전체</option>
          <option v-for="c in mediumCategories" :key="c.id" :value="String(c.id)">
            {{ c.name }}
          </option>
        </select>
        <select v-model="smallCategoryId" :disabled="smallCategories.length === 0">
          <option value="">소분류 전체</option>
          <option v-for="c in smallCategories" :key="c.id" :value="String(c.id)">
            {{ c.name }}
          </option>
        </select>
        <input
          v-model="keyword"
          class="search"
          placeholder="상품명 검색..."
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">검색</button>
      </div>

      <h3 class="card-title">모든 상품</h3>

      <div class="table-wrap">
        <table class="product-table">
          <thead>
            <tr>
              <th>상품 코드</th>
              <th>카테고리</th>
              <th>제품명</th>
              <th class="numeric">공급가</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="p in products"
              :key="p.productId"
              class="clickable-row"
              @click="goDetail(p.productId)"
            >
              <td class="code">{{ p.productCode }}</td>
              <td>{{ p.categoryName }}</td>
              <td>{{ p.productName }}</td>
              <td class="numeric"><Money :value="p.price" /></td>
              <td>
                <span :class="['chip', p.status ? 's-active' : 's-inactive']">{{
                  p.status ? '활성' : '비활성'
                }}</span>
              </td>
            </tr>
            <tr v-if="products.length === 0">
              <td colspan="5" class="no-data">등록된 상품이 없습니다.</td>
            </tr>
            <tr v-if="isLoading">
              <td colspan="6" class="empty-data">목록을 불러오는 중...</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="pager" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
          ‹ Previous
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
          Next ›
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { getProductList } from '@/components/api/product/productService'
import { getTopCategories, getChildCategories } from '@/components/api/product/categoryService'

const router = useRouter()

const MAX_VISIBLE_PAGES = 5

const products = ref([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const isLoading = ref(false)

const largeCategoryId = ref('')
const mediumCategoryId = ref('')
const smallCategoryId = ref('')

const largeCategories = ref([])
const mediumCategories = ref([])
const smallCategories = ref([])

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

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const fetchProducts = async (page = 1) => {
  isLoading.value = true
  try {
    const data = await getProductList(
      page,
      pageSize.value,
      keyword.value,
      largeCategoryId.value ? Number(largeCategoryId.value) : null,
      mediumCategoryId.value ? Number(mediumCategoryId.value) : null,
      smallCategoryId.value ? Number(smallCategoryId.value) : null,
    )

    products.value = data.products
    totalCount.value = data.totalCount
    currentPage.value = data.currentPage
  } catch (e) {
    console.error(e)
    alert('상품 목록 조회에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

const initCategories = async () => {
  largeCategories.value = await getTopCategories()
}

watch(largeCategoryId, async (newVal) => {
  mediumCategoryId.value = ''
  smallCategoryId.value = ''
  mediumCategories.value = []
  smallCategories.value = []

  if (newVal) {
    mediumCategories.value = await getChildCategories(Number(newVal))
  }
  fetchProducts(1)
})

watch(mediumCategoryId, async (newVal) => {
  smallCategories.value = []
  smallCategoryId.value = ''

  if (newVal) {
    smallCategories.value = await getChildCategories(Number(newVal))
  }
  fetchProducts(1)
})

watch(smallCategoryId, () => {
  fetchProducts(1)
})

const handleSearch = () => {
  fetchProducts(1)
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  fetchProducts(page)
}

const goDetail = (productId) => {
  // 실제 라우트 이름/파라미터에 맞게 수정
  router.push({ name: 'hq-product-detail', params: { id: productId } })
}

const goCreate = () => {
  router.push({ name: 'hq-product-register' })
}

onMounted(async () => {
  await initCategories() // 대분류 먼저 로딩
  fetchProducts(1) // 첫 페이지 상품 조회
})
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
.header-actions .btn-primary {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
}
.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 16px;
}
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.filters select {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6ea;
}
.card-title {
  font-size: 16px;
  margin-bottom: 12px;
}
.product-table {
  width: 100%;
  border-collapse: collapse;
}
.product-table th,
.product-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
}
.product-table td.numeric {
  text-align: left;
}
.code {
  font-weight: 600;
}
.chip {
  padding: 6px 10px;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
}
.s-active {
  background: #6b46ff;
}
.s-inactive {
  background: #e6e6ea;
  color: #333;
}
.pagination {
  margin-top: 18px;
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
}
.page {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #fff;
  cursor: pointer;
}
.page.active {
  background: #6b46ff;
  color: #fff;
  border-color: #6b46ff;
}
.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
}
.clickable-row {
  cursor: pointer;
}

.page-btn {
  min-width: 32px;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
  line-height: 1;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px; /* input과 버튼 사이 간격 */
  max-width: 360px; /* 전체 검색바 가로 폭 */
}

.search {
  flex: 1 1 auto; /* 남는 공간 다 쓰기 */
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

/* 검색 버튼 */
.search-btn {
  flex: 0 0 80px; /* 버튼 폭 고정 */
  padding: 8px 0; /* 위아래만 패딩 → 폭은 flex가 결정 */
  border-radius: 8px;
  border: 1px solid #6b46ff;
  background: #6b46ff;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.08s ease;
}

.search-btn:hover {
  background: #5a3ae0;
  box-shadow: 0 2px 6px rgba(107, 70, 255, 0.25);
}

.search-btn:active {
  transform: translateY(1px);
  box-shadow: 0 1px 3px rgba(107, 70, 255, 0.2);
}

.search-btn:disabled {
  background: #cbd5e1;
  border-color: #cbd5e1;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
