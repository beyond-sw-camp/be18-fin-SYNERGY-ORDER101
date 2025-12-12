import apiClient from '..'

/**
 * 발주 상태를 한글로 매핑하는 함수
 * @param {string} status - 백엔드 상태 코드
 * @returns {string} 한글 상태명
 */
export function mapPurchaseStatus(status) {
  const statusMap = {
    DRAFT_AUTO: '초안',
    SUBMITTED: '제출',
    CONFIRMED: '승인',
    REJECTED: '반려',
    CANCELLED: '취소',
    MANUAL: '일반',
    AUTO: '자동',
    SMART: '스마트',
  }
  return statusMap[status] || '알 수 없음'
}

export function purchaseStatusOptions() {
  return [
    { text: '전체', value: 'ALL' },
    { text: '초안', value: 'DRAFT_AUTO' },
    { text: '제출', value: 'SUBMITTED' },
    { text: '승인', value: 'CONFIRMED' },
    { text: '반려', value: 'REJECTED' },
    { text: '취소', value: 'CANCELLED' },
  ]
}

export function purchaseTypeOptions() {
  return [
    { text: '전체', value: 'ALL' },
    { text: '일반', value: 'MANUAL' },
    { text: '자동', value: 'AUTO' },
    { text: '스마트', value: 'SMART' },
  ]
}

// 가맹점 주문용 상태 옵션 (초안 없음)
export function franchiseOrderStatusOptions() {
  return [
    { text: '전체', value: 'ALL' },
    { text: '제출', value: 'SUBMITTED' },
    { text: '승인', value: 'CONFIRMED' },
    { text: '반려', value: 'REJECTED' },
    { text: '취소', value: 'CANCELLED' },
  ]
}

/**
 * [발주 상태 업데이트(승인 및 거절) ]
 * @param
 *
 */

export async function updatePurchaseStatus(purchaseId, status) {
  const url = `/api/v1/purchase-orders/${purchaseId}/${status}`
  try {
    const response = await apiClient.patch(url, {
      status: status,
    })
    return response.data
  } catch (error) {
    throw new Error('API 서버와의 통신에 실패했습니다.')
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
    sourceType: 'REGULAR', // 구분용
  }
}

/**
 * 스마트 발주 DTO를 공통 형식으로 변환
 * @param {object} smartOrder - 스마트 발주 DTO
 * @returns {object} 공통 형식의 발주 객체
 */
function mapSmartPurchase(smartOrder) {
  const unitPrice = smartOrder.unitPrice || smartOrder.price || 0
  const qty = smartOrder.recommendedOrderQty || 0
  return {
    purchaseId: smartOrder.id,
    poNo: `SMART-${smartOrder.id}`, // 스마트 발주는 PO 번호 생성
    supplierName: smartOrder.supplierName || '공급업체 정보 없음',
    requesterName: '자동 생성', // 스마트 발주는 자동 생성
    totalQty: qty,
    unitPrice: unitPrice,
    totalAmount: Number(unitPrice) * Number(qty),
    status: smartOrder.smartOrderStatus,
    orderType: 'SMART',
    requestedAt: smartOrder.snapshotAt || smartOrder.updatedAt,
    sourceType: 'SMART', // 구분용
    // 스마트 발주 전용 필드
    targetWeek: smartOrder.targetWeek,
    forecastQty: smartOrder.forecastQty,
    demandForecastId: smartOrder.demandForecastId,
  }
}

/**
 * [일반 발주 목록 조회]
 * @param {object} cond - 검색 조건 객체
 * @param {number} page 조회할 페이지 번호 (0부터 시작)
 * @param {number} perPage 페이지당 항목 수
 * @returns {Promise<object>} 발주 목록 및 페이지 정보
 */
export async function getRegularPurchases(cond, page, perPage) {
  const url = '/api/v1/purchase-orders'

  try {
    const params = {
      page: page || 0,
      size: perPage || 10,
    }

    // TradeSearchCondition 필드 매핑
    if (cond.types && cond.types.length > 0) params.types = cond.types.join(',')
    if (cond.statuses && cond.statuses.length > 0) params.statuses = cond.statuses.join(',')
    if (cond.vendorId) params.vendorId = cond.vendorId
    if (cond.searchText) params.searchText = cond.searchText
    if (cond.fromDate) params.fromDate = cond.fromDate
    if (cond.toDate) params.toDate = cond.toDate

    const response = await apiClient.get(url, { params })

    // 일반 발주 데이터를 공통 형식으로 변환
    const mappedContent = (response.data.content || []).map(mapRegularPurchase)

    return {
      ...response.data,
      content: mappedContent,
    }
  } catch (error) {
    throw error
  }
}

/**
 * [스마트 발주 목록 조회]
 * @param {string} [status] 상태 필터
 * @param {string} [from] 시작 날짜 (YYYY-MM-DD)
 * @param {string} [to] 종료 날짜 (YYYY-MM-DD)
 * @returns {Promise<object>} 발주 목록 (페이지네이션 없이 List 반환)
 */
export async function getSmartPurchases(status, from, to) {
  const url = '/api/v1/smart-orders'

  try {
    const params = {}
    if (status) params.status = status
    if (from) params.from = from
    if (to) params.to = to

    const response = await apiClient.get(url, { params })

    // 스마트 발주는 List로 반환
    const smartOrders = Array.isArray(response.data) ? response.data : []

    return {
      rawData: smartOrders, // 원본 데이터 추가
      content: smartOrders.map(mapSmartPurchase),
      totalElements: smartOrders.length,
      totalPages: 1,
    }
  } catch (error) {
    throw error
  }
}

/**
 * [스마트 발주를 po_number별로 그룹화]
 * @param {Array} smartOrders - 스마트 발주 원본 배열
 * @returns {Array} po_number별로 그룹화된 발주 목록
 */
export function groupSmartOrdersByPoNumber(smartOrders) {
  const grouped = {}

  smartOrders.forEach((order) => {
    // 백엔드에서 po_number가 아직 안 올라온 경우 임시로 supplierId 사용
    const poNumber = order.poNumber || order.po_number || `TEMP-${order.supplierId}`

    if (!grouped[poNumber]) {
      grouped[poNumber] = {
        poNumber: poNumber,
        supplierId: order.supplierId,
        supplierName: order.supplierName || '공급업체 정보 없음',
        items: [],
        smartOrderIds: [], // 같은 po_number의 모든 smart order ID
        totalQty: 0,
        totalAmount: 0,
        status: order.smartOrderStatus,
        requestedAt: order.snapshotAt || order.updatedAt,
        targetWeek: order.targetWeek,
      }
    }

    grouped[poNumber].items.push({
      productId: order.productId,
      productName: order.productName,
      recommendedOrderQty: order.recommendedOrderQty,
      forecastQty: order.forecastQty,
      unitPrice: order.unitPrice || order.price || 0,
    })

    grouped[poNumber].smartOrderIds.push(order.id)
    grouped[poNumber].totalQty += order.recommendedOrderQty || 0
    // 단가가 있으면 총액 누적
    const lineUnit = order.unitPrice || order.price || 0
    grouped[poNumber].totalAmount += Number(lineUnit) * (order.recommendedOrderQty || 0)
  })

  return Object.values(grouped).map((group, index) => ({
    purchaseId: `SMART-GROUP-${group.poNumber}`,
    poNo: group.poNumber,
    supplierName: group.supplierName,
    supplierId: group.supplierId,
    requesterName: '자동 생성',
    totalQty: group.items.length, // 품목 수 (items 배열 길이)
    totalAmount: group.totalAmount || 0,
    status: group.status,
    orderType: 'SMART',
    requestedAt: group.requestedAt,
    sourceType: 'SMART',
    items: group.items,
    smartOrderIds: group.smartOrderIds, // 승인 시 사용
    targetWeek: group.targetWeek,
  }))
}

/**
 * [스마트 발주 승인/반려]
 * @param {Array} smartOrderIds - 승인/반려할 스마트 발주 ID 배열
 * @param {string} status - 'CONFIRMED' or 'REJECTED'
 * @returns {Promise<object>} 승인/반려 결과
 */
export async function updateSmartOrderStatus(smartOrderIds, status) {
  try {
    const promises = smartOrderIds.map((id) =>
      apiClient.patch(`/api/v1/smart-orders/${id}/${status.toLowerCase()}`),
    )

    const results = await Promise.all(promises)

    return {
      success: true,
      count: results.length,
    }
  } catch (error) {
    throw new Error(`스마트 발주 ${status} 처리에 실패했습니다.`)
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
      toDate: endDate || null,
    }

    // 두 API를 병렬로 호출 (모든 데이터 가져오기)
    const [regularData, smartData] = await Promise.all([
      getRegularPurchases(regularCond, 0, 1000).catch((err) => {
        return { content: [], totalElements: 0, totalPages: 0 }
      }),
      getSmartPurchases(status, startDate, endDate).catch((err) => {
        return { rawData: [], content: [], totalElements: 0, totalPages: 0 }
      }),
    ])

    // 스마트 발주를 po_number별로 그룹화
    const groupedSmartOrders = groupSmartOrdersByPoNumber(smartData.rawData || [])

    // 두 결과를 병합
    const mergedContent = [...(regularData.content || []), ...groupedSmartOrders]

    // requestedAt 기준으로 내림차순 정렬 (최신순)
    mergedContent.sort((a, b) => {
      const dateA = new Date(a.requestedAt || 0)
      const dateB = new Date(b.requestedAt || 0)
      return dateB - dateA
    })

    // 클라이언트 사이드 페이지네이션
    const startIndex = page * perPage
    const endIndex = startIndex + perPage
    const paginatedContent = mergedContent.slice(startIndex, endIndex)

    // 통합 결과 반환
    const result = {
      content: paginatedContent,
      totalElements: mergedContent.length,
      totalPages: Math.ceil(mergedContent.length / perPage),
      size: perPage,
      number: page,
    }

    return result
  } catch (error) {
    throw new Error('API 서버와의 통신에 실패했습니다.')
  }
}

export async function getPurchaseDetail(id) {
  const url = `/api/v1/purchase-orders/${id}`

  try {
    const response = await apiClient.get(url, {})
    const detailObject = response.data
    return detailObject
  } catch (error) {
    throw new Error('API 서버와의 통신에 실패했습니다.')
  }
}

/**
 * [발주 승인용 최적화 조회 - 일반 발주 + 스마트 발주 통합]
 * SUBMITTED 상태의 발주만 조회
 * @param {number} page - 페이지 번호 (0-based)
 * @param {number} perPage - 페이지당 항목 수
 * @param {string} [searchText] - 검색어 (PO 번호, 공급사명)
 * @param {string} [orderType] - 발주 타입 필터
 *   - null/undefined: 전체 (스마트 + 일반 모두)
 *   - 'SMART': 스마트 발주만 (스마트 API만 호출)
 *   - 'AUTO': 자동 발주만 (일반 API 호출 후 AUTO 필터링)
 *   - 'MANUAL': 일반 발주만 (일반 API 호출 후 AUTO 제외)
 * @returns {Promise<object>} 통합 발주 목록
 */
export async function getPurchasesForApproval(page = 0, perPage = 10, searchText = null, orderType = null) {
  try {
    let mergedContent = []

    // 일반 발주 API 파라미터
    const regularParams = {
      page: 0,
      size: 1000,
      statuses: ['SUBMITTED'],
      searchText: searchText || null,
      sort: 'createdAt,desc',
    }

    // orderType에 따라 다른 API 호출 전략
    if (orderType === 'SMART') {
      // 스마트 발주만: 스마트 API만 호출
      const smartData = await getSmartPurchases('SUBMITTED', null, null).catch(() => ({ rawData: [] }))
      mergedContent = groupSmartOrdersByPoNumber(smartData.rawData || [])
      
    } else if (orderType === 'AUTO') {
      // 자동 발주만: 일반 API 호출 후 AUTO만 필터링
      const regularResponse = await apiClient.get('/api/v1/purchase-orders', { params: regularParams }).catch(() => ({ data: { content: [] } }))
      const regularOrders = (regularResponse.data.content || []).map(item => ({
        purchaseId: item.purchaseId,
        poNo: item.poNo,
        supplierName: item.supplierName,
        supplierId: item.supplierId,
        totalQty: item.totalQty,
        totalAmount: item.totalAmount,
        requestedAt: item.requestedAt,
        status: item.status,
        orderType: item.orderType || 'MANUAL',
        sourceType: item.sourceType || 'REGULAR',
        targetWeek: item.targetWeek,
        smartOrderIds: item.smartOrderIds,
      }))
      // AUTO만 필터링
      mergedContent = regularOrders.filter(item => item.orderType === 'AUTO')
      
    } else if (orderType === 'MANUAL') {
      // 일반 발주만: 일반 API 호출 후 AUTO 제외
      const regularResponse = await apiClient.get('/api/v1/purchase-orders', { params: regularParams }).catch(() => ({ data: { content: [] } }))
      const regularOrders = (regularResponse.data.content || []).map(item => ({
        purchaseId: item.purchaseId,
        poNo: item.poNo,
        supplierName: item.supplierName,
        supplierId: item.supplierId,
        totalQty: item.totalQty,
        totalAmount: item.totalAmount,
        requestedAt: item.requestedAt,
        status: item.status,
        orderType: item.orderType || 'MANUAL',
        sourceType: item.sourceType || 'REGULAR',
        targetWeek: item.targetWeek,
        smartOrderIds: item.smartOrderIds,
      }))
      // AUTO 제외 (MANUAL만)
      mergedContent = regularOrders.filter(item => item.orderType !== 'AUTO')
      
    } else {
      // 전체: 스마트 + 일반 모두 호출하여 병합
      const [regularResponse, smartData] = await Promise.all([
        apiClient.get('/api/v1/purchase-orders', { params: regularParams }).catch(() => ({ data: { content: [] } })),
        getSmartPurchases('SUBMITTED', null, null).catch(() => ({ rawData: [] })),
      ])

      const regularOrders = (regularResponse.data.content || []).map(item => ({
        purchaseId: item.purchaseId,
        poNo: item.poNo,
        supplierName: item.supplierName,
        supplierId: item.supplierId,
        totalQty: item.totalQty,
        totalAmount: item.totalAmount,
        requestedAt: item.requestedAt,
        status: item.status,
        orderType: item.orderType || 'MANUAL',
        sourceType: item.sourceType || 'REGULAR',
        targetWeek: item.targetWeek,
        smartOrderIds: item.smartOrderIds,
      }))

      const groupedSmartOrders = groupSmartOrdersByPoNumber(smartData.rawData || [])
      mergedContent = [...regularOrders, ...groupedSmartOrders]
    }

    // requestedAt 기준 내림차순 정렬
    mergedContent.sort((a, b) => {
      const dateA = new Date(a.requestedAt || 0)
      const dateB = new Date(b.requestedAt || 0)
      return dateB - dateA
    })

    // 클라이언트 사이드 페이지네이션
    const startIndex = page * perPage
    const endIndex = startIndex + perPage
    const paginatedContent = mergedContent.slice(startIndex, endIndex)

    return {
      content: paginatedContent,
      totalElements: mergedContent.length,
      totalPages: Math.ceil(mergedContent.length / perPage) || 1,
      size: perPage,
      number: page,
    }
  } catch (error) {
    console.error('발주 승인 목록 조회 실패:', error)
    throw error
  }
}
