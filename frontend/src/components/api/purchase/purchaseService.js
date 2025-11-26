
import axios from 'axios';

/**
 * 발주 상태를 한글로 매핑하는 함수
 * @param {string} status - 백엔드 상태 코드
 * @returns {string} 한글 상태명
 */
export function mapPurchaseStatus(status) {
    const statusMap = {
        'DRAFT_AUTO': '초안',
        'SUBMITTED': '제출',
        'CONFIRMED': '승인',
        'REJECTED': '반려',
        'CANCELLED': '취소',
        'MANUAL': '일반',
        'AUTO': '자동',
        'SMART': '스마트',
    };
    return statusMap[status] || '알 수 없음';
}

export function purchaseStatusOptions() {
    return [
        { text: '전체', value: 'ALL' },
        { text: '초안', value: 'DRAFT_AUTO' },
        { text: '제출', value: 'SUBMITTED' },
        { text: '승인', value: 'CONFIRMED' },
        { text: '반려', value: 'REJECTED' },
        { text: '취소', value: 'CANCELLED' },
        { text: '일반', value: 'MANUAL' },
        { text: '자동', value: 'AUTO' },
        { text: '스마트', value: 'SMART' },
    ];
}

/**
 * [발주 상태 업데이트(승인 및 거절) ]
 * @param
 * 
 */

export async function updatePurchaseStatus(purchaseId, status) {

    const url = `/api/v1/purchase-orders/${purchaseId}/${status}`;
    console.log("발주 상태 업데이트 URL:", url);
    try {
        console.log("발주 상태 업데이트 요청 - purchaseId:", purchaseId, "status:", status);
        const response = await axios.patch(url, {
            status: status,
        });
        console.log("발주 상태 업데이트 응답:", response.data);
        return response.data;
    }
    catch (error) {
        console.error("[API Error] 발주 상태 업데이트 실패:", error.message, error.response);
        throw new Error("API 서버와의 통신에 실패했습니다.");
    }
}

/**
 * 일반 발주 DTO를 공통 형식으로 변환
 * @param {object} purchase - 일반 발주 DTO
 * @returns {object} 공통 형식의 발주 객체
 */
function mapRegularPurchase(purchase) {
    return {
        purchaseId: purchase.purchaseId,
        poNo: purchase.poNo,
        supplierName: purchase.supplierName,
        requesterName: purchase.requesterName,
        totalQty: purchase.totalQty,
        totalAmount: purchase.totalAmount,
        status: purchase.status,
        orderType: purchase.orderType || 'MANUAL',
        requestedAt: purchase.requestedAt,
        sourceType: 'REGULAR' // 구분용
    };
}

/**
 * 스마트 발주 DTO를 공통 형식으로 변환
 * @param {object} smartOrder - 스마트 발주 DTO
 * @returns {object} 공통 형식의 발주 객체
 */
function mapSmartPurchase(smartOrder) {
    return {
        purchaseId: smartOrder.id,
        poNo: `SMART-${smartOrder.id}`, // 스마트 발주는 PO 번호 생성
        supplierName: smartOrder.supplierName || '공급업체 정보 없음',
        requesterName: '자동 생성', // 스마트 발주는 자동 생성
        totalQty: smartOrder.recommendedOrderQty || 0,
        totalAmount: 0, // 스마트 발주는 금액 정보 없음
        status: smartOrder.smartOrderStatus,
        orderType: 'SMART',
        requestedAt: smartOrder.snapshotAt || smartOrder.updatedAt,
        sourceType: 'SMART', // 구분용
        // 스마트 발주 전용 필드
        targetWeek: smartOrder.targetWeek,
        forecastQty: smartOrder.forecastQty,
        demandForecastId: smartOrder.demandForecastId
    };
}

/**
 * [일반 발주 목록 조회]
 * @param {object} cond - 검색 조건 객체
 * @param {number} page 조회할 페이지 번호 (0부터 시작)
 * @param {number} perPage 페이지당 항목 수
 * @returns {Promise<object>} 발주 목록 및 페이지 정보
 */
async function getRegularPurchases(cond, page, perPage) {
    const url = '/api/v1/purchase-orders';

    try {
        const params = {
            page: page || 0,
            size: perPage || 10
        };

        // TradeSearchCondition 필드 매핑
        if (cond.types && cond.types.length > 0) params.types = cond.types.join(',');
        if (cond.statuses && cond.statuses.length > 0) params.statuses = cond.statuses.join(',');
        if (cond.vendorId) params.vendorId = cond.vendorId;
        if (cond.searchText) params.searchText = cond.searchText;
        if (cond.fromDate) params.fromDate = cond.fromDate;
        if (cond.toDate) params.toDate = cond.toDate;

        const response = await axios.get(url, { params });

        // 일반 발주 데이터를 공통 형식으로 변환
        const mappedContent = (response.data.content || []).map(mapRegularPurchase);

        console.log("일반 발주 목록 조회 응답:", response);

        return {
            ...response.data,
            content: mappedContent
        };
    } catch (error) {
        console.error("[API Error] 일반 발주 목록 조회 실패:", error.message);
        throw error;
    }
}

/**
 * [스마트 발주 목록 조회]
 * @param {string} [status] 상태 필터
 * @param {string} [from] 시작 날짜 (YYYY-MM-DD)
 * @param {string} [to] 종료 날짜 (YYYY-MM-DD)
 * @returns {Promise<object>} 발주 목록 (페이지네이션 없이 List 반환)
 */
async function getSmartPurchases(status, from, to) {
    const url = '/api/v1/smart-orders';

    try {
        const params = {};
        if (status) params.status = status;
        if (from) params.from = from;
        if (to) params.to = to;

        const response = await axios.get(url, { params });

        // 스마트 발주는 List로 반환되므로 content 형식으로 변환
        const smartOrders = Array.isArray(response.data) ? response.data : [];
        const mappedContent = smartOrders.map(mapSmartPurchase);

        console.log("스마트 발주 목록 조회 응답:", response);

        return {
            content: mappedContent,
            totalElements: mappedContent.length,
            totalPages: 1
        };
    } catch (error) {
        console.error("[API Error] 스마트 발주 목록 조회 실패:", error.message);
        throw error;
    }
}

/**
 * [통합 발주 목록 조회]
 * 일반 발주와 스마트 발주를 병합하여 반환합니다.
 * @param {number} page 조회할 페이지 번호 (0부터 시작)
 * @param {number} perPage 페이지당 항목 수
 * @param {string} [q] 검색어 (searchText)
 * @param {string} [status] 상태 필터 (단일 상태, statuses 배열로 변환)
 * @param {string} [vendorId] 공급업체 ID
 * @param {string} [startDate] 시작 날짜 (fromDate)
 * @param {string} [endDate] 종료 날짜 (toDate)
 * @returns {Promise<object>} 병합된 발주 목록 및 페이지 정보
 * @throws {Error} API 통신 실패 시 에러 발생
 */
export async function getPurchases(page, perPage, q, status, vendorId, startDate, endDate) {
    try {

        // 일반 발주 검색 조건 생성
        const regularCond = {
            types: [], // 발주는 types 사용 안 함
            statuses: status ? [status] : [], // 단일 상태를 배열로 변환
            vendorId: vendorId || null,
            searchText: q || null,
            fromDate: startDate || null,
            toDate: endDate || null
        };
        console.log("파라미터 regularCond:", regularCond);

        // 두 API를 병렬로 호출 (모든 데이터 가져오기)
        const [regularData, smartData] = await Promise.all([
            getRegularPurchases(regularCond, 0, 1000).catch(err => {
                console.warn("일반 발주 조회 실패, 빈 결과 반환:", err.message);
                return { content: [], totalElements: 0, totalPages: 0 };
            }),
            getSmartPurchases(status, startDate, endDate).catch(err => {
                console.warn("스마트 발주 조회 실패, 빈 결과 반환:", err.message);
                return { content: [], totalElements: 0, totalPages: 0 };
            })
        ]);

        // 두 결과를 병합
        const mergedContent = [
            ...(regularData.content || []),
            ...(smartData.content || [])
        ];

        // requestedAt 기준으로 내림차순 정렬 (최신순)
        mergedContent.sort((a, b) => {
            const dateA = new Date(a.requestedAt || 0);
            const dateB = new Date(b.requestedAt || 0);
            return dateB - dateA;
        });

        // 클라이언트 사이드 페이지네이션
        const startIndex = page * perPage;
        const endIndex = startIndex + perPage;
        const paginatedContent = mergedContent.slice(startIndex, endIndex);

        // 통합 결과 반환
        const result = {
            content: paginatedContent,
            totalElements: mergedContent.length,
            totalPages: Math.ceil(mergedContent.length / perPage),
            size: perPage,
            number: page
        };

        console.log("통합 발주 목록 조회 응답:", result);
        return result;

    } catch (error) {
        console.error("[API Error] 통합 발주 목록 조회 실패:", error.message, error.response);
        throw new Error("API 서버와의 통신에 실패했습니다.");
    }
}

export async function getPurchaseDetail(id) {

    const url = `/api/v1/purchase-orders/${id}`;

    try {
        const response = await axios.get(url, {});

        // ItemsResponseDto 객체에서 'purchaseDetail' 필드를 직접 추출
        // response.data가 { "purchaseDetail": { ... } } 구조라고 가정합니다.
        const detailObject = response.data;

        // 추출된 상세 객체 자체를 반환합니다.
        // 이렇게 하면 호출하는 컴포넌트에서 response.purchaseDetail 대신 detailObject를 바로 사용할 수 있습니다.
        return detailObject;

    } catch (error) {
        // API 통신 실패 로그를 남기고, 에러를 다시 던집니다.
        console.error("[API Error] 발주 상세 조회 실패:", error.message, error.response);
        throw new Error("API 서버와의 통신에 실패했습니다.");
    }
}

