<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>주문 목록</h1>
    </header>

    <section class="filters card">
      <FranchiseFilter @search="handleSearch"></FranchiseFilter>
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="orders-table">
          <thead>
            <tr>
              <th>주문 ID</th>
              <th>가맹점</th>
              <th>품목 수</th>
              <th>총 수량</th>
              <th class="numeric">예상 가격</th>
              <th>생성 시간</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id" class="clickable-row" @click="openDetail(row)">
              <td class="po">{{ row.No }}</td>
              <td>{{ row.store }}</td>
              <td class="numeric">{{ row.itemCount }}</td>
              <td class="numeric">{{ row.totalQty }}</td>
              <td class="numeric">
                <Money :value="row.totalPrice" />
              </td>
              <td>{{ row.createdAt }}</td>
              <td>
                <span :class="['chip', statusClass(row.status)]">{{
                  statusLabel(row.status)
                }}</span>
              </td>
            </tr>
            <tr v-if="filteredRows.length === 0">
              <td colspan="7" class="no-data">검색 조건에 맞는 주문이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <div class="pages">
          <button v-for="p in totalPages" :key="p" :class="{ active: p === currentPage }" @click="goPage(p)">
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
import Money from '@/components/global/Money.vue'
import FranchiseFilter from '@/components/domain/franchise/filter/FranchiseFilter.vue'
import { getFranchiseOrderList } from '@/components/api/store/StoreService.js'
import { mapPurchaseStatus } from '@/components/api/purchase/purchaseService.js'
import { formatDateTimeMinute, getPastDateString, getTodayString } from '@/components/global/Date.js'

const router = useRouter()

// 페이지네이션
const currentPage = ref(1)
const perPage = ref(10)
const totalElements = ref(0)
const totalPagesFromBackend = ref(0)

// 필터 상태
const filters = ref({
  storeId: '',
  startDate: getPastDateString(30),
  endDate: getTodayString(),
  keyword: '',
  statuses: null // 전체 상태 조회 (null)
})

// 데이터
const rows = ref([])

// 총 페이지 수
const totalPages = computed(() => totalPagesFromBackend.value || 1)

// 필터된 행 (API에서 이미 필터링된 데이터를 받으므로 그대로 사용)
const filteredRows = computed(() => rows.value)

onMounted(() => {
  searchStoreOrders()
})

// 필터 검색 이벤트 핸들러
function handleSearch(filterData) {
  console.log('🔍 필터 검색:', filterData)
  filters.value = {
    storeId: filterData.vendorId || null,
    startDate: filterData.startDate,
    endDate: filterData.endDate,
    keyword: filterData.keyword,
    statuses: filterData.scope !== 'ALL' ? filterData.scope : null // scope를 status로 사용
  }
  currentPage.value = 1
  searchStoreOrders()
}

// API 조회
const searchStoreOrders = async () => {
  try {
    const params = {
      storeId: filters.value.storeId || null,
      fromDate: filters.value.startDate || null,
      toDate: filters.value.endDate || null,
      statuses: filters.value.statuses, // 전체 상태 조회
      searchText: filters.value.keyword || null
    };

    console.log('📤 요청 파라미터:', params);

    const pageData = await getFranchiseOrderList(
      currentPage.value,
      perPage.value,
      params
    );

    console.log('📦 API 응답:', pageData);

    totalElements.value = pageData.totalElements || 0;
    totalPagesFromBackend.value = pageData.totalPages || 1;

    rows.value = (pageData.content || []).map(item => ({
      id: item.storeOrderId,
      No: item.orderNo,
      store: item.storeName,
      itemCount: item.itemCount || 0,
      totalQty: item.totalQTY || 0,
      totalPrice: item.totalAmount || 0,
      createdAt: formatDateTimeMinute(item.orderDate || item.createdAt),
      status: mapPurchaseStatus(item.orderStatus || item.status)
    }));

    console.log('✅ 변환된 데이터:', rows.value);

  } catch (error) {
    console.error('❌ 데이터 로드 실패:', error);

    let errorMessage = '데이터를 불러오는 중 오류가 발생했습니다.';

    if (error.response) {
      errorMessage = `서버 오류 (${error.response.status}): ${error.response.data?.message || '알 수 없는 오류'}`;
    } else if (error.request) {
      errorMessage = '서버와 연결할 수 없습니다. 네트워크 상태를 확인해주세요.';
    }

    alert(errorMessage);
    rows.value = [];
  }
}

// 페이지 이동
function goPage(p) {
  currentPage.value = p
  searchStoreOrders()
}

function openDetail(row) {
  router.push({ name: 'hq-franchise-order-detail', params: { id: row.id } })
}

function statusClass(s) {
  if (!s) return ''
  if (s === '승인') return 's-accepted'
  if (s === '제출' || s === '대기' || s === '초안') return 's-waiting'
  if (s === '반려' || s === '취소') return 's-rejected'
  return ''
}

function statusLabel(s) {
  return s // mapPurchaseStatus에서 이미 한글로 변환됨
}
</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  margin-bottom: 18px;
}

.filters-row {
  display: flex;
  gap: 12px;
  align-items: center;
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

.clickable-row {
  cursor: pointer;
}

.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.pages {
  display: flex;
  gap: 8px;
}

.pages button {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s;
}

.pages button:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.pages button.active {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border-color: transparent;
  font-weight: 600;
}
</style>
