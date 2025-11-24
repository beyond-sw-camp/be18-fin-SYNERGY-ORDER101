import axios from 'axios'

/**
 * [상품 목록 조회]
 * 페이지네이션 및 검색 조건에 맞는 상품 목록을 백엔드로부터 조회합니다.
 *
 * @param {number} page       조회할 페이지 번호 (1부터 시작)
 * @param {number} pageSize   페이지당 항목 수 (numOfRows)
 * @param {string} keyword    검색어 (선택)
 * @param {number|null} largeCategoryId
 * @param {number|null} mediumCategoryId
 * @param {number|null} smallCategoryId
 *
 * @returns {Promise<{products: Array<object>, totalCount: number, currentPage: number}>}
 */
export async function getProductList(
  page,
  pageSize,
  keyword,
  largeCategoryId,
  mediumCategoryId,
  smallCategoryId,
) {
  const url = '/api/v1/products'

  const params = {
    page,
    numOfRows: pageSize,
  }

  if (keyword && keyword.trim() !== '') {
    params.keyword = keyword.trim()
  }
  if (largeCategoryId) {
    params.largeCategoryId = largeCategoryId
  }
  if (mediumCategoryId) {
    params.mediumCategoryId = mediumCategoryId
  }
  if (smallCategoryId) {
    params.smallCategoryId = smallCategoryId
  }

  try {
    const response = await axios.get(url, { params })
    const apiData = response.data

    return {
      products: apiData.items ?? [],
      totalCount: apiData.totalCount ?? 0,
      currentPage: apiData.page ?? page,
    }
  } catch (error) {
    console.error('[API Error] 상품 목록 조회 실패:', error.message, error.response)
    throw new Error('상품 API 서버와의 통신에 실패했습니다.')
  }
}

/**
 * 상품 상세 조회
 * @param {number} productId
 * @returns {Promise<object>} ProductRes
 */

export async function getProductDetail(productId) {
  const url = `/api/v1/products/${productId}`

  try {
    const { data } = await axios.get(url)

    if (data?.items && Array.isArray(data.items) && data.items.length > 0) {
      return data.items[0]
    }
  } catch (error) {
    console.error('[API Error] 상품 상세 조회 실패:', error.message, error.response)
    throw new Error('상품 상세 API 서버와의 통신에 실패했습니다.')
  }
}

export async function getProductInventory(productId, page = 1, numOfRows = 10) {
  const url = `/api/v1/products/${productId}/inventory`
  const params = { page, numOfRows }

  const { data } = await axios.get(url, { params })

  const detail = data.items?.[0]

  return {
    summary: detail?.summary ?? null,
    items: detail?.items ?? [],
    page: detail?.page ?? page,
    numOfRows: detail?.numOfRows ?? numOfRows,
    totalCount: detail?.totalCount ?? 0,
  }
}

export async function updateProduct(productId, requestDto, imageFile) {
  const fd = new FormData()

  fd.append('request', new Blob([JSON.stringify(requestDto)], { type: 'application/json' }))

  if (imageFile) {
    fd.append('image', imageFile)
  }

  const res = await axios.put(`/api/v1/products/${productId}`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return res.data.items?.[0]
}

export async function deleteProduct(productId) {
  const { data } = await axios.delete(`/api/v1/products/${productId}`)
  return data
}

export async function createProduct(request, imageFile) {
  const fd = new FormData()

  fd.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }))

  if (imageFile) fd.append('image', imageFile)

  const { data } = await axios.post('/api/v1/products', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return data.items?.[0]
}
