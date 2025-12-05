<template>
  <div class="supplier-detail">
    <h2 class="page-title">공급사 상세 정보</h2>

    <div class="card info-card" v-if="supplier">
      <div class="info-row">
        <div>
          <div class="label">코드</div>
          <div class="value">{{ supplier.supplierCode }}</div>
        </div>
        <div>
          <div class="label">이름</div>
          <div class="value">{{ supplier.supplierName }}</div>
        </div>
        <div>
          <div class="label">주소</div>
          <div class="value">{{ supplier.address }}</div>
        </div>
        <div>
          <div class="label">등록일</div>
          <div class="value">{{ formatDate(supplier.createdAt) }}</div>
        </div>
      </div>

      <div class="divider"></div>

      <div class="contact-row">
        <div>
          <div class="label">담당자</div>
          <div class="value">{{ supplier.contactName }}</div>
        </div>
        <div>
          <div class="label">전화번호</div>
          <div class="value">{{ supplier.contactNumber }}</div>
        </div>
      </div>
    </div>

    <div class="card products-card">
      <h3 class="card-title">제품 목록</h3>

      <input
        v-model="search"
        class="search"
        placeholder="제품명/코드 검색..."
        @keyup.enter="handleSearch"
      />
      <button class="search-btn" @click="handleSearch">검색</button>

      <div class="table-wrap">
        <table class="products-table">
          <thead>
            <tr>
              <th>제품 ID</th>
              <th>공급사 제품 코드</th>
              <th>제품명</th>
              <th>구매 가격</th>
              <th>리드 타임 (일)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="isLoading">
              <td colspan="5" class="empty-data">목록을 불러오는 중...</td>
            </tr>

            <tr v-else-if="filteredProducts.length === 0">
              <td colspan="5" class="empty-data">등록된 제품이 없습니다.</td>
            </tr>

            <tr v-else v-for="p in filteredProducts" :key="p.productId">
              <td>{{ p.productId }}</td>
              <td>{{ p.supplierProductCode }}</td>
              <td>{{ p.productName }}</td>
              <td class="numeric"><Money :value="p.price" /></td>
              <td class="numeric">{{ p.leadTimeDays }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 제품 목록 페이징 -->
      <div class="pagination" v-if="totalPages > 1">
        <button
          class="pager"
          :disabled="currentPage === 1 || isLoading"
          @click="changePage(currentPage - 1)"
        >
          ‹ Previous
        </button>

        <button
          v-for="page in pageNumbers"
          :key="page"
          class="page"
          :class="{ active: page === currentPage }"
          :disabled="isLoading"
          @click="changePage(page)"
        >
          {{ page }}
        </button>

        <button
          class="pager"
          :disabled="currentPage === totalPages || isLoading"
          @click="changePage(currentPage + 1)"
        >
          Next ›
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { getSupplierDetail } from '@/components/api/supplier/supplierService'

const route = useRoute()
const supplierId = Number(route.params.id) // SupplierListView에서 넘겨준 id

const supplier = ref(null)
const products = ref([])

const isLoading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

const search = ref('')

const MAX_VISIBLE_PAGES = 5

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

// 검색어로 현재 페이지의 products만 필터링
const filteredProducts = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return products.value

  return products.value.filter((p) => {
    return (
      p.productName.toLowerCase().includes(q) ||
      p.productCode.toLowerCase().includes(q) ||
      (p.supplierProductCode && p.supplierProductCode.toLowerCase().includes(q))
    )
  })
})

const formatDate = (iso) => {
  if (!iso) return ''
  return iso.toString().slice(0, 10)
}

const fetchSupplierDetail = async (page = 1) => {
  if (!supplierId) return

  isLoading.value = true
  try {
    const data = await getSupplierDetail(supplierId, page, pageSize.value, search.value)

    supplier.value = data.supplier
    products.value = data.products
    currentPage.value = data.page
    totalCount.value = data.totalCount
    // pageSize는 서버에서 바뀌어 올 수도 있음
    if (data.pageSize) {
      pageSize.value = data.pageSize
    }
  } catch (e) {
    console.error(e)
    alert('공급사 상세 조회에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  fetchSupplierDetail(page)
}

onMounted(() => {
  fetchSupplierDetail(1)
})

const handleSearch = () => {
  fetchSupplierDetail(1) // 항상 1페이지부터 다시
}
</script>

<style scoped>
.page-title {
  font-size: 20px;
  margin-bottom: 12px;
}
.info-card {
  padding: 18px;
  margin-bottom: 18px;
}
.info-row {
  display: flex;
  gap: 24px;
}
.label {
  color: #6b7280;
  font-size: 13px;
}
.value {
  font-weight: 600;
  margin-top: 6px;
}
.divider {
  height: 1px;
  background: #eef2f7;
  margin: 14px 0;
}
.products-card {
  padding: 18px;
}
.products-table {
  width: 100%;
  border-collapse: collapse;
}
.products-table thead th {
  text-align: left;
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
}
.products-table tbody td {
  padding: 12px;
  border-top: 1px solid #f7f7f9;
}
.numeric {
  text-align: left;
}
.search {
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 12px;
  outline: none;
}

/* 검색 버튼 */
.search-btn {
  margin-left: 6px;
  padding: 8px 14px;
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

.empty-data {
  text-align: center;
  padding: 20px 8px;
  color: #9ca3af;
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
</style>
