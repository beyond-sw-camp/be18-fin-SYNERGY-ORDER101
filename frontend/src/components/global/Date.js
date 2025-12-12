/**
 * ISO 8601 날짜 문자열을 "YYYY-MM-DD HH:MM" 형식으로 변환합니다. (브라우저 현지 시간 기반)
 * @param {string} dateString - '2025-11-18T10:08:03.103352' 형태의 문자열
 * @returns {string} - '2025-11-18 10:08' 형태의 문자열
 */
export function formatDateTimeMinute(dateString) {
    if (!dateString) return '-';

    try {
        // UTC 시간을 브라우저 현지 시간으로 자동 변환
        const date = new Date(dateString);

        // 날짜 구성 요소 추출 (브라우저 로케일 자동 적용)
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        // 최종 형식으로 조합
        return `${year}-${month}-${day} ${hour}:${minute}`;

    } catch (error) {
        console.error("날짜 포맷팅 오류:", error);
        return dateString;
    }
}

/**
 * 브라우저 현지 시간 기반으로 Date 객체를 'YYYY-MM-DD' 형식의 문자열로 포맷합니다.
 * @param {Date} date - Date 객체
 * @returns {string} - 'YYYY-MM-DD' 형식의 문자열
 */
export const formatDateToString = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

/**
 * 브라우저 현지 시간 기준 오늘 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @returns {string} 예: '2025-12-10'
 */
export const getTodayString = () => {
    return formatDateToString(new Date());
};

/**
 * 브라우저 현지 시간 기준으로 현재 날짜로부터 지정된 일수(days) 후의 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @param {number} days - 현재 날짜에 더하거나(양수) 뺄(음수) 일수
 * @returns {string} 예: '2025-12-19' (30일 후)
 */
export const getFutureDateString = (days) => {
    const now = new Date();
    now.setDate(now.getDate() + days);
    return formatDateToString(now);
};

/**
 * 브라우저 현지 시간 기준으로 현재 날짜로부터 지정된 일수(days) 전의 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @param {number} days - 현재 날짜에서 뺄 일수
 * @returns {string} 예: '2025-11-10' (30일 전)
 */
export const getPastDateString = (days) => {
    const now = new Date();
    now.setDate(now.getDate() - days);
    return formatDateToString(now);
};

/**
 * 브라우저 현지 시간 기준 현재 시간을 ISO 8601 형식으로 반환합니다.
 * 백엔드 API 요청 시 사용합니다.
 * @returns {string} 예: '2025-12-10T14:30:00'
 */
export const getLocalDateTime = () => {
    const now = new Date();
    
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hour = String(now.getHours()).padStart(2, '0');
    const minute = String(now.getMinutes()).padStart(2, '0');
    const second = String(now.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
};

/**
 * 브라우저 현지 시간 기준 오늘 날짜를 'YYYY-MM-DD' 형식으로 반환합니다.
 * getTodayString()과 동일하며, 명확성을 위해 별도 제공합니다.
 * @returns {string} 예: '2025-12-10'
 */
export const getLocalDateString = () => {
    return getTodayString();
};

// 하위 호환성을 위한 별칭 (deprecated)
export const getKSTDateTime = getLocalDateTime;
export const getKSTDateString = getLocalDateString;