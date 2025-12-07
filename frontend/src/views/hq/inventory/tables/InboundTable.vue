<template>
  <div class="table-card">
    <table class="movements-table">
      <thead>
        <tr>
          <th>입고 번호</th>
          <th>공급사 이름</th>
          <th>품목 수</th>
          <th>총 수량</th>
          <th>입고일</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in items" :key="row.inboundId" class="row-clickable"
          @click="$emit('open-modal', { id: row.inboundId, no: row.inboundNo })">
          <td>{{ row.inboundNo }}</td>
          <td>{{ row.supplierName }}</td>
          <td>{{ row.itemCount }}</td>
          <td class="numeric">{{ row.totalReceivedQty }}</td>
          <td>{{ formatDate(row.inboundDatetime) }}</td>
        </tr>

        <tr v-if="items.length === 0">
          <td colspan="5" style="text-align: center; color: #aaa;">데이터가 없습니다.</td>
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
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { 
    type: Array, 
    default: () => [] 
  },
  page: { 
    type: Number, 
    default: 1 
  },
  totalPages: { 
    type: Number,
    default: 1 
  }
})

const emit = defineEmits(["change-page"])

const changePage = (p) => {
  if (p < 1 || p > props.totalPages) return
  emit("change-page", p)
}

const formatDate = (dt) => {
  if (!dt) return "-"
  return dt.slice(0, 10)
}

// 표시할 페이지 번호 계산 (최대 5개)
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page
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
.row-clickable {
  cursor: pointer;
}
.row-clickable:hover {
  background: #f5f8ff;
}
</style>
