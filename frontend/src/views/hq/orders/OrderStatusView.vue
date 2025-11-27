<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>발주 관리</h1>
    </header>

    <section class="filters card">
      <PurchaseFilter @search="handleSearch" />
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="orders-table">
          <thead>
            <tr>
              <th>PO 번호</th>
              <th>요청자</th>
              <th>공급업체</th>
              <th class="center">품목 수</th>
              <th class="center">금액</th>
              <th class="center">요청일</th>
              <th>상태</th>
              <th class="center">타입</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="clickable-row" @click="openApproval(row)">
              <td class="po">{{ row.No }}</td>
              <td>{{ row.requester }}</td>
              <td>{{ row.vendor }}</td>
              <td class="center">{{ row.items }}</td>
              <td class="center">
                <Money :value="row.amount"></Money>
              </td>
              <td class="center">{{ formatDateTimeMinute(row.requestedAt) }}</td>
              <td>
                <span :class="['chip', statusClass(row.status)]">{{ row.status }}</span>
              </td>
              <td class="center">{{ row.orderType }}</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="8" class="no-data">검색 조건에 맞는 발주가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="page-nav" @click="goPage(1)" :disabled="page === 1">
          &laquo;
        </button>
        <button class="page-nav" @click="goPage(page - 1)" :disabled="page === 1">
          &lsaquo;
        </button>

        <div class="pages">
          <button v-for="p in visiblePages" :key="p" :class="{ active: p === page }" @click="goPage(p)">
            {{ p }}
          </button>
        </div>

        <button class="page-nav" @click="goPage(page + 1)" :disabled="page === totalPages">
          &rsaquo;
        </button>
        <button class="page-nav" @click="goPage(totalPages)" :disabled="page === totalPages">
          &raquo;
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRegularPurchases, mapPurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute, getPastDateString } from '@/components/global/Date';
import PurchaseFilter from '@/components/domain/order/PurchaseFilter.vue';

const filters = ref({
  status: 'ALL',
  vendorId: null,
  startDate: getPastDateString(30),
  endDate: new Date().toISOString().slice(0, 10),
  keyword: ''
})

const page = ref(1)
const perPage = ref(10)
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

// 시작시
onMounted(() => {
  search();
});

// 필터 검색 이벤트 핸들러
function handleSearch(filterData) {
  console.log('🔍 발주 필터 검색:', filterData)
  filters.value = {
    status: filterData.status !== 'ALL' ? filterData.status : null,
    vendorId: filterData.vendorId !== 'ALL' ? filterData.vendorId : null,
    startDate: filterData.startDate,
    endDate: filterData.endDate,
    keyword: filterData.keyword
  }
  page.value = 1
  search()
}

async function search() {
  loading.value = true
  error.value = null

  const apiPage = page.value - 1; // 0-based index로 변환

  try {
    console.log("검색 조건:", filters.value);

    // 일반 발주 검색 조건 생성
    const regularCond = {
      types: [],
      statuses: filters.value.status ? [filters.value.status] : [],
      vendorId: filters.value.vendorId || null,
      searchText: filters.value.keyword || null,
      fromDate: filters.value.startDate || null,
      toDate: filters.value.endDate || null
    };

    const data = await getRegularPurchases(
      regularCond,
      apiPage,
      perPage.value
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
      status: mapPurchaseStatus(item.status),
      orderType: mapPurchaseStatus(item.orderType)
    }));

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
  if (s === '승인') return 's-accepted'
  if (s === '제출' || s === '대기') return 's-waiting'
  if (s === '반려') return 's-rejected'
  if (s === '취소') return 's-rejected'
  if (s === '초안') return 's-waiting'
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

.orders-table th.center,
.orders-table td.center {
  text-align: center;
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
  gap: 8px;
  padding-top: 20px;
  margin-top: 16px;
}

.page-nav {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.page-nav:hover:not(:disabled) {
  background: #f3f4f6;
}

.page-nav:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pages {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.pages button {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e6e6e9;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 36px;
}

.pages button:hover:not(.active) {
  background: #f3f4f6;
}

.pages button.active {
  background: #111827;
  color: white;
}

.clickable-row {
  cursor: pointer;
}
</style>
