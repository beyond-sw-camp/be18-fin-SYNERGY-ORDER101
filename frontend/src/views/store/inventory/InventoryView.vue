<script setup>
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/components/api'
import { useAuthStore } from '@/stores/authStore'

const categories = ref(['대분류', '중분류', '소분류', '상품별'])
const selectedCategory = ref('대분류')
const search = ref('')

// backend items will be normalized into this shape: { id, code, name, stock, incoming, safetyQty, updatedAt }
const items = ref([])
const loading = ref(false)
const error = ref('')

const fetchInventory = async () => {
  loading.value = true
  error.value = ''
  try {
    // determine storeId from auth store (or fallback to localStorage)
    const auth = useAuthStore()
    const storeId =
      auth.userInfo && auth.userInfo.storeId
        ? auth.userInfo.storeId
        : localStorage.getItem('storeId')
    if (!storeId) {
      throw new Error('storeId not available')
    }
    // user-provided endpoint; apiClient has baseURL configured
    const res = await apiClient.get(`/api/v1/stores/${storeId}/inventory`)
    const arr = (res && res.data && res.data.items) || []
    items.value = arr.map((it) => ({
      id: it.storeInventoryId,
      code: it.productCode,
      name: it.productName,
      stock: it.onHandQty ?? 0,
      incoming: it.inTransitQty ?? 0,
      safetyQty: it.safetyQty ?? null,
      updatedAt: it.updatedAt || '',
    }))
  } catch (e) {
    console.error('fetch inventory failed', e)
    error.value = (e && e.message) || '데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchInventory()
})
</script>

<template>
  <div class="store-inventory">
    <header class="page-header">
      <h1>창고 재고 상태</h1>
    </header>

    <section class="overview-card">
      <h2 class="overview-title">현재 재고 개요</h2>
      <div class="table-wrap">
        <table class="inventory-table">
          <thead>
            <tr>
              <th>코드</th>
              <th>품목명</th>
              <th>재고 수량</th>
              <th>입고 예정 수량</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="4">불러오는 중...</td>
            </tr>
            <tr v-else-if="error">
              <td colspan="4">{{ error }}</td>
            </tr>
            <tr v-else v-for="item in items" :key="item.id">
              <td>{{ item.code }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.stock }}</td>
              <td>{{ item.incoming }}</td>
            </tr>
            <tr v-if="!loading && !error && items.length === 0">
              <td colspan="4">데이터가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
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
}
.inventory-table thead th {
  text-align: left;
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
