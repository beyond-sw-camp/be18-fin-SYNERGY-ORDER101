<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>일일 정산 목록</h1>
      <button class="pdf-btn" @click="exportPdf">📄 PDF 내보내기</button>
    </header>

    <section class="card filters">
      <h3>필터 옵션</h3>
      <p class="hint">날짜, 매장, 창고별로 정산 내역을 필터링합니다.</p>

      <div class="filter-row">
        <label>
          기간 시작
          <input type="date" v-model="filters.start" />
        </label>
        <label>
          기간 종료
          <input type="date" v-model="filters.end" />
        </label>

        <label>
          매장
          <select v-model="filters.store">
            <option value="전체">전체</option>
            <option value="매장A">매장A</option>
            <option value="매장B">매장B</option>
          </select>
        </label>

        <label>
          창고
          <select v-model="filters.warehouse">
            <option value="전체">전체</option>
            <option value="본사창고">본사창고</option>
            <option value="수도권창고">수도권창고</option>
          </select>
        </label>

        <div class="search-group">
          <input class="search-input" placeholder="검색" v-model="filters.q" />
          <button class="search-btn" @click="onSearch">검색</button>
        </div>

        <div class="sort-group">
          <label>
            정렬
            <select v-model="sortBy">
              <option value="date">정산 날짜</option>
              <option value="totalSupply">총 공급 금액</option>
              <option value="deliverQty">배송 수량</option>
            </select>
          </label>
          <button class="sort-toggle" @click="toggleSortDir">{{ sortDirLabel }}</button>
        </div>
      </div>
    </section>

    <section class="card data">
      <h3>정산 데이터</h3>
      <p class="hint">날짜별, 매장/창고별 정산 내역 목록입니다.</p>

      <div class="table-wrap">
        <table class="settlement-table">
          <thead>
            <tr>
              <th>정산 ID</th>
              <th @click="setSort('date')" class="sortable">
                정산 날짜 <span v-if="sortBy === 'date'">{{ sortDirSymbol }}</span>
              </th>
              <th @click="setSort('deliverQty')" class="sortable">
                배송 수량 <span v-if="sortBy === 'deliverQty'">{{ sortDirSymbol }}</span>
              </th>
              <th @click="setSort('totalSupply')" class="sortable">
                총 공급 금액 <span v-if="sortBy === 'totalSupply'">{{ sortDirSymbol }}</span>
              </th>
              <th>공급 수량</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in sortedRows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ formatDate(row.date) }}</td>
              <td class="numeric">{{ row.deliverQty }}</td>
              <td class="numeric">{{ formatWon(row.totalSupply) }}</td>
              <td class="numeric">{{ row.supplyQty }}</td>
              <td>
                <span :class="['status', statusClass(row.status)]">{{ row.status }}</span>
              </td>
            </tr>
            <tr v-if="sortedRows.length === 0">
              <td colspan="6" class="no-data">조건에 맞는 정산 내역이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const filters = reactive({
  start: '',
  end: '',
  store: '전체',
  warehouse: '전체',
  q: '',
})

const sortBy = ref('date')
const sortDir = ref('desc') // 'asc' | 'desc'

function toggleSortDir() {
  sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
}
function setSort(field) {
  if (sortBy.value === field) toggleSortDir()
  else sortBy.value = field
}

const rows = ref([
  {
    id: 'SETL-20240726-001',
    date: '2024-07-26',
    deliverQty: 250,
    totalSupply: 1000000,
    supplyQty: 200,
    status: '완료',
  },
  {
    id: 'SETL-20240726-002',
    date: '2024-07-26',
    deliverQty: 180,
    totalSupply: 650000,
    supplyQty: 150,
    status: '진행 중',
  },
  {
    id: 'SETL-20240725-003',
    date: '2024-07-25',
    deliverQty: 300,
    totalSupply: 1200000,
    supplyQty: 280,
    status: '오류',
  },
  {
    id: 'SETL-20240725-004',
    date: '2024-07-25',
    deliverQty: 100,
    totalSupply: 400000,
    supplyQty: 80,
    status: '대기 중',
  },
  {
    id: 'SETL-20240724-005',
    date: '2024-07-24',
    deliverQty: 220,
    totalSupply: 780000,
    supplyQty: 190,
    status: '완료',
  },
])

function formatWon(value) {
  if (value == null) return '-'
  return new Intl.NumberFormat('ko-KR').format(value) + '원'
}
function formatDate(d) {
  if (!d) return '-'
  const dt = new Date(d)
  return (
    dt.getFullYear() +
    '년 ' +
    String(dt.getMonth() + 1).padStart(2, '0') +
    '월 ' +
    String(dt.getDate()).padStart(2, '0') +
    '일'
  )
}

const filteredRows = computed(() => {
  return rows.value.filter((r) => {
    // date range
    if (filters.start) {
      if (new Date(r.date) < new Date(filters.start)) return false
    }
    if (filters.end) {
      if (new Date(r.date) > new Date(filters.end)) return false
    }
    // store & warehouse (currently sample rows don't include store/warehouse fields)
    if (filters.q) {
      const q = filters.q.toLowerCase()
      if (!r.id.toLowerCase().includes(q)) return false
    }
    return true
  })
})

const sortedRows = computed(() => {
  const arr = filteredRows.value.slice()
  const dir = sortDir.value === 'asc' ? 1 : -1
  arr.sort((a, b) => {
    if (sortBy.value === 'date') {
      return dir * (new Date(a.date) - new Date(b.date))
    }
    if (sortBy.value === 'totalSupply') {
      return dir * ((a.totalSupply || 0) - (b.totalSupply || 0))
    }
    if (sortBy.value === 'deliverQty') {
      return dir * ((a.deliverQty || 0) - (b.deliverQty || 0))
    }
    return 0
  })
  return arr
})

function onSearch() {
  // computed filters already react; this method exists to match the UI's explicit "검색" button
}

function exportPdf() {
  // simple placeholder: trigger print; in a real app you'd generate a PDF server-side or use a client PDF lib
  window.print()
}

function statusClass(s) {
  if (!s) return ''
  if (s.includes('완료')) return 's-complete'
  if (s.includes('진행')) return 's-progress'
  if (s.includes('오류')) return 's-error'
  if (s.includes('대기')) return 's-pending'
  return ''
}

const sortDirLabel = computed(() => (sortDir.value === 'asc' ? '오름차순' : '내림차순'))
const sortDirSymbol = computed(() => (sortDir.value === 'asc' ? '▲' : '▼'))
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.pdf-btn {
  border: 1px solid #e6e6e9;
  background: white;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 20px;
}

.filters .filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filters label {
  display: flex;
  flex-direction: column;
  font-size: 13px;
  color: #666;
}

.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
}

.search-btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: none;
  background: #6b7280;
  color: white;
}

.sort-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.sort-toggle {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
  background: white;
}

.table-wrap {
  margin-top: 12px;
}

.settlement-table {
  width: 100%;
  border-collapse: collapse;
}

.settlement-table th,
.settlement-table td {
  padding: 18px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f3;
}

.settlement-table th.sortable {
  cursor: pointer;
}

.settlement-table td.numeric {
  text-align: right;
}

.status {
  padding: 6px 10px;
  border-radius: 12px;
  font-size: 13px;
  color: #fff;
}

.s-complete {
  background: #2f855a;
}

.s-progress {
  background: #db2777;
}

.s-error {
  background: #ef4444;
}

.s-pending {
  background: #f97316;
}

.no-data {
  text-align: center;
  padding: 26px;
  color: #999;
}

.hint {
  color: #777;
  margin-top: 6px;
  margin-bottom: 12px;
}
</style>
