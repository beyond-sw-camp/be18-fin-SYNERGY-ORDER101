import apiClient from '..'

/**
 * 정산 목록 조회 (페이징)
 * @param {Object} params - 검색 조건
 * @param {number} page - 페이지 번호 (0-based)
 * @param {number} size - 페이지 크기
 * @returns {Promise<Object>} Spring Page 객체
 */
export async function getSettlements(params, page = 0, size = 20) {
  // types가 ALL이면 null, 아니면 배열로 변환하여 전달
  let typesParam = null
  if (params.types && params.types !== 'ALL') {
    typesParam = [params.types] // 'AR' or 'AP' -> ['AR'] or ['AP']
  }

  // vendorId가 ALL이면 null로 처리
  const vendorIdParam = params.vendorId === 'ALL' ? null : params.vendorId

  const response = await apiClient.get('/api/v1/settlements', {
    params: {
      // SettlementSearchCondition 필드
      fromDate: params.fromDate,
      toDate: params.toDate,
      types: typesParam, // null | ['AR'] | ['AP']
      vendorId: vendorIdParam, // 특정 업체 ID
      searchText: params.searchText,

      // Pageable 필드
      page: page,
      size: size,
      sort: 'createdAt,desc', // 생성일 내림차순
    },
    paramsSerializer: {
      indexes: null, // types[0]=AR 대신 types=AR로 전송
    },
  })

  return response.data
}

/**
 * 정산 리포트용 - 모든 데이터 조회 (집계용)
 * @param {Object} params - 검색 조건
 * @returns {Promise<Object>} 전체 데이터
 */
export async function getSettlementReport(params) {
  // 충분히 큰 페이지 크기로 전체 조회
  return getSettlements(params, 0, 10000)
}
