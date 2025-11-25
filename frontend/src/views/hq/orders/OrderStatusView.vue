<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 관리</h1>
    </header>

    <section class="card controls">
      <div class="controls-row">
        <input class="search" v-model="filters.q" placeholder="PO 번호, 공급업체 등으로 검색..." />

        <div class="controls-right">
          <OrderStatusSelect v-model="filters.status" />
          <button class="btn" @click="search">조회</button>
        </div>
      </div>
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="orders-table">
          <thead>
            <tr>
              <th>PO 번호</th>
              <th>요청자</th>
              <th>공급업체</th>
              <th>품목 수</th>
              <th class="numeric">금액</th>
              <th>요청일</th>
              <th>상태</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="clickable-row" @click="openApproval(row)">
              <td class="po">{{ row.No }}</td>
              <td>{{ row.requester }}</td>
              <td>{{ row.vendor }}</td>
              <td class="numeric">{{ row.items }}</td>
              <td class="numeric">
                <Money :value="row.amount"></Money>
              </td>
              <td>{{ formatDateTimeMinute(row.requestedAt) }}</td>
              <td>
                <span :class="['chip', statusClass(row.status)]">{{ row.status }}</span>
              </td>
              <td class="actions">⋯</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="8" class="no-data">검색 조건에 맞는 발주가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <div class="pages">
          <button v-for="p in totalPages" :key="p" :class="{ active: p === page }" @click="goPage(p)">
            {{ p }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPurchases } from '@/components/api/purchase/purchaseService.js'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date';
import OrderStatusSelect from '@/components/OrderStatusSelect.vue';

const filters = ref({ q: '', status: '전체' })

const page = ref(1)
const perPage = ref(7)
const rows = ref([])
const totalElements = ref(0) // 전체 항목 수
const totalPagesFromBackend = ref(0) // 백엔드에서 받은 총 페이지 수
const loading = ref(false)
const error = ref(null)

const router = useRouter()

// 백엔드 페이지네이션 정보 기반으로 총 페이지 수 계산
const totalPages = computed(() => totalPagesFromBackend.value || 1)

// 현재 페이지의 데이터만 표시 (백엔드에서 이미 페이징된 데이터)
const filteredRowsPaginated = computed(() => rows.value)

// 시작시
onMounted(() => {
  search();
});

async function search() {
  loading.value = true
  error.value = null

  const apiPage = page.value - 1; // 0-based index로 변환

  try {
    const data = await getPurchases(
      apiPage,
      perPage.value,
      filters.value.q,
      filters.value.status === '전체' ? null : filters.value.status
    );


    console.log("API 응답 데이터:", data);

    // 백엔드 페이지네이션 정보 저장
    totalElements.value = data.totalElements || 0
    totalPagesFromBackend.value = data.totalPages || 1

    // 데이터 매핑
    rows.value = (data.content || []).map(item => ({
      id: item.purchaseId,
      No: item.poNo,
      vendor: item.supplierName,
      requester: item.requesterName,
      items: item.totalQty,
      amount: item.totalAmount,
      requestedAt: item.requestedAt,
      status: item.status
    }));

    console.log("데이터 할당 완료:", rows.value);

  } catch (err) {
    console.error('발주 목록을 가져오는 중 오류 발생:', err);
    error.value = err.message || '발주 목록을 불러올 수 없습니다.';
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  page.value = p
  search() // 페이지 변경 시 다시 검색
}

function openApproval(row) {
  router.push({ name: 'hq-orders-approval-detail', params: { id: row.id } })
}

function statusClass(s) {
  if (!s) return ''
  if (s.includes('CONFIRMED')) return 's-accepted'
  if (s.includes('SUBMITTED')) return 's-waiting'
  if (s.includes('REJECTED')) return 's-rejected'
  return ''
}
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

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.card {
  background: #fff;
  border: 1px solid #f0f0f3;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search {
  flex: 1;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e6e6e9;
}

.controls-right {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
}

.table-wrap {
  margin-top: 12px;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
}

.orders-table th,
.orders-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
}

.orders-table td.numeric {
  text-align: right;
}

.po {
  font-weight: 600;
}

.chip {
  padding: 6px 10px;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
}

.s-accepted {
  background: #16a34a;
}

.s-waiting {
  background: #d97706;
}

.s-rejected {
  background: #ef4444;
}

.actions {
  text-align: center;
}

.no-data {
  text-align: center;
  padding: 26px;
  color: #999;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 12px;
}

.pages {
  display: flex;
  gap: 8px;
}

.pages button {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
}

.pages button.active {
  background: #111827;
  color: white;
}

.clickable-row {
  cursor: pointer;
}
</style>
