/**
 * ISO 8601 날짜 문자열을 "YYYY-MM-DD HH:MM" 형식으로 변환합니다.
 * @param {string} dateString - '2025-11-18T10:08:03.103352' 형태의 문자열
 * @returns {string} - '2025-11-18 10:08' 형태의 문자열
 */
export function formatDateTimeMinute(dateString) {
    if (!dateString) return '-';

    try {
        const date = new Date(dateString);

        // 날짜 구성 요소 추출
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        // 최종 형식으로 조합
        return `${year}-${month}-${day} ${hour}:${minute}`;

    } catch (error) {
        console.error("날짜 포맷팅 오류:", error);
        return dateString; // 오류 발생 시 원본 문자열 반환
    }
}

// 날짜 객체를 'YYYY-MM-DD' 형식의 문자열로 포맷하는 내부 헬퍼 함수
export const formatDateToString = (date) => {
    const year = date.getFullYear();
    // 월은 0부터 시작하므로 +1, 두 자리로 맞춤
    const month = String(date.getMonth() + 1).padStart(2, '0');
    // 일자를 두 자리로 맞춤
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

/**
 * 오늘 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @returns {string} 예: '2025-11-19'
 */
export const getTodayString = () => {
    return formatDateToString(new Date());
};

/**
 * 현재 날짜로부터 지정된 일수(days) 후의 날짜를 'YYYY-MM-DD' 형식의 문자열로 반환합니다.
 * @param {number} days - 현재 날짜에 더하거나(양수) 뺄(음수) 일수
 * @returns {string} 예: '2025-12-19' (30일 후)
 */
export const getFutureDateString = (days) => {
    const date = new Date();
    // setDate()를 사용하여 자동으로 월과 연도가 조정되도록 계산
    date.setDate(date.getDate() + days);

    return formatDateToString(date);
};

export const getPastDateString = (days) => {
    const date = new Date();
    // setDate()를 사용하여 자동으로 월과 연도가 조정되도록 계산
    date.setDate(date.getDate() - days);

    return formatDateToString(date);
};