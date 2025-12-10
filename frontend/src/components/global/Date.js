/**
 * ISO 8601 날짜 문자열을 "YYYY-MM-DD HH:MM" 형식으로 변환합니다. (한국 표준시 기반)
 * @param {string} dateString - '2025-11-18T10:08:03.103352' 형태의 문자열
 * @returns {string} - '2025-11-18 10:08' 형태의 문자열
 */
export function formatDateTimeMinute(dateString) {
    if (!dateString) return '-';

    try {
        // UTC 시간을 한국 시간으로 변환
        const date = new Date(dateString);
        const kstOffset = 9 * 60;
        const utc = date.getTime() + (date.getTimezoneOffset() * 60000);
        const kstDate = new Date(utc + (kstOffset * 60000));

        // 날짜 구성 요소 추출
        const year = kstDate.getFullYear();
        const month = String(kstDate.getMonth() + 1).padStart(2, '0');
        const day = String(kstDate.getDate()).padStart(2, '0');
        const hour = String(kstDate.getHours()).padStart(2, '0');
        const minute = String(kstDate.getMinutes()).padStart(2, '0');

        // 최종 형식으로 조합
        return `${year}-${month}-${day} ${hour}:${minute}`;

    } catch (error) {
        console.error("날짜 포맷팅 오류:", error);
        return dateString;
    }
}

/**
 * 한국 표준시(KST) 기반으로 Date 객체를 'YYYY-MM-DD' 형식의 문자열로 포맷합니다.
 * @param {Date} date - Date 객체
 * @returns {string} - 'YYYY-MM-DD' 형식의 문자열
 */
export const formatDateToString = (date) => {
    const kstOffset = 9 * 60;
    const utc = date.getTime() + (date.getTimezoneOffset() * 60000);
    const kstDate = new Date(utc + (kstOffset * 60000));

    const year = kstDate.getFullYear();
    const month = String(kstDate.getMonth() + 1).padStart(2, '0');
    const day = String(kstDate.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

/**
 * 한국 표준시(KST) 기준 오늘 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @returns {string} 예: '2025-12-10'
 */
export const getTodayString = () => {
    return formatDateToString(new Date());
};

/**
 * 한국 표준시(KST) 기준으로 현재 날짜로부터 지정된 일수(days) 후의 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @param {number} days - 현재 날짜에 더하거나(양수) 뺄(음수) 일수
 * @returns {string} 예: '2025-12-19' (30일 후)
 */
export const getFutureDateString = (days) => {
    const now = new Date();
    const kstOffset = 9 * 60;
    const utc = now.getTime() + (now.getTimezoneOffset() * 60000);
    const kstDate = new Date(utc + (kstOffset * 60000));

    kstDate.setDate(kstDate.getDate() + days);
    return formatDateToString(kstDate);
};

/**
 * 한국 표준시(KST) 기준으로 현재 날짜로부터 지정된 일수(days) 전의 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @param {number} days - 현재 날짜에서 뺄 일수
 * @returns {string} 예: '2025-11-10' (30일 전)
 */
export const getPastDateString = (days) => {
    const now = new Date();
    const kstOffset = 9 * 60;
    const utc = now.getTime() + (now.getTimezoneOffset() * 60000);
    const kstDate = new Date(utc + (kstOffset * 60000));

    kstDate.setDate(kstDate.getDate() - days);
    return formatDateToString(kstDate);
};

/**
 * 한국 표준시(KST) 기준 현재 시간을 ISO 8601 형식으로 반환합니다.
 * 백엔드 API 요청 시 사용합니다.
 * @returns {string} 예: '2025-12-10T14:30:00'
 */
export const getKSTDateTime = () => {
    const now = new Date();
    const kstOffset = 9 * 60;
    const utc = now.getTime() + (now.getTimezoneOffset() * 60000);
    const kstDate = new Date(utc + (kstOffset * 60000));

    const year = kstDate.getFullYear();
    const month = String(kstDate.getMonth() + 1).padStart(2, '0');
    const day = String(kstDate.getDate()).padStart(2, '0');
    const hour = String(kstDate.getHours()).padStart(2, '0');
    const minute = String(kstDate.getMinutes()).padStart(2, '0');
    const second = String(kstDate.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
};

/**
 * 한국 표준시(KST) 기준 오늘 날짜를 'YYYY-MM-DD' 형식으로 반환합니다.
 * getTodayString()과 동일하며, 명확성을 위해 별도 제공합니다.
 * @returns {string} 예: '2025-12-10'
 */
export const getKSTDateString = () => {
    return getTodayString();
};