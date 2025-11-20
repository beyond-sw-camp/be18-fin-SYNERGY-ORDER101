<template>
  <div class="stock-view">
    <h2 class="page-title">창고 재고 상태</h2>

    <div class="filter-card">
      <div class="filter-row">
        <select class="select">
          <option>카테고리 분류</option>
          <option>대분류</option>
          <option>중분류</option>
          <option>소분류</option>
        </select>
        <select class="select">
          <option>카테고리 명</option>
          <option>분류에 따라 달라짐</option>
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
        <button class="page-btn">‹ Previous</button>
        <div class="page-numbers">
          <button class="page current">1</button>
          <button class="page">2</button>
        </div>
        <button class="page-btn">Next ›</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useInventoryStore } from '@/stores/inventory/inventory'

const inventoryStore = useInventoryStore()

// const page = ref(1)
// const numOfRows = ref(20)
// const totalCount = ref(0)
// const parentId = ref(1)
const page = 1
const numOfRows = 20
const totalCount = 0
const parentId = 1

onMounted(() => {
  inventoryStore.fetchInventory({ page, numOfRows, totalCount })
  // inventoryStore.loadCategories({ parentId })
})

function refreshData() {
  inventoryStore.fetchInventory({ page, numOfRows, totalCount })
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
