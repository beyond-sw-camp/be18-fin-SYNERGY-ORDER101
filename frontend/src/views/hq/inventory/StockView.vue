<template>
  <div class="stock-view">
    <h2 class="page-title">창고 재고 상태</h2>

    <div class="filter-card">
      <div class="filter-row">
        <select class="select" v-model="largeCategoryId">
          <option value="">대분류</option>
          <option v-for="c in largeCategories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>

        <select class="select" v-model="mediumCategoryId" :disabled="mediumCategories.length === 0">
          <option value="">중분류</option>
          <option v-for="c in mediumCategories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>

        <select class="select" v-model="smallCategoryId" :disabled="!smallCategories.length">
          <option value="">소분류</option>
          <option v-for="c in smallCategories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>
      </div>
    </div>

    <div class="table-card">
      <h3 class="card-title">현재 재고 개요</h3>
      <table class="stock-table">
        <thead>
          <tr>
            <th>상품 코드</th>
            <th>카테고리</th>
            <th>상품명</th>
            <th>현 재고량</th>
            <th>안전재고량</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in inventoryStore.items" :key="r.warehouseInventoryId">
            <td>{{ r.productCode }}</td>
            <td>{{ r.productCategory }}</td>
            <td>{{ r.productName }}</td>
            <td class="numeric">{{ r.onHandQty }}</td>
            <td class="numeric">{{ r.safetyQty }}</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="page-btn" @click="changePage(page - 1)" :disabled="page <= 1">
          ‹ Previous
        </button>

        <div class="page-numbers">
          <button v-for="p in visiblePages" :key="p" class="page"
            :class="['page-button', { active: page === p }]" @click="changePage(p)">
            {{ p }}
          </button>
        </div>

        <button class="page-btn" @click="changePage(page + 1)" :disabled="page >= totalPages">
          Next ›
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed ,onMounted, watch } from 'vue'
import { useInventoryStore } from '@/stores/inventory/inventoryStore'
import { getTopCategories, getChildCategories } from '@/components/api/product/categoryService'

const inventoryStore = useInventoryStore()

const largeCategories = ref([])
const mediumCategories = ref([])
const smallCategories = ref([])

const largeCategoryId = ref('')
const mediumCategoryId = ref('')
const smallCategoryId = ref('')

const page = computed(() => inventoryStore.page)
const totalPages = computed(() => inventoryStore.totalPages)

const pages = computed(() =>
  Array.from({ length: totalPages.value }, (_, i) => i + 1)
)

// 표시할 페이지 번호 계산 (최대 5개)
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const delta = 2 // 현재 페이지 양옆으로 보여줄 페이지 수
  const pages = []

  if (total <= 5) {
    // 전체 페이지가 5개 이하면 모두 표시
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 5개보다 많으면 현재 페이지 기준으로 표시
    let start = Math.max(1, current - delta)
    let end = Math.min(total, current + delta)

    // 시작이 1이면 끝을 늘림
    if (start === 1) {
      end = Math.min(5, total)
    }
    // 끝이 마지막이면 시작을 줄임
    if (end === total) {
      start = Math.max(1, total - 4)
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
  }

  return pages
})

onMounted(async () => {
  largeCategories.value = await getTopCategories()
  await fetchInventory(1)
})

const fetchInventory = async (page = 1) => {
  await inventoryStore.fetchInventory({
    page,
    largeId: largeCategoryId.value || null,
    mediumId: mediumCategoryId.value || null,
    smallId: smallCategoryId.value || null
  })
}

// 카테고리 필터 감지
watch(largeCategoryId, async (newVal) => {
  mediumCategoryId.value = ''
  smallCategoryId.value = ''
  mediumCategories.value = []
  smallCategories.value = []
  if (newVal) mediumCategories.value = await getChildCategories(Number(newVal))
  fetchInventory(1)
})

watch(mediumCategoryId, async (newVal) => {
  smallCategoryId.value = ''
  smallCategories.value = []
  if (newVal) smallCategories.value = await getChildCategories(Number(newVal))
  fetchInventory(1)
})

watch(smallCategoryId, () => fetchInventory(1))

const changePage = async (p) => {
  if (p < 1 || p > totalPages.value) return
  fetchInventory(p)
}
</script>

<style scoped>
.stock-view {
  padding: 16px;
}
.stock-image-wrap {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stock-image {
  max-width: 100%;
  height: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.caption {
  margin-top: 8px;
  color: #6b7280;
  font-size: 14px;
}
.note {
  margin-top: 16px;
  color: #374151;
}
.page-title {
  font-size: 20px;
  margin: 8px 0 16px;
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
}
.select {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
}
.table-card {
  background: #fff;
  border: 1px solid #e9eef6;
  padding: 16px;
  border-radius: 8px;
}
.card-title {
  font-size: 16px;
  margin-bottom: 12px;
}
.stock-table {
  width: 100%;
  border-collapse: collapse;
}
.stock-table thead th {
  background: #f3f4f6;
  text-align: left;
  padding: 14px;
  border-bottom: 1px solid #eef2f7;
}
.stock-table tbody td {
  padding: 18px 14px;
  border-top: 1px solid #f7f7f9;
}
.numeric {
  text-align: left;
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
.page.active {
  border-color: #2563eb;
  color: #2563eb;
  font-weight: 600;
  background: #eff6ff;
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
