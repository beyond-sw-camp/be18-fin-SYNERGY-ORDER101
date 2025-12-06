import apiClient from '..'

export async function getFranchiseOrderList(page, pageSize, searchParams) {
  const url = '/api/v1/store-orders' // 가맹점 API 엔드포인트
  const apiPage = page - 1 // 0-based 변환

  const params = {
    // 검색 조건
    fromDate: searchParams.fromDate || null,
    toDate: searchParams.toDate || null,
    vendorId: searchParams.vendorId || searchParams.storeId || null, // 가맹점 ID (vendorId 우선)
    statuses: searchParams.statuses || null,
    searchText: searchParams.searchText || null,

    // Pageable 필드
    page: apiPage,
    size: pageSize,
    sort: 'createdAt,desc', // 생성일 내림차순
  }

  try {
    const response = await apiClient.get(url, { params })
    const apiData = response.data

    // Settlement과 동일한 구조로 반환 (Spring Page 객체)
    return apiData
  } catch (error) {
    throw new Error('가맹점 API 서버와의 통신에 실패했습니다.')
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
  const url = '/api/v1/stores' // 가맹점 API 엔드포인트
  const apiPage = page - 1 // 0-based 변환

  const params = {
    page: apiPage,
    size: pageSize,
    keyword: keyword || '',
  }

  try {
    const response = await apiClient.get(url, { params })
    const apiData = response.data

    return {
      franchises: apiData.items || [],
      totalCount: apiData.totalCount || 0,
      // API returns `page` as 1-based in our backend samples, so use it directly.
      // Previously we added +1 which caused the UI to show page 2 by default.
      currentPage: apiData.page !== undefined ? apiData.page : page,
    }
  } catch (error) {
    throw new Error('가맹점 API 서버와의 통신에 실패했습니다.')
  }
}

/**
 * [가맹점 주문 상세 조회]
 * @param {number} storeOrderId - 주문 ID
 * @returns {Promise<Object>}
 */
export async function getStoreOrderById(storeOrderId) {
  const url = `/api/v1/store-orders/${storeOrderId}`

  try {
    const response = await apiClient.get(url)
    return response
  } catch (error) {
    throw new Error('주문 정보를 불러올 수 없습니다.')
  }
}

/**
 * [가맹점 주문 상태 업데이트]
 * @param {number} storeOrderId - 주문 ID
 * @param {string} newStatus - 새로운 상태 (CONFIRMED | REJECTED)
 * @returns {Promise<Object>}
 */
export async function updateStoreOrderStatus(storeOrderId, newStatus) {
  const url = `/api/v1/store-orders/${storeOrderId}/${newStatus}`

  try {
    const response = await apiClient.patch(url, { status: newStatus })
    return response.data
  } catch (error) {
    throw new Error('주문 상태 업데이트에 실패했습니다.')
  }
}

/**
 * [주문 승인 전 창고 재고 확인]
 * @param {number} storeOrderId - 주문 ID
 * @returns {Promise<{hasEnoughStock: boolean, insufficientItems: Array}>}
 */
export async function checkStockForOrder(storeOrderId) {
  const url = `/api/v1/store-orders/${storeOrderId}/stock-check`

  try {
    const response = await apiClient.get(url)
    return response.data
  } catch (error) {
    throw new Error('재고 확인에 실패했습니다.')
  }
}
