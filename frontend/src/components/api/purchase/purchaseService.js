
import axios from 'axios';

/**
 * [발주 상태 업데이트(승인 및 거절) ]
 * @param
 * 
 */

export async function updatePurchaseStatus(purchaseId, status) {

    const url = `/api/v1/purchase-orders/${purchaseId}/${status}`;
    console.log("발주 상태 업데이트 URL:", url);
    try {
        console.log("발주 상태 업데이트 요청 - purchaseId:", purchaseId, "status:", status);
        const response = await axios.patch(url, {
            status: status,
        });
        console.log("발주 상태 업데이트 응답:", response.data);
        return response.data;
    }
    catch (error) {
        console.error("[API Error] 발주 상태 업데이트 실패:", error.message, error.response);
        throw new Error("API 서버와의 통신에 실패했습니다.");
    }
}

/**
 * [발주 목록 조회]
 * 페이지네이션 및 검색 조건에 맞는 발주 목록을 백엔드로부터 조회합니다.
 * @param {number} page 조회할 페이지 번호 (0부터 시작)
 * @param {number} perPage 페이지당 항목 수
 * @param {string} [q] PO 번호, 공급업체, 요청자 등을 포함하는 검색어 (선택 사항)
 * @returns {Purchase<object>} 발주 목록 및 관련 페이지 정보를 포함하는 객체 (예: { purchases: [...], totalCount: 100 })
 * @throws {Error} API 통신 실패 시 에러 발생
 */
export async function getPurchases(page, perPage, q, status) {

    const url = '/api/v1/purchase-orders';

    try {
        // Axios의 'params' 옵션을 사용하여 쿼리 파라미터를 전송합니다.
        console.log("발주 목록 조회 요청 - page:", page, "perPage:", perPage, "q:", q, "status:", status);

        const response = await axios.get(url, {
            params: {
                keyword: q,
                status: status || '',
                page: page || 0,       // 0-based index
                size: perPage || 10,    // 페이지당 항목 수
            }
        });
        console.log("발주 목록 조회 응답:", response.data);
        // 백엔드 응답 구조가 발주 목록을 포함하는 객체라고 가정합니다.
        // 예: response.data === { purchases: [...], totalCount: 100, ... }
        // 호출하는 컴포넌트의 search 함수에서 사용된 구조와 일치하도록 응답을 반환합니다.
        return response.data;

    } catch (error) {
        // API 통신 실패 로그
        console.error("[API Error] 발주 목록 조회 실패:", error.message, error.response);

        // 에러 발생
        throw new Error("API 서버와의 통신에 실패했습니다.");
    }
}

export async function getPurchaseDetail(id) {

    const url = `/api/v1/purchase-orders/${id}`;

    try {
        const response = await axios.get(url, {});

        // ItemsResponseDto 객체에서 'purchaseDetail' 필드를 직접 추출
        // response.data가 { "purchaseDetail": { ... } } 구조라고 가정합니다.
        const detailObject = response.data;

        // 추출된 상세 객체 자체를 반환합니다.
        // 이렇게 하면 호출하는 컴포넌트에서 response.purchaseDetail 대신 detailObject를 바로 사용할 수 있습니다.
        return detailObject;

    } catch (error) {
        // API 통신 실패 로그를 남기고, 에러를 다시 던집니다.
        console.error("[API Error] 발주 상세 조회 실패:", error.message, error.response);
        throw new Error("API 서버와의 통신에 실패했습니다.");
    }
}

