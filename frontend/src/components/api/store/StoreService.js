import axios from 'axios';


export async function getFranchiseOrderList(page, pageSize, searchParams) {
    const url = '/api/v1/store-orders'; // 가맹점 API 엔드포인트
    const apiPage = page - 1; // 0-based 변환

    console.log("가맹점 API 요청 파라미터:", searchParams);
    const params = {
        // 검색 조건
        fromDate: searchParams.fromDate || null,
        toDate: searchParams.toDate || null,
        storeId: searchParams.storeId || null,       // 가맹점 ID
        statuses: searchParams.statuses || null,
        searchText: searchParams.searchText || null,

        // Pageable 필드
        page: apiPage,
        size: pageSize,
        sort: 'createdAt,desc'                  // 생성일 내림차순
    };

    console.log("📤 실제 전송 파라미터:", params);

    try {
        const response = await axios.get(url, { params });
        const apiData = response.data;

        // Settlement과 동일한 구조로 반환 (Spring Page 객체)
        return apiData;
    } catch (error) {
        console.error("[API Error] 가맹점 목록 조회 실패:", error.message);
        throw new Error("가맹점 API 서버와의 통신에 실패했습니다.");
    }
}


/**
 * [가맹점 목록 조회]
 * @param {number} page - 페이지 번호 (1-based)
 * @param {number} pageSize - 페이지 크기
 * @param {string} keyword - 검색 키워드
 * @returns {Promise<{franchises: Array, totalCount: number, currentPage: number}>}
 */
export async function getFranchiseList(page, pageSize, keyword) {
    const url = '/api/v1/stores'; // 가맹점 API 엔드포인트
    const apiPage = page - 1; // 0-based 변환

    const params = {
        page: apiPage,
        size: pageSize,
        keyword: keyword || ''
    };

    try {
        const response = await axios.get(url, { params });
        const apiData = response.data;

        return {
            franchises: apiData.items || [],
            totalCount: apiData.totalCount || 0,
            currentPage: (apiData.page !== undefined ? apiData.page + 1 : page)
        };
    } catch (error) {
        console.error("[API Error] 가맹점 목록 조회 실패:", error.message);
        throw new Error("가맹점 API 서버와의 통신에 실패했습니다.");
    }
}

/**
 * [가맹점 주문 상세 조회]
 * @param {number} storeOrderId - 주문 ID
 * @returns {Promise<Object>}
 */
export async function getStoreOrderById(storeOrderId) {
    const url = `/api/v1/store-orders/${storeOrderId}`;

    try {
        const response = await axios.get(url);
        console.log(`주문 ${storeOrderId} 조회 성공:`, response.data);
        return response;
    } catch (error) {
        console.error("[API Error] 주문 상세 조회 실패:", error.message);
        throw new Error("주문 정보를 불러올 수 없습니다.");
    }
}

/**
 * [가맹점 주문 상태 업데이트]
 * @param {number} storeOrderId - 주문 ID
 * @param {string} newStatus - 새로운 상태 (CONFIRMED | REJECTED)
 * @returns {Promise<Object>}
 */
export async function updateStoreOrderStatus(storeOrderId, newStatus) {
    const url = `/api/v1/store-orders/${storeOrderId}/${newStatus}`;

    try {
        const response = await axios.patch(url, { status: newStatus });
        console.log(`주문 ${storeOrderId} 상태 업데이트 성공:`, response.data);
        return response.data;
    } catch (error) {
        console.error("[API Error] 주문 상태 업데이트 실패:", error.message);
        throw new Error("주문 상태 업데이트에 실패했습니다.");
    }
}