<template>
  <div class="settlement-list-page">
    <div class="page-header">
      <h1 class="page-title">정산 목록</h1>
      <p class="page-subtitle">가맹점 및 공급사의 정산 내역을 조회하고 관리합니다</p>
    </div>

    <!-- 필터 섹션 -->
    <section class="filter-section">
      <SettlementFilter @search="handleSearch" />
    </section>



    <!-- 목록 섹션 -->
    <section class="list-section">
      <div class="section-header">
        <div class="header-left">
          <h2 class="section-title">정산 내역</h2>
          <span class="result-count">총 <strong>{{ totalElements }}</strong>건</span>
        </div>
      </div>

      <div class="table-container">
        <table class="settlement-table">
          <thead>
            <tr>
              <th class="col-id">정산 ID</th>
              <th class="col-type">유형</th>
              <th class="col-vendor">상점/공급사</th>
              <th class="col-period">기간</th>
              <th class="col-qty">총 수량</th>
              <th class="col-amount">정산 금액</th>
              <th class="col-status">상태</th>
              <th class="col-date">생성일</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rows" :key="r.id" class="data-row">
              <td class="col-id">
                <span class="settlement-id">{{ r.id }}</span>
              </td>
              <td class="col-type">
                <span class="type-badge" :class="r.type === 'AR' ? 'type-ar' : 'type-ap'">
                  {{ r.type === 'AR' ? '미수금(AR)' : '미지급금(AP)' }}
                </span>
              </td>
              <td class="col-vendor">
                <div class="vendor-info">
                  <span class="vendor-name">{{ r.entity }}</span>
                </div>
              </td>
              <td class="col-period">
                <span class="period-text">{{ r.period }}</span>
              </td>
              <td class="col-qty">
                <span class="qty-text">{{ formatNumber(r.qty) }}개</span>
              </td>
              <td class="col-amount">
                <span class="amount-text">
                  <Money :value="r.total" />
                </span>
              </td>
              <td class="col-status">
                <span class="status-badge" :class="getStatusClass(r.status)">
                  {{ r.status }}
                </span>
              </td>
              <td class="col-date">
                <span class="date-text">{{ r.created }}</span>
              </td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="8" class="empty-state">
                <div class="empty-content">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1">
                    <rect x="3" y="3" width="18" height="18" rx="2" stroke-width="2" />
                    <path d="M3 9h18M9 21V9" stroke-width="2" />
                  </svg>
                  <div v-if="loading" class="loading-container">
                    <div class="spinner"></div>
                    <p class="empty-text">데이터를 불러오는 중...</p>
                  </div>
                  <div v-else>
                    <p class="empty-text">조회된 정산 내역이 없습니다</p>
                    <p class="empty-hint">검색 조건을 변경해보세요</p>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이지네이션 -->
      <div v-if="totalPages > 1" class="pagination">
        <button
          class="pager"
          :disabled="currentPage === 0"
          @click="changePage(currentPage - 1)"
        >
          ‹ 이전
        </button>

        <button
          v-for="page in pageNumbers"
          :key="page"
          class="page-btn"
          :class="{ active: page === currentPage }"
          @click="changePage(page)"
        >
          {{ page + 1 }}
        </button>

        <button
          class="pager"
          :disabled="currentPage === totalPages - 1"
          @click="changePage(currentPage + 1)"
        >
          다음 ›
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SettlementFilter from '@/components/domain/settlement/filter/SettlementFilter.vue';
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute, getTodayString, getPastDateString } from '@/components/global/Date.js';
import { getSettlements } from '@/components/api/settlement/SettlementService.js';

const loading = ref(false);
const todayString = getTodayString();
const rows = ref([])
const currentFilterData = ref(null)

// 페이지네이션 상태
const currentPage = ref(0)  // 0-based
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const MAX_VISIBLE_PAGES = 5

// 페이지 번호 배열 계산
const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)

  let start = Math.max(0, current - half)
  let end = Math.min(total - 1, start + MAX_VISIBLE_PAGES - 1)

  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(0, end - MAX_VISIBLE_PAGES + 1)
  }

  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const mapStatus = (backendStatus) => {
  switch (backendStatus) {
    case 'ISSUED': return '발행됨';
    case 'DRAFT': return '초안';
    case 'VOID': return '확정';
    case 'COMPLETED': return '완료';
    case 'PENDING': return '대기';
    case 'DELAYED': return '지연';
    default: return '알 수 없음';
  }
};

const getStatusClass = (status) => {
  const statusMap = {
    '발행됨': 'status-issued',
    '완료': 'status-completed',
    '초안': 'status-draft',
    '대기': 'status-pending',
    '지연': 'status-delayed',
    '무효': 'status-void',
  };
  return statusMap[status] || 'status-default';
};

const formatNumber = (value) => {
  if (!value && value !== 0) return '0';
  return Number(value).toLocaleString('ko-KR');
};

async function handleSearch(filters, page = 0) {
  currentFilterData.value = filters;
  loading.value = true;

  try {
    // API 파라미터 구성
    const params = {
      // scope가 ALL이면 백엔드에서 AR, AP 모두 조회
      types: filters.scope,  // 'ALL' | 'AR' | 'AP' (getSettlements에서 처리)
      vendorId: filters.vendorId, // getSettlements에서 'ALL' 처리
      fromDate: filters.startDate,
      toDate: filters.endDate,
      searchText: filters.keyword || null
    };


    // API 호출 (Spring Page 객체 반환)
    const pageData = await getSettlements(params, page, pageSize.value);

    // 페이지네이션 정보 업데이트
    currentPage.value = pageData.number ?? page;
    totalElements.value = pageData.totalElements ?? 0;
    totalPages.value = pageData.totalPages ?? 1;

    // 테이블 데이터 변환
    rows.value = pageData.content.map(settlement => ({
      id: settlement.settlementNo,
      type: settlement.settlementType,  // 'AR' | 'AP'
      entity: settlement.storeName === null
        ? settlement.supplierName
        : settlement.storeName,
      period: settlement.settledAt === null ?
        '정산 미완료' : settlement.settledAt,
      qty: settlement.settlementQty,
      total: settlement.settlementAmount,
      status: mapStatus(settlement.settlementStatus),
      created: formatDateTimeMinute(settlement.createdAt)
    }));


  } catch (error) {
    // 에러 메시지 상세화
    let errorMessage = '데이터를 불러오는 중 오류가 발생했습니다.';

    if (error.response) {
      // 서버 응답 에러
      errorMessage = `서버 오류 (${error.response.status}): ${error.response.data?.message || '알 수 없는 오류'}`;
    } else if (error.request) {
      // 네트워크 에러
      errorMessage = '서버와 연결할 수 없습니다. 네트워크 상태를 확인해주세요.';
    }

    alert(errorMessage);

    // 에러 시 데이터 초기화
    rows.value = [];
    totalElements.value = 0;
    totalPages.value = 0;
  } finally {
    loading.value = false;
  }
}

// 페이지 변경
function changePage(page) {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return;
  if (!currentFilterData.value) return;
  handleSearch(currentFilterData.value, page);
}

onMounted(() => {

});
</script>

<style scoped>
/* ============ Layout ============ */
.settlement-list-page {
  min-height: 100vh;
  background: #f8fafc;
  padding: 24px;
}

/* ============ Page Header ============ */
.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* ============ Filter Section ============ */
.filter-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

/* ============ List Section ============ */
.list-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 2px solid #e2e8f0;
  background: linear-gradient(to bottom, #ffffff, #f8fafc);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.3px;
}

.result-count {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.result-count strong {
  color: #6366f1;
  font-weight: 700;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-icon {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #1e293b;
}

.btn-icon svg {
  width: 16px;
  height: 16px;
}

/* ============ Table ============ */
.table-container {
  overflow-x: auto;
}

.settlement-table {
  width: 100%;
  border-collapse: collapse;
}

/* Table Header */
.settlement-table thead {
  background: #f8fafc;
  border-bottom: 2px solid #e2e8f0;
}

.settlement-table thead th {
  padding: 16px 20px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* Column Widths */
.col-id {
  width: 12%;
  text-align: center;
}

.col-type {
  width: 10%;
  text-align: center;
}

.col-vendor {
  width: 18%;
  text-align: left;
}

.col-period {
  width: 12%;
  text-align: center;
}

.col-qty {
  width: 10%;
  text-align: right;
}

.col-amount {
  width: 15%;
  text-align: right;
}

.col-status {
  width: 10%;
  text-align: center;
}

.col-date {
  width: 13%;
  text-align: center;
}

/* Table Body */
.settlement-table tbody td {
  padding: 18px 20px;
  font-size: 14px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

/* Body cell alignment overrides */
.settlement-table tbody .col-id {
  text-align: center;
}

.settlement-table tbody .col-type {
  text-align: center;
}

.settlement-table tbody .col-vendor {
  text-align: left;
}

.settlement-table tbody .col-period {
  text-align: center;
}

.settlement-table tbody .col-qty {
  text-align: right;
}

.settlement-table tbody .col-amount {
  text-align: right;
}

.settlement-table tbody .col-status {
  text-align: center;
}

.settlement-table tbody .col-date {
  text-align: center;
}

.data-row {
  transition: all 0.2s;
  border-bottom: 1px solid #f1f5f9;
}

.data-row:hover {
  background: #f8fafc;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

/* Settlement ID */
.settlement-id {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 13px;
  font-weight: 600;
  color: #3b82f6;
  letter-spacing: 0;
  background: #eff6ff;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
}

/* Type Badge */
.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: -0.2px;
}

.type-ar {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  border: 1px solid #93c5fd;
}

.type-ap {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid #fcd34d;
}

/* Vendor Info */
.vendor-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.vendor-name {
  font-weight: 700;
  color: #0f172a;
  font-size: 14px;
  letter-spacing: -0.2px;
}

/* Period Text */
.period-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  letter-spacing: -0.3px;
}

/* Quantity Text */
.qty-text {
  font-weight: 700;
  color: #334155;
  font-variant-numeric: tabular-nums;
  font-size: 14px;
  letter-spacing: -0.3px;
}

/* Amount Text */
.amount-text {
  font-weight: 800;
  color: #0f172a;
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5px;
}

/* Status Badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: -0.2px;
}

.status-issued {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  border: 1px solid #93c5fd;
}

.status-completed {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
  border: 1px solid #86efac;
}

.status-draft {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #475569;
  border: 1px solid #cbd5e1;
}

.status-pending {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid #fcd34d;
}

.status-delayed {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
  border: 1px solid #fca5a5;
}

.status-void {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #94a3b8;
  border: 1px solid #cbd5e1;
}

/* Date Text */
.date-text {
  font-size: 13px;
  color: #64748b;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  letter-spacing: -0.3px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

/* ============ Empty State ============ */
.empty-state {
  padding: 80px 20px !important;
  text-align: center;
  background: #fafbfc;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-content svg {
  opacity: 0.3;
}

.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
  margin: 0;
}

.empty-hint {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

/* ============ Pagination ============ */
.pagination {
  margin-top: 20px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

.pager {
  background: transparent;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 8px 12px;
  font-size: 14px;
  border-radius: 6px;
  transition: all 0.2s;
}

.pager:hover:not(:disabled) {
  background: #f1f5f9;
  color: #334155;
}

.pager:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.page-btn:hover:not(.active) {
  background: #f8fafc;
  border-color: #d1d5db;
}

.page-btn.active {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
  font-weight: 600;
}

/* ============ Responsive ============ */
@media (max-width: 1024px) {
  .settlement-list-page {
    padding: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .settlement-table thead th,
  .settlement-table tbody td {
    padding: 12px 16px;
    font-size: 13px;
  }

  .col-id,
  .col-type,
  .col-vendor,
  .col-period,
  .col-qty,
  .col-amount,
  .col-status,
  .col-date {
    width: auto;
  }
}

@media (max-width: 768px) {
  .page-header {
    margin-bottom: 20px;
  }

  .page-title {
    font-size: 20px;
  }

  .result-count {
    font-size: 13px;
  }

  .table-container {
    overflow-x: scroll;
  }

  .settlement-table {
    min-width: 900px;
  }
}
</style>