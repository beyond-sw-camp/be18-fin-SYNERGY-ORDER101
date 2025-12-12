import apiClient from '..'

/**
 * 정산 목록 조회 (페이징)
 * @param {Object} params - 검색 조건
 * @param {number} page - 페이지 번호 (0-based)
 * @param {number} size - 페이지 크기
 * @returns {Promise<Object>} Spring Page 객체
 */
export async function getSettlements(params, page = 0, size = 20) {
  // types가 ALL이면 파라미터 제외, 아니면 단일 값으로 전달
  const typesParam = (params.types && params.types !== 'ALL') ? params.types : null

  // vendorId가 ALL이면 null로 처리
  const vendorIdParam = params.vendorId === 'ALL' ? null : params.vendorId

  // URL 쿼리 직접 구성 (배열 직렬화 문제 방지)
  const queryParts = []
  
  if (params.fromDate) queryParts.push(`fromDate=${params.fromDate}`)
  if (params.toDate) queryParts.push(`toDate=${params.toDate}`)
  if (params.searchText) queryParts.push(`searchText=${encodeURIComponent(params.searchText)}`)
  if (typesParam) queryParts.push(`types=${typesParam}`)
  if (vendorIdParam) queryParts.push(`vendorId=${vendorIdParam}`)
  
  queryParts.push(`page=${page}`)
  queryParts.push(`size=${size}`)
  queryParts.push(`sort=createdAt,desc`)

  const queryString = queryParts.join('&')
  
  // 디버깅용 로그
  console.log('Settlement API 요청:', {
    params,
    typesParam,
    vendorIdParam,
    url: `/api/v1/settlements?${queryString}`
  })
  
  const response = await apiClient.get(`/api/v1/settlements?${queryString}`)

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
