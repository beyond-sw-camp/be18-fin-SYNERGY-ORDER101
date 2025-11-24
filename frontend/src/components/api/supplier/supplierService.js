// supplierService.js

import axios from 'axios'

/**
 * [공급업체 목록 조회]
 * 페이지네이션 및 검색 조건에 맞는 공급업체 목록을 백엔드로부터 조회합니다.
 * * @param {number} page 조회할 페이지 번호 (1부터 시작한다고 가정하고, API에 맞게 0-기반으로 변환)
 * @param {number} pageSize 페이지당 항목 수
 * @param {string} keyword 검색어 (선택 사항)
 * @returns {Promise<{suppliers: Array<object>, totalCount: number, currentPage: number}>} 가공된 응답 객체
 * @throws {Error} API 통신 실패 시 에러 발생
 */
export async function getSupplierList(page, pageSize, keyword, status) {
  // API 엔드포인트는 /api/v1/suppliers 또는 유사한 경로라고 가정합니다.
  const url = '/api/v1/suppliers'

  // API는 0-기반 페이지 인덱스를 사용한다고 가정
  const apiPage = page - 1

  // 쿼리 파라미터 객체 구성
  const params = {
    page: apiPage,
    size: pageSize,
    keyword: keyword || '',
    status: status || '',
  }

  // 🚨 핵심: API 응답 구조를 Spring Page 객체 또는 유사한 DTO로 가정하고 구현합니다.
  try {
    const response = await axios.get(url, { params })
    const apiData = response.data // 서버에서 받은 원본 데이터 (Page 객체 예상)

    // 1. 데이터 가공 및 매핑 (필요하다면 여기서 필드 이름 매핑을 수행)
    const mappedSuppliers = apiData.items || [] // Spring Page 객체 가정

    // 2. fetchSuppliers가 요구하는 형태로 반환
    return {
      suppliers: mappedSuppliers,
      // totalElements를 totalCount로 이름 변경
      totalCount: apiData.totalCount || 0,
      // 현재 페이지는 API가 알려준 페이지 번호에 +1 (1-기반으로 변환)
      currentPage: apiData.number !== undefined ? apiData.page + 1 : page,
    }
  } catch (error) {
    console.error('[API Error] 공급업체 목록 조회 실패:', error.message, error.response)
    // 에러를 호출자에게 다시 던져서 컴포넌트의 catch 블록에서 처리하도록 합니다.
    throw new Error('공급업체 API 서버와의 통신에 실패했습니다.')
  }
}

export async function getSupplierDetail(supplierId, page, pageSize, keyword) {
  const url = `/api/v1/suppliers/${supplierId}`

  const params = {
    page,
    numOfRows: pageSize,
  }
  if (keyword && keyword.trim() !== '') {
    params.keyword = keyword.trim()
  }

  try {
    const response = await axios.get(url, { params })
    const apiData = response.data

    let detail

    if (apiData.items && Array.isArray(apiData.items) && apiData.items.length > 0) {
      detail = apiData.items[0]
    } else if (apiData.item) {
      detail = apiData.item
    } else {
      // 혹시 그냥 SupplierDetailRes 그대로 온다면
      detail = apiData
    }
    if (!detail || !detail.supplier) {
      throw new Error('잘못된 상세 응답 형식입니다.')
    }

    return {
      supplier: detail.supplier,
      products: detail.products ?? [],
      page: detail.page ?? page,
      pageSize: detail.numOfRows ?? pageSize,
      totalCount: detail.totalCount ?? (detail.products ? detail.products.length : 0),
    }
  } catch (error) {
    console.error('[API Error] 공급사 상세 조회 실패:', error.message, error.response)
    throw new Error('공급사 상세 API 서버와의 통신에 실패했습니다.')
  }
}
