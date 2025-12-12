<template>
  <div class="settlement-detail-page">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>정산 정보를 불러오는 중...</p>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="error-container">
      <div class="error-content">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#ef4444">
          <circle cx="12" cy="12" r="10" stroke-width="2" />
          <path d="M12 8v4M12 16h.01" stroke-width="2" stroke-linecap="round" />
        </svg>
        <h3>데이터를 불러올 수 없습니다</h3>
        <p>{{ error }}</p>
        <button class="btn-primary" @click="fetchSettlementDetail">다시 시도</button>
      </div>
    </div>

    <!-- 상세 정보 -->
    <template v-else-if="settlement">
      <div class="page-header">
        <div class="header-top">
          <button class="back-btn" @click="goBack">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M19 12H5M12 19l-7-7 7-7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            돌아가기
          </button>
          <span class="status-badge" :class="getStatusClass(settlement.status)">
            {{ settlement.status }}
          </span>
        </div>
        <h1 class="page-title">정산 상세 정보</h1>
        <p class="page-subtitle">정산번호: {{ settlement.settlementNo }}</p>
      </div>

      <!-- 주요 정보 카드 -->
      <div class="info-cards">
        <div class="info-card">
          <div class="card-icon type">📋</div>
          <div class="card-content">
            <span class="card-label">정산 유형</span>
            <span class="card-value">
              <span class="type-badge" :class="settlement.type === 'AR' ? 'type-ar' : 'type-ap'">
                {{ settlement.type === 'AR' ? '미수금 (AR)' : '미지급금 (AP)' }}
              </span>
            </span>
          </div>
        </div>

        <div class="info-card">
          <div class="card-icon amount">💰</div>
          <div class="card-content">
            <span class="card-label">정산 금액</span>
            <span class="card-value amount-value">
              <Money :value="settlement.amount" />
            </span>
          </div>
        </div>

        <div class="info-card">
          <div class="card-icon qty">📦</div>
          <div class="card-content">
            <span class="card-label">정산 수량</span>
            <span class="card-value qty-value">{{ formatNumber(settlement.qty) }}개</span>
          </div>
        </div>
      </div>

      <!-- 상세 정보 섹션 -->
      <div class="detail-section card">
        <div class="section-header">
          <h2>기본 정보</h2>
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="item-label">{{ settlement.type === 'AR' ? '가맹점명' : '공급사명' }}</span>
            <span class="item-value">{{ settlement.vendorName }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">생성일시</span>
            <span class="item-value">{{ settlement.createdAt }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">완료일시</span>
            <span class="item-value">{{ settlement.settledAt || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 정산 항목 테이블 -->
      <div class="items-section card">
        <div class="section-header">
          <h2>정산 항목</h2>
          <span class="item-count">총 {{ settlement.items?.length || 0 }}개 항목</span>
        </div>
        <div class="table-wrapper">
          <table class="items-table">
            <thead>
              <tr>
                <th>품목명</th>
                <th>품목코드</th>
                <th class="text-right">수량</th>
                <th class="text-right">단가</th>
                <th class="text-right">금액</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in settlement.items" :key="index">
                <td>{{ item.productName }}</td>
                <td>{{ item.productCode }}</td>
                <td class="text-right">{{ formatNumber(item.qty) }}</td>
                <td class="text-right"><Money :value="item.price" /></td>
                <td class="text-right amount-cell"><Money :value="item.amount" /></td>
              </tr>
              <tr v-if="!settlement.items || settlement.items.length === 0">
                <td colspan="5" class="empty-cell">정산 항목이 없습니다</td>
              </tr>
            </tbody>
            <tfoot v-if="settlement.items && settlement.items.length > 0">
              <tr class="total-row">
                <td colspan="4" class="text-right total-label">합계</td>
                <td class="text-right total-amount"><Money :value="settlement.amount" /></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <!-- 액션 버튼 -->
      <div class="action-section">
        <button class="btn-secondary" @click="goBack">목록으로</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Money from '@/components/global/Money.vue'
import { formatDateTimeMinute } from '@/components/global/Date.js'
import apiClient from '@/components/api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref(null)
const settlement = ref(null)

const settlementId = route.params.id

async function fetchSettlementDetail() {
  loading.value = true
  error.value = null

  try {
    // 정산 상세 API 호출
    const response = await apiClient.get(`/api/v1/settlements/${settlementId}`)
    const data = response.data

    console.log('Settlement 상세 데이터:', data)

    // 데이터 매핑
    settlement.value = {
      settlementNo: data.settlementNo || settlementId,
      type: data.settlementType || 'AR',
      status: mapStatus(data.settlementStatus),
      amount: data.settlementAmount || 0,
      qty: data.settlementQty || 0,
      vendorName: data.vendorName || '-',
      createdAt: data.createdAt ? formatDateTimeMinute(data.createdAt) : '-',
      settledAt: data.settledDate ? formatDateTimeMinute(data.settledDate) : null,
      periodStart: data.periodStart ? formatDateTimeMinute(data.periodStart) : null,
      periodEnd: data.periodEnd ? formatDateTimeMinute(data.periodEnd) : null,
      items: data.items?.map(item => ({
        productName: item.productName || '-',
        productCode: item.productCode || '-',
        qty: item.qty || 0,
        price: item.price || 0,
        amount: item.amount || 0
      })) || []
    }

  } catch (err) {
    console.error('정산 상세 조회 실패:', err)
    error.value = err.response?.data?.message || err.message || '정산 정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

function mapStatus(status) {
  const statusMap = {
    'DRAFT': '초안',
    'ISSUED': '발행됨',
    'COMPLETED': '완료',
    'VOID': '완료',
    'PENDING': '대기',
    'DELAYED': '지연',
  }
  return statusMap[status] || status || '알 수 없음'
}

function getStatusClass(status) {
  const classMap = {
    '초안': 'status-draft',
    '발행됨': 'status-issued',
    '완료': 'status-completed',
    '대기': 'status-pending',
    '지연': 'status-delayed',
  }
  return classMap[status] || 'status-default'
}

function formatNumber(value) {
  if (!value && value !== 0) return '0'
  return Number(value).toLocaleString('ko-KR')
}

function goBack() {
  router.push({ name: 'SettlementList' })
}

onMounted(() => {
  fetchSettlementDetail()
})
</script>

<style scoped>
.settlement-detail-page {
  min-height: 100vh;
  background: #f8fafc;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ============ Loading & Error ============ */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 16px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f4f6;
  border-top-color: #6b72f9;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.error-content h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.error-content p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

/* ============ Page Header ============ */
.page-header {
  margin-bottom: 32px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.back-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #334155;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

/* ============ Status Badge ============ */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
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

.status-default {
  background: #f3f4f6;
  color: #6b7280;
}

/* ============ Info Cards ============ */
.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.info-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.card-icon.type {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.card-icon.amount {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.card-icon.qty {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.card-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.card-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.amount-value {
  color: #0f172a;
}

.qty-value {
  color: #334155;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
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

/* ============ Card Section ============ */
.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.item-count {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

/* ============ Detail Grid ============ */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.item-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.item-value {
  font-size: 15px;
  color: #1e293b;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

/* ============ Items Table ============ */
.table-wrapper {
  overflow-x: auto;
  margin-top: 16px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table thead {
  background: #f8fafc;
  border-bottom: 2px solid #e2e8f0;
}

.items-table thead th {
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  text-align: left;
  white-space: nowrap;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.items-table tbody td {
  padding: 14px 16px;
  font-size: 14px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.items-table tfoot {
  background: #f8fafc;
  border-top: 2px solid #e2e8f0;
}

.items-table tfoot td {
  padding: 16px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.text-right {
  text-align: right !important;
}

.amount-cell {
  font-weight: 700;
  color: #0f172a;
}

.total-row {
  background: #f8fafc;
}

.total-label {
  color: #64748b;
  font-size: 14px;
}

.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
  padding: 40px !important;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

/* ============ Action Section ============ */
.action-section {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 32px;
}

.btn-primary,
.btn-secondary {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

.btn-primary {
  background: linear-gradient(135deg, #6b72f9 0%, #5b62e9 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(107, 114, 249, 0.3);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(107, 114, 249, 0.4);
}

.btn-secondary {
  background: white;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.btn-secondary:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #334155;
}
</style>
