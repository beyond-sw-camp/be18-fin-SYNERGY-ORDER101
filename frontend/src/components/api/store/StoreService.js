import axios from 'axios';

/**
 * [가맹점 목록 조회]
 * @param {number} page - 페이지 번호 (1-based)
 * @param {number} pageSize - 페이지 크기
 * @param {string} keyword - 검색 키워드
 * @returns {Promise<{franchises: Array, totalCount: number, currentPage: number}>}
 */
export async function getFranchiseList(page, pageSize, keyword) {
    const url = '/api/v1/store-orders'; // 가맹점 API 엔드포인트
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

export async function getStoreList(page = 1, pageSize = 10, keyword = '') {
  try {
    const res = await axios.get('/api/v1/stores', {
      params: {
        page,
        numOfRows: pageSize,
        keyword: keyword
      }
    })

    const data = res.data

    return {
      stores: data.items || [],
      totalCount: data.totalCount || 0,
      currentPage: data.page || page
    }
  } catch (error) {
    console.error('[API Error] 가맹점 목록 조회 실패', error)
    throw error
  }
}