<template>
    <div class="settlement-report-page">
        <div class="page-header">
            <h1 class="page-title">정산 리포트</h1>
            <p class="page-subtitle">가맹점 및 공급사의 정산 데이터를 분석하고 시각화합니다</p>
        </div>

        <!-- 필터 섹션 -->
        <section class="filter-section">
            <SettlementFilter @search="handleSearch" />
        </section>

        <!-- 로딩 상태 -->
        <div v-if="loading" class="loading-container">
            <div class="spinner"></div>
            <p>데이터를 불러오는 중...</p>
        </div>

        <!-- 데이터 표시 -->
        <template v-else-if="summaryData">
            <!-- 주요 통계 카드 -->
            <section class="summary-cards">
                <div class="stat-card">
                    <div class="stat-icon">📋</div>
                    <div class="stat-content">
                        <span class="stat-label">총 정산 건수</span>
                        <span class="stat-value">{{ formatNumber(summaryData.totalRecords) }}건</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-icon">💳</div>
                    <div class="stat-content">
                        <span class="stat-label">총 정산 금액</span>
                        <span class="stat-value">₩{{ formatNumber(summaryData.totalAmount) }}</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-icon">📊</div>
                    <div class="stat-content">
                        <span class="stat-label">평균 단가</span>
                        <span class="stat-value">₩{{ formatNumber(summaryData.averagePrice) }}</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-icon">💰</div>
                    <div class="stat-content">
                        <span class="stat-label">미결제 금액</span>
                        <span class="stat-value status-warning">₩{{ formatNumber(summaryData.unpaidAmount) }}</span>
                    </div>
                </div>
            </section>

            <!-- 차트 섹션 -->
            <section class="charts-section">
                <div class="chart-row">
                    <!-- 월별 정산 금액 -->
                    <div class="chart-card">
                        <div class="card-header">
                            <h3>월별 정산 금액</h3>
                            <p class="card-subtitle">최근 12개월간 정산 금액 추이</p>
                        </div>
                        <MonthlyBarChart v-if="monthlyData.length > 0" :data="monthlyData" />
                        <div v-else class="chart-empty">월별 데이터가 없습니다.</div>
                    </div>

                    <!-- 가맹점/공급사별 정산 비율 -->
                    <div class="chart-card">
                        <div class="card-header">
                            <h3>
                                {{ currentFilterData?.scope === 'AR' ? '가맹점별' : '공급사별' }} 정산 비율
                            </h3>
                            <p class="card-subtitle">총 정산 금액에서 차지하는 비율</p>
                        </div>
                        <RatioDonutChart v-if="ratioData.length > 0" :data="ratioData" />
                        <div v-else class="chart-empty">비율 데이터가 없습니다.</div>
                    </div>
                </div>

                <!-- 정산 상태 분포 -->
                <div class="chart-card full-width">
                    <div class="card-header">
                        <h3>정산 상태 분포</h3>
                        <p class="card-subtitle">전체 정산 상태별 금액 분포</p>
                    </div>
                    <RatioDonutChart v-if="distributionData.length > 0" :data="distributionData" />
                    <div v-else class="chart-empty">상태별 데이터가 없습니다.</div>
                </div>
            </section>

            <!-- 요약 테이블 -->
            <section class="table-section card">
                <div class="card-header">
                    <h3>정산 요약 테이블</h3>
                    <p class="card-subtitle">{{ currentFilterData?.scope === 'AR' ? '가맹점' : '공급사' }}별 집계</p>
                </div>

                <div class="table-wrapper">
                    <table class="summary-table">
                        <thead>
                            <tr>
                                <th>{{ currentFilterData?.scope === 'AR' ? '가맹점명' : '공급사명' }}</th>
                                <th>정산 수량</th>
                                <th>정산 금액</th>
                                <th>상태</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(row, index) in tableData" :key="index">
                                <td>
                                    <span class="vendor-name">{{ row.name }}</span>
                                    <span class="vendor-type">{{ row.type === 'AR' ? '(가맹점)' : '(공급사)' }}</span>
                                </td>
                                <td class="numeric">{{ formatNumber(row.count) }}개</td>
                                <td class="numeric">₩{{ formatNumber(row.netAmount) }}</td>
                                <td>
                                    <span class="status-badge" :class="getStatusClass(row.status)">
                                        {{ row.status }}
                                    </span>
                                </td>
                            </tr>
                            <tr v-if="tableData.length === 0">
                                <td colspan="4" class="empty-cell">
                                    조회된 데이터가 없습니다.
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </template>

        <!-- 데이터 없음 -->
        <div v-else class="empty-state">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#d1d5db">
                <circle cx="12" cy="12" r="10" stroke-width="2" />
                <path d="M12 6v6l4 2" stroke-width="2" stroke-linecap="round" />
            </svg>
            <h3>데이터가 없습니다</h3>
            <p>필터를 설정하고 검색 버튼을 눌러주세요.</p>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import SettlementFilter from '@/components/domain/settlement/filter/SettlementFilter.vue';
import MonthlyBarChart from '@/components/domain/settlement/charts/MonthlyBarChart.vue';
import RatioDonutChart from '@/components/domain/settlement/charts/RatioDonutChart.vue';
import { getSettlementReport } from '@/components/api/settlement/SettlementService';
import { SettlementDataProcessor } from '@/components/global/SettlementDataProcessor.js';

const loading = ref(false);
const currentFilterData = ref(null);

// 데이터 상태
const summaryData = ref(null);
const monthlyData = ref([]);
const ratioData = ref([]);
const distributionData = ref([]);
const tableData = ref([]);

/**
 * 필터 검색 핸들러
 */
async function handleSearch(filters) {

    currentFilterData.value = filters;
    loading.value = true;

    try {
        // API 파라미터 구성
        const params = {
            // scope가 null이면 백엔드에서 AR, AP 모두 조회
            types: filters.scope,  // null | 'AR' | 'AP'
            vendorId: filters.vendorId === 'ALL'
                ? null : filters.vendorId,
            fromDate: filters.startDate,
            toDate: filters.endDate,
            searchText: filters.keyword || null
        };

        // API 호출 (Spring Page 객체 반환)
        const pageData = await getSettlementReport(params);

        // 빈 데이터 체크
        if (!pageData.content || pageData.content.length === 0) {
            summaryData.value = null;
            monthlyData.value = [];
            ratioData.value = [];
            distributionData.value = [];
            tableData.value = [];

            return;
        }

        // 프론트엔드에서 데이터 가공
        const processor = new SettlementDataProcessor(pageData);

        summaryData.value = processor.getSummary();
        monthlyData.value = processor.getMonthlyData();
        ratioData.value = processor.getRatioData();
        distributionData.value = processor.getDistributionData();
        tableData.value = processor.getTableData();

    } catch (error) {
        // 에러 메시지 상세화
        let errorMessage = '데이터를 불러오는 중 오류가 발생했습니다.';

        if (error.response) {
            // 서버 응답 에러
            errorMessage = `서버 오류 (${error.response.status}): ${error.response.data?.message || '알 수 없는 오류'
                }`;
        } else if (error.request) {
            // 네트워크 에러
            errorMessage = '서버와 연결할 수 없습니다. 네트워크 상태를 확인해주세요.';
        }

        alert(errorMessage);

        // 에러 시 데이터 초기화
        summaryData.value = null;
        monthlyData.value = [];
        ratioData.value = [];
        distributionData.value = [];
        tableData.value = [];
    } finally {
        loading.value = false;
    }
}

/**
 * 숫자 포맷팅
 */
function formatNumber(value) {
    if (!value && value !== 0) return '0';
    return Number(value).toLocaleString('ko-KR');
}

/**
 * 상태 배지 클래스
 */
function getStatusClass(status) {
    const statusMap = {
        '완료': 'status-complete',
        '승인': 'status-complete',
        '대기': 'status-pending',
        '지연': 'status-delayed',
        '취소': 'status-cancelled',
        '거절': 'status-cancelled'
    };
    return statusMap[status] || 'status-default';
}
</script>

<style scoped>
.settlement-report-page {
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

.card {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 로딩 */
.loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
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
    to {
        transform: rotate(360deg);
    }
}

/* 통계 카드 */
.summary-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 24px;
}

.stat-card {
    background: white;
    border-radius: 12px;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
    font-size: 32px;
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f0f2ff;
    border-radius: 12px;
}

.stat-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.stat-label {
    font-size: 13px;
    color: #6b7280;
    font-weight: 500;
}

.stat-value {
    font-size: 24px;
    font-weight: 700;
    color: #1f2937;
}

/* 차트 섹션 */
.charts-section {
    margin-bottom: 24px;
}

.chart-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
}

.chart-card {
    background: white;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.chart-card.full-width {
    width: 100%;
}

.card-header {
    margin-bottom: 20px;
}

.card-header h3 {
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
    margin: 0 0 4px 0;
}

.card-subtitle {
    font-size: 13px;
    color: #9ca3af;
    margin: 0;
}

/* 테이블 섹션 */
.table-section {
    margin-bottom: 24px;
}

.table-wrapper {
    overflow-x: auto;
    margin-top: 16px;
}

.summary-table {
    width: 100%;
    border-collapse: collapse;
}

.summary-table thead {
    background: #f8fafc;
}

.summary-table th {
    padding: 12px 16px;
    text-align: left;
    font-size: 13px;
    font-weight: 600;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    border-bottom: 2px solid #e5e7eb;
}

.summary-table td {
    padding: 16px;
    border-bottom: 1px solid #f1f3f5;
    font-size: 14px;
    color: #374151;
}

.summary-table tbody tr:hover {
    background: #f8fafc;
}

.numeric {
    text-align: right;
    font-variant-numeric: tabular-nums;
    font-weight: 500;
}

.status-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
}

.status-complete {
    background: #d1fae5;
    color: #065f46;
}

.status-pending {
    background: #fef3c7;
    color: #92400e;
}

.status-delayed {
    background: #fee2e2;
    color: #991b1b;
}

.status-cancelled {
    background: #f3f4f6;
    color: #6b7280;
    text-decoration: line-through;
}

.status-default {
    background: #f3f4f6;
    color: #6b7280;
}

.empty-cell {
    text-align: center;
    color: #9ca3af;
    padding: 40px !important;
}

/* 빈 상태 */
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
    background: white;
    border-radius: 12px;
    gap: 16px;
}

.empty-state h3 {
    margin: 0;
    font-size: 18px;
    color: #374151;
}

.empty-state p {
    margin: 0;
    font-size: 14px;
    color: #9ca3af;
}

/* 추가 스타일 */
.status-warning {
    color: #dc2626;
}

.chart-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 300px;
    color: #9ca3af;
    font-size: 14px;
}

.vendor-name {
    font-weight: 600;
    color: #1f2937;
}

.vendor-type {
    margin-left: 8px;
    font-size: 12px;
    color: #6b7280;
}
</style>