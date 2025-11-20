<template>
  <div class="table-card">
    <table class="movements-table">
      <thead>
        <tr>
          <th>출고 번호</th>
          <th>매장 이름</th>
          <th>품목 수</th>
          <th>총 수량</th>
          <th>출고일</th>
        </tr>
      </thead>

      <tbody>
        <tr 
          v-for="row in items" 
          :key="row.outboundId"
          class="row-clickable"
          @click="$emit('open-modal', { id: row.outboundId, no: row.outboundNo })"
        >
          <td>{{ row.outboundNo }}</td>
          <td>{{ row.customerName }}</td>
          <td>{{ row.itemCount }}</td>
          <td class="numeric">{{ row.totalShippedQty }}</td>
          <td>{{ formatDate(row.outboundDatetime)  }}</td>
        </tr>

        <tr v-if="items.length === 0">
          <td colspan="5" style="text-align: center; color: #aaa;">데이터가 없습니다.</td>
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
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})

const formatDate = (dt) => {
  if (!dt) return '-'
  return dt.slice(0, 10)
}
</script>

<style scoped>
.table-card {
  background: #fff;
  border: 1px solid #e9eef6;
  padding: 0;
  border-radius: 8px;
  overflow: hidden;
}
.movements-table {
  width: 100%;
  border-collapse: collapse;
}
.movements-table thead th {
  background: #f3f4f6;
  text-align: left;
  padding: 16px;
}
.movements-table tbody td {
  padding: 18px 16px;
  border-top: 1px solid #f3f4f6;
  text-align: left;
}
.numeric {
  text-align: right;
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
.row-clickable {
  cursor: pointer;
}
.row-clickable:hover {
  background-color: #f5f8ff;
}
</style>
