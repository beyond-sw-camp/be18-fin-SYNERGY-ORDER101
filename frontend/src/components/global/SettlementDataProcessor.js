/**
 * 정산 데이터 가공 유틸리티
 */
export class SettlementDataProcessor {
    constructor(pageData, filterScope = null) {
        // Spring Page 객체 구조 처리
        this.settlements = pageData.content || [];
        this.totalElements = pageData.totalElements || 0;
        this.totalPages = pageData.totalPages || 0;
        this.currentPage = pageData.number || 0;
        this.pageSize = pageData.size || 20;
        this.filterScope = filterScope; // 'AR' | 'AP' | null
    }

    /**
     * 요약 통계 계산
     */
    getSummary() {
        // 총 금액
        const totalAmount = this.settlements.reduce(
            (sum, item) => sum + (Number(item.settlementAmount) || 0),
            0
        );

        // 총 수량
        const totalCount = this.settlements.reduce(
            (sum, item) => sum + (item.settlementQty || 0),
            0
        );

        // 미결제 항목
        const unpaidItems = this.settlements.filter(
            item => item.settlementStatus === 'PENDING' ||
                item.settlementStatus === 'DELAYED'
        );

        const unpaidAmount = unpaidItems.reduce(
            (sum, item) => sum + (Number(item.settlementAmount) || 0),
            0
        );

        // 평균 단가
        const averagePrice = totalCount > 0
            ? totalAmount / totalCount
            : 0;

        return {
            totalCount: totalCount,
            totalAmount: totalAmount,
            averagePrice: Math.round(averagePrice),
            unpaidAmount: unpaidAmount,
            totalRecords: this.totalElements  // 전체 레코드 수
        };
    }

    /**
     * 월별 결제 금액 집계
     */
    getMonthlyData() {
        const monthlyMap = new Map();

        this.settlements.forEach(item => {
            if (!item.createdAt) return;

            // "2023-11-24T10:30:00" → "2023-11"
            const month = item.createdAt.substring(0, 7);

            const existing = monthlyMap.get(month) || 0;
            monthlyMap.set(month, existing + (Number(item.settlementAmount) || 0));
        });

        // 최근 12개월 데이터만 추출 (정렬)
        const sortedMonths = Array.from(monthlyMap.entries())
            .sort((a, b) => a[0].localeCompare(b[0]))
            .slice(-12);

        return sortedMonths.map(([month, amount]) => ({
            month: this._formatMonth(month),
            amount: amount
        }));
    }

    /**
     * 가맹점/공급사별 결제 비율
     */
    getRatioData() {
        const vendorMap = new Map();

        // 필터 scope에 따라 데이터 필터링
        const filteredSettlements = this.filterScope
            ? this.settlements.filter(item => item.settlementType === this.filterScope)
            : this.settlements;

        filteredSettlements.forEach(item => {
            // settlementType에 따라 이름 선택
            const name = item.settlementType === 'AR'
                ? item.storeName      // 가맹점
                : item.supplierName;  // 공급사

            if (!name) return;

            const existing = vendorMap.get(name) || 0;
            vendorMap.set(name, existing + (Number(item.settlementAmount) || 0));
        });

        // 상위 10개만 표시
        return Array.from(vendorMap.entries())
            .sort((a, b) => b[1] - a[1])
            .slice(0, 10)
            .map(([name, value]) => ({ name, value }));
    }

    /**
     * 결제 상태별 분포
     */
    getDistributionData() {
        const statusMap = new Map();

        this.settlements.forEach(item => {
            const status = item.settlementStatus || 'UNKNOWN';
            const existing = statusMap.get(status) || 0;
            statusMap.set(status, existing + (Number(item.settlementAmount) || 0));
        });

        return Array.from(statusMap.entries()).map(([name, value]) => ({
            name: this._translateStatus(name),
            value: value
        }));
    }

    /**
     * 요약 테이블 데이터
     */
    getTableData() {
        const vendorMap = new Map();

        // 필터 scope에 따라 데이터 필터링
        const filteredSettlements = this.filterScope
            ? this.settlements.filter(item => item.settlementType === this.filterScope)
            : this.settlements;

        filteredSettlements.forEach(item => {
            const key = item.settlementType === 'AR'
                ? item.storeName
                : item.supplierName;

            if (!key) return;

            if (!vendorMap.has(key)) {
                vendorMap.set(key, {
                    name: key,
                    type: item.settlementType,
                    count: 0,
                    productAmount: 0,
                    netAmount: 0,
                    statuses: new Set()
                });
            }

            const vendor = vendorMap.get(key);
            vendor.count += item.settlementQty || 0;
            vendor.productAmount += Number(item.settlementAmount) || 0;
            vendor.netAmount += Number(item.settlementAmount) || 0;
            vendor.statuses.add(item.settlementStatus);
        });

        // 금액 기준 내림차순 정렬
        return Array.from(vendorMap.values())
            .map(vendor => ({
                ...vendor,
                status: this._determineOverallStatus(vendor.statuses),
                statuses: Array.from(vendor.statuses)  // Set → Array
            }))
            .sort((a, b) => b.netAmount - a.netAmount);
    }

    // ========== Private Methods ==========

    _formatMonth(monthString) {
        // '2023-01' → '2023년 1월' (월 표시는 차트 컴포넌트에서 처리)
        const [year, month] = monthString.split('-');
        return `${year}년 ${parseInt(month)}`;
    }

    _translateStatus(status) {
        const statusMap = {
            'DRAFT': '초안',
            'ISSUED': '발행됨',
            'COMPLETED': '완료',
            'VOID': '완료',
            'PENDING': '대기',
            'DELAYED': '지연',
            'CANCELLED': '취소',
            'APPROVED': '승인',
            'REJECTED': '거절'
        };
        return statusMap[status] || status;
    }

    _determineOverallStatus(statuses) {
        if (statuses.has('DELAYED')) return '지연';
        if (statuses.has('PENDING')) return '대기';
        if (statuses.has('COMPLETED')) return '완료';
        if (statuses.has('APPROVED')) return '승인';
        return '알 수 없음';
    }
}