<script setup>
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/components/api'
import { useAuthStore } from '@/stores/authStore'

const items = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const numOfRows = ref(20)
const totalCount = ref(0)

const totalPages = computed(() => Math.ceil(totalCount.value / numOfRows.value))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const delta = 2
  const pages = []

  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    let start = Math.max(1, current - delta)
    let end = Math.min(total, current + delta)

    if (start === 1) end = Math.min(5, total)
    if (end === total) start = Math.max(1, total - 4)

    for (let i = start; i <= end; i++) pages.push(i)
  }

  return pages
})

const fetchInventory = async () => {
  loading.value = true
  try {
    const auth = useAuthStore()
    const storeId =
      auth.userInfo?.storeId ||
      JSON.parse(localStorage.getItem('userInfo') || '{}').storeId

    const res = await apiClient.get(`/api/v1/stores/${storeId}/inventory`, {
      params: {
        page: page.value,
        numOfRows: numOfRows.value,
      },
    })

    const arr = res?.data?.items || []
    totalCount.value = res?.data?.totalCount ?? 0

    items.value = arr.map((it) => ({
      id: it.storeInventoryId,
      code: it.productCode,
      name: it.productName,
      stock: it.onHandQty ?? 0,
      incoming: it.inTransitQty ?? 0,
    }))
  } catch (e) {
    error.value = e.message || '데이터 불러오기 실패'
  } finally {
    loading.value = false
  }
}

const goPage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchInventory()
}

onMounted(() => fetchInventory())
</script>

<template>
  <div class="inventory-page">
    <h1 class="page-title">가맹점 재고 조회</h1>

    <div class="card">
      <h2 class="card-title">현재 재고 개요</h2>

      <div class="table-container">
        <table class="inventory-table">
          <thead>
            <tr>
              <th class="col-code">코드</th>
              <th class="col-name">품목명</th>
              <th class="col-stock">재고 수량</th>
              <th class="col-incoming">입고 예정 수량</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="loading">
              <td colspan="4">불러오는 중...</td>
            </tr>

            <tr v-else-if="error">
              <td colspan="4">{{ error }}</td>
            </tr>

            <tr v-for="item in items" :key="item.id">
              <td class="col-code">{{ item.code }}</td>
              <td class="col-name">{{ item.name }}</td>
              <td class="col-stock">{{ item.stock }}</td>
              <td class="col-incoming">{{ item.incoming }}</td>
            </tr>

            <tr v-if="!loading && !error && items.length === 0">
              <td colspan="4" class="no-data">데이터가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ORDER101 스타일 pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-nav" @click="goPage(1)" :disabled="page === 1">&laquo;</button>
        <button class="page-nav" @click="goPage(page - 1)" :disabled="page === 1">&lsaquo;</button>

        <div class="pages">
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="{ active: p === page }"
            @click="goPage(p)"
          >
            {{ p }}
          </button>
        </div>

        <button class="page-nav" @click="goPage(page + 1)" :disabled="page === totalPages">&rsaquo;</button>
        <button class="page-nav" @click="goPage(totalPages)" :disabled="page === totalPages">&raquo;</button>
      </div>
    </div>
  </div>
</template>



<style scoped>
.store-inventory {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header h1 {
  font-size: 20px;
  margin: 0;
}
.overview-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #eef0f3;
}
.overview-title {
  font-size: 16px;
  margin: 0 0 12px 0;
}
.table-wrap {
  overflow: auto;
}
.inventory-table {
  width: 100%;
  border-collapse: collapse;
  text-align: center;
}
.inventory-table thead th {
  padding: 14px;
  background: #fbfbfd;
  color: #6b7280;
  font-weight: 600;
}
.inventory-table tbody td {
  padding: 18px 14px;
  border-top: 1px solid #f3f4f6;
}
.inventory-table tbody tr:nth-child(odd) td {
  background: #fff;
}
.inventory-table tbody tr:nth-child(even) td {
  background: #fbfbfd;
}
.inventory-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.card-title {
  font-size: 18px;
  margin-bottom: 16px;
}

.table-container {
  overflow-x: auto;
  text-align: center;
}

/* ---- 컬럼 폭/정렬 ---- */
/* .col-code {
  width: 240px;
  white-space: nowrap;
}

.col-name {
  width: 400px;
}

.col-stock,
.col-incoming {
  text-align: right;
  width: 150px;
}

.inventory-table thead th {
  background: #f9fafb;
  color: #6b7280;
  font-weight: 600;
} */

.no-data {
  text-align: center;
  padding: 20px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
}

.page-nav,
.pages button {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
  font-size: 15px;
  min-width: 36px;
}

.pages button.active {
  background: #111827;
  color: white;
}

@media (max-width: 768px) {
  .inventory-table thead {
    display: none;
  }
  .inventory-table tbody td {
    display: block;
    width: 100%;
  }
}
</style>
