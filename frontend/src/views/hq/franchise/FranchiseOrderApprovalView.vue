<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>가맹점 주문 승인 요청 목록</h1>
    </header>

    <section class="filters card">
      <FranchiseFilter :showScope="false" @search="handleSearch" />
    </section>

    <section class="card list">
      <div class="table-wrap">
        <table class="approval-table">
          <thead>
            <tr>
              <th class="center">주문 ID</th>
              <th class="center">가맹점</th>
              <th class="center">품목 수</th>
              <th class="center">총 수량</th>
              <th class="center">예상 가격</th>
              <th class="center">생성 시간</th>
              <th class="center">승인</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id" class="clickable-row" @click="openDetail(row)">
              <td class="center po">{{ row.No }}</td>
              <td class="center">{{ row.store }}</td>
              <td class="center">{{ row.itemCount }}</td>
              <td class="center">{{ row.totalQty }}</td>
              <td class="right">
                <Money :value="row.estimatedPrice" />
              </td>
              <td class="center">{{ formatDateTimeMinute(row.createdAt) }}</td>
              <td class="center">
                <div class="actions">
                  <button class="btn-accept" @click.stop="approve(row)">승인</button>
                  <button class="btn-reject" @click.stop="reject(row)">반려</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredRows.length === 0">
              <td colspan="7" class="no-data">승인 요청이 없습니다.</td>
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
  vendorId: '',
  startDate: getPastDateString(30),
  endDate: getTodayString(),
  keyword: '',
  statuses: 'SUBMITTED'
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
    vendorId: filterData.vendorId === null || filterData.vendorId === 'ALL' ? null : filterData.vendorId,
    startDate: filterData.startDate,
    endDate: filterData.endDate,
    keyword: filterData.keyword,
    statuses: 'SUBMITTED' // 승인 요청 목록은 항상 SUBMITTED만 조회
  }
  currentPage.value = 1 // 검색 시 첫 페이지로 이동
  searchStoreOrders()
}

// API 조회
const searchStoreOrders = async () => {
  try {
    // ✅ API 파라미터 구성 (Settlement과 동일한 패턴)
    const params = {
      vendorId: filters.value.vendorId || null,
      fromDate: filters.value.startDate || null,
      toDate: filters.value.endDate || null,
      statuses: 'SUBMITTED',
      searchText: filters.value.keyword || null
    };


    // ✅ 단일 API 호출 (Spring Page 객체 반환)
    const pageData = await getFranchiseOrderList(
      currentPage.value,
      perPage.value,
      params
    );

    totalElements.value = pageData.totalElements || 0;
    totalPagesFromBackend.value = pageData.totalPages || 1;

    // ✅ 테이블 데이터 변환
    rows.value = (pageData.content || []).map(item => ({
      id: item.storeOrderId,
      No: item.orderNo || item.storeOrderId,
      store: item.storeName,
      itemCount: item.itemCount || 0,
      totalQty: item.totalQTY || 0,
      estimatedPrice: item.totalAmount || 0,
      createdAt: item.orderDate || item.createdAt,
      status: mapPurchaseStatus(item.orderStatus || item.status)
    }));

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
  router.push({ name: 'hq-franchise-approval-detail', params: { id: row.id } })
}

async function approve(row) {
  try {
    // 1. 먼저 재고 확인
    const { checkStockForOrder } = await import('@/components/api/store/StoreService.js')
    const stockCheck = await checkStockForOrder(row.id)

    // 2. 재고가 부족한 경우
    if (!stockCheck.hasEnoughStock) {
      const insufficientItems = stockCheck.insufficientItems || []
      const itemList = insufficientItems
        .map(item => `- ${item.productName}: 요청 ${item.requestedQty}개, 재고 ${item.availableQty}개 (부족: ${item.shortageQty}개)`)
        .join('\n')

      const goToPurchase = confirm(
        `창고 재고가 부족합니다.\n\n${itemList}\n\n공급사 발주 페이지로 이동하시겠습니까?`
      )

      if (goToPurchase) {
        // 발주 생성 페이지로 이동
        router.push({ name: 'hq-orders-create' })
      }
      return
    }

    // 3. 재고가 충분한 경우 승인 진행
    if (!confirm(`${row.store} 가맹점의 주문을 승인하시겠습니까?`)) return

    const { updateStoreOrderStatus } = await import('@/components/api/store/StoreService.js')
    await updateStoreOrderStatus(row.id, 'CONFIRMED')

    // ✅ 목록에서 제거 (필터링)
    rows.value = rows.value.filter(r => r.id !== row.id)
    totalElements.value = Math.max(0, totalElements.value - 1)

    alert(`${row.store} 가맹점의 주문이 승인되었습니다.`)
  } catch (error) {
    console.error('승인 처리 실패:', error)
    alert('승인 처리 중 오류가 발생했습니다.')
  }
}

async function reject(row) {
  if (!confirm(`${row.store} 가맹점의 주문을 반려하시겠습니까?`)) return

  try {
    const { updateStoreOrderStatus } = await import('@/components/api/store/StoreService.js')
    await updateStoreOrderStatus(row.id, 'REJECTED')

    // ✅ 목록에서 제거 (필터링)
    rows.value = rows.value.filter(r => r.id !== row.id)
    totalElements.value = Math.max(0, totalElements.value - 1)

    alert(`${row.store} 가맹점의 주문이 반려되었습니다.`)
  } catch (error) {
    console.error('반려 처리 실패:', error)
    alert('반려 처리 중 오류가 발생했습니다.')
  }
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

.approval-table {
  width: 100%;
  border-collapse: collapse;
}

.approval-table th,
.approval-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f0f0f3;
  text-align: left;
}

.approval-table td.center,
.approval-table th.center {
  text-align: center;
}

.approval-table td.right,
.approval-table th.right {
  text-align: right;
}

.po {
  font-weight: 600;
}

.actions {
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.btn-accept {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-reject {
  background: #fff;
  color: #6b46ff;
  border: 1px solid #c7b8ff;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
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
