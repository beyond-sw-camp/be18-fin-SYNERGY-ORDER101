<template>
  <div class="supplier-list">
    <h2 class="page-title">공급사 조회</h2>

    <div class="controls card">
      <!-- 검색어 → keyword 상태와 바인딩 -->
      <input
        v-model="keyword"
        class="search"
        placeholder="공급사 이름 검색..."
        @keyup.enter="handleSearch"
      />
      <button class="search-btn" @click="handleSearch">검색</button>
    </div>

    <div class="list-card card">
      <h3 class="card-title">공급사</h3>

      <table class="supplier-table">
        <thead>
          <tr>
            <th>공급사 ID</th>
            <th>공급사 이름</th>
            <th>주소</th>
            <th>담당자</th>
            <th>연락처</th>
            <th>등록일</th>
          </tr>
        </thead>

        <tbody>
          <!-- 로딩 상태 -->
          <tr v-if="isLoading">
            <td colspan="8" class="empty-data">목록을 불러오는 중...</td>
          </tr>

          <!-- 데이터 없을 때 -->
          <tr v-else-if="suppliers.length === 0">
            <td colspan="8" class="empty-data">등록된 공급사가 없습니다.</td>
          </tr>

          <!-- 실제 데이터 렌더링 : SupplierListRes 기준 필드명 사용 -->
          <tr
            v-else
            v-for="s in suppliers"
            :key="s.supplierId"
            @click="goDetail(s.supplierId)"
            class="clickable"
          >
            <td>{{ s.supplierCode }}</td>
            <td>{{ s.supplierName }}</td>
            <td>{{ s.address }}</td>
            <td>{{ s.contactName }}</td>
            <td>{{ s.contactNumber }}</td>
            <td>{{ formatDate(s.createdDate) }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 간단 페이지네이션 예시 -->
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
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getSupplierList } from '@/components/api/supplier/supplierService'

const router = useRouter()

const MAX_VISIBLE_PAGES = 5

// ─ 상태 ─
const suppliers = ref([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const isLoading = ref(false)

// 총 페이지 수
const totalPages = computed(() =>
  totalCount.value > 0 ? Math.ceil(totalCount.value / pageSize.value) : 1,
)

// 날짜 포맷 (createdDate → yyyy-MM-dd 정도로 잘라서 표시)
const formatDate = (iso) => {
  if (!iso) return ''
  return iso.toString().slice(0, 10)
}

// API 호출
const fetchSuppliers = async (page = 1) => {
  isLoading.value = true
  try {
    const {
      suppliers: list,
      totalCount: total,
      currentPage: cp,
    } = await getSupplierList(page, pageSize.value, keyword.value)

    suppliers.value = list
    totalCount.value = total
    currentPage.value = cp
  } catch (e) {
    console.error(e)
    alert('공급사 목록 조회에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)

  let start = Math.max(1, current - half)
  let end = Math.min(total, start + MAX_VISIBLE_PAGES - 1)

  // 끝에서 밀렸을 때 다시 보정
  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(1, end - MAX_VISIBLE_PAGES + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 검색 버튼 / 엔터
const handleSearch = () => {
  fetchSuppliers(1)
}

// 페이지 이동
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  fetchSuppliers(page)
}

// 상세 페이지 이동 (SupplierListRes 기준으로 supplierId 사용)
const goDetail = (supplierId) => {
  router.push({ name: 'hq-supplier-detail', params: { id: supplierId } })
}

// 최초 진입 시 1페이지 조회
onMounted(() => {
  fetchSuppliers(1)
})
</script>

<style scoped>
.page-title {
  font-size: 20px;
  margin-bottom: 12px;
}
.controls {
  display: flex;
  gap: 12px;
  padding: 14px;
  margin-bottom: 18px;
}
.card {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}
.list-card {
  padding: 20px;
}
.supplier-table {
  width: 100%;
  border-collapse: collapse;
}
.supplier-table thead th {
  text-align: left;
  padding: 14px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
}
.supplier-table tbody td {
  padding: 14px;
  border-top: 1px solid #f7f7f9;
}
.clickable {
  cursor: pointer;
}
.clickable:hover {
  background: #f9fafb;
}
.more {
  background: transparent;
  border: none;
  cursor: pointer;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 16px;
  font-size: 14px;
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

.page-btn.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
  font-weight: 600;
}

.page-btn:disabled {
  background: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
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
