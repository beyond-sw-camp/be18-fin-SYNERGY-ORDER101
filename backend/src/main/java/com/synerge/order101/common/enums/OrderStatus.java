package com.synerge.order101.common.enums;

import org.hibernate.query.Order;

import java.util.Random;

public enum OrderStatus {
    DRAFT_AUTO,        // 자동 생성 초안
    SUBMITTED,         // 담당자 확정 및 제출
    CONFIRMED,         // 관리자 승인
    REJECTED,          // 관리자 반려
    CANCELLED          // 담당자 취소
    ;

    // 💡 Random 인스턴스를 static으로 재사용하여 성능 최적화
    private static final Random RANDOM = new Random();

    public static OrderStatus getRandOrderStatus() {
        // 1. 모든 상수 배열을 가져옵니다.
        OrderStatus[] statuses = OrderStatus.values();

        // 2. 0부터 배열 길이 미만까지의 랜덤 인덱스를 생성합니다.
        // Math.random() 대신 Random.nextInt()를 사용하는 것이 더 권장됩니다.
        int randomIndex = RANDOM.nextInt(statuses.length);

        // 3. 해당 인덱스의 상수를 반환합니다.
        return statuses[randomIndex];
    }

}
