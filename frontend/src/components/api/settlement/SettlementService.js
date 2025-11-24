import axios from 'axios';

/**
 * 정산 목록 조회 (페이징)
 * @param {Object} params - 검색 조건
 * @param {number} page - 페이지 번호 (0-based)
 * @param {number} size - 페이지 크기
 * @returns {Promise<Object>} Spring Page 객체
 */
export async function getSettlements(params, page = 0, size = 20) {
    const response = await axios.get('/api/v1/settlements', {
        params: {
            // SettlementSearchCondition 필드
            fromDate: params.fromDate,
            toDate: params.toDate,
            types: params.types === 'ALL' ? null : params.types,  // null | 'AR' | 'AP'
            vendorId: params.vendorId,              // 특정 업체 ID
            searchText: params.searchText,


            // Pageable 필드
            page: page,
            size: size,
            sort: 'createdAt,desc'                  // 생성일 내림차순
        }
    });

    return response.data;
}

/**
 * 정산 리포트용 - 모든 데이터 조회 (집계용)
 * @param {Object} params - 검색 조건
 * @returns {Promise<Object>} 전체 데이터
 */
export async function getSettlementReport(params) {
    // 충분히 큰 페이지 크기로 전체 조회
    return getSettlements(params, 0, 10000);
}
