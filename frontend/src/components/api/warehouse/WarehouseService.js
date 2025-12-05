import apiClient from '@/components/api'

// ============================================
// 창고 설정 (Configuration)
// ============================================

/**
 * 백엔드 API 사용 여부 플래그
 * - true: 백엔드 /api/v1/warehouses API를 호출하여 창고 목록 조회
 * - false: 기본 창고 데이터 사용 (본사 창고 1개)
 * 
 * TODO: 백엔드 API가 구현되면 true로 변경
 */
const USE_WAREHOUSE_API = false

/**
 * 기본 창고 데이터 (본사 창고)
 * 백엔드 API가 없을 때 사용되는 fallback 데이터
 */
export const DEFAULT_WAREHOUSE = {
  warehouseId: 1,
  warehouseCode: 'WH-HQ-001',
  warehouseName: '본사 창고',
  name: '본사 창고',
  address: '서울특별시 강남구',
  location: '서울특별시 강남구',
  contactNumber: '02-1234-5678',
  isActive: true
}

/**
 * 기본 창고 목록 (현재는 본사 창고 1개만 존재)
 */
const DEFAULT_WAREHOUSE_LIST = [DEFAULT_WAREHOUSE]

// ============================================
// 창고 API 함수
// ============================================

/**
 * [기본 창고 조회]
 * 현재 설정된 기본 창고(본사 창고)를 반환합니다.
 * 
 * @returns {object} 기본 창고 정보
 */
export function getDefaultWarehouse() {
  return { ...DEFAULT_WAREHOUSE }
}

/**
 * [창고 목록 조회]
 * 창고 목록을 조회합니다.
 * USE_WAREHOUSE_API 플래그에 따라 백엔드 API 또는 기본 데이터를 사용합니다.
 *
 * @param {number} page       조회할 페이지 번호 (1부터 시작)
 * @param {number} pageSize   페이지당 항목 수 (numOfRows)
 *
 * @returns {Promise<{warehouses: Array<object>, totalCount: number, currentPage: number}>}
 */
export async function getWarehouseList(page = 1, pageSize = 100) {
  // 백엔드 API를 사용하지 않는 경우 기본 데이터 반환
  if (!USE_WAREHOUSE_API) {
    return {
      warehouses: DEFAULT_WAREHOUSE_LIST,
      totalCount: DEFAULT_WAREHOUSE_LIST.length,
      currentPage: 1,
    }
  }

  // 백엔드 API 호출
  const url = '/api/v1/warehouses'

  const params = {
    page,
    numOfRows: pageSize,
  }

  try {
    const response = await apiClient.get(url, { params })
    const apiData = response.data

    // API 응답 구조에 맞게 처리
    const items = apiData.items ?? apiData ?? []
    
    // 프론트엔드에서 사용하기 쉽도록 필드 매핑
    const warehouses = items.map(w => ({
      warehouseId: w.warehouseId,
      warehouseCode: w.warehouseCode,
      name: w.warehouseName,  // warehouseName -> name 매핑
      warehouseName: w.warehouseName,
      address: w.address,
      location: w.address,    // address -> location 매핑 (호환성)
      contactNumber: w.contactNumber,
      isActive: w.isActive
    }))

    return {
      warehouses,
      totalCount: apiData.totalCount ?? warehouses.length,
      currentPage: apiData.page ?? page,
    }
  } catch (error) {
    // API 실패 시 기본 창고 데이터로 fallback
    return {
      warehouses: DEFAULT_WAREHOUSE_LIST,
      totalCount: DEFAULT_WAREHOUSE_LIST.length,
      currentPage: 1,
    }
  }
}

/**
 * [창고 재고 목록 조회]
 * 페이지네이션에 맞는 창고 재고 목록을 백엔드로부터 조회합니다.
 *
 * @param {number} page       조회할 페이지 번호 (1부터 시작)
 * @param {number} pageSize   페이지당 항목 수 (numOfRows)
 *
 * @returns {Promise<{inventories: Array<object>, totalCount: number, currentPage: number}>}
 *
 * @example 응답 항목 구조
 * {
 *   warehouseInventoryId: number,
 *   productCode: string,
 *   productCategory: string,
 *   productName: string,
 *   onHandQty: number,
 *   safetyQty: number
 * }
 */
export async function getInventoryList(page = 1, pageSize = 10) {
  const url = '/api/v1/warehouses/inventory'

  const params = {
    page,
    numOfRows: pageSize,
  }

  try {
    const response = await apiClient.get(url, { params })
    const apiData = response.data

    return {
      inventories: apiData.items ?? [],
      totalCount: apiData.totalCount ?? 0,
      currentPage: apiData.page ?? page,
    }
  } catch (error) {
    throw new Error('창고 재고 API 서버와의 통신에 실패했습니다.')
  }
}

/**
 * [재고 수량 변경 요청]
 * 특정 상품의 재고 수량을 변경합니다.
 *
 * @param {number} productId  상품 ID
 * @param {number} quantity   변경할 수량
 *
 * @returns {Promise<object>} 변경 결과
 */
export async function updateInventoryQuantity(productId, quantity) {
  const url = '/api/v1/warehouses/inventory/change'

  const requestDto = {
    productId,
    quantity,
  }

  try {
    const response = await apiClient.post(url, requestDto)
    return response.data
  } catch (error) {
    throw new Error('재고 수량 변경에 실패했습니다.')
  }
}
