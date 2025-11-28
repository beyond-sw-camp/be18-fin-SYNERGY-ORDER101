package com.synerge.order101.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true) // 부모 클래스의 필드도
public class TradeSearchCondition extends BaseSearchCondition {
    // 정산 유형: 체크된 값들(AR, AP)을 리스트로 받습니다.
    private List<String> types;

    // 상태: 체크된 값들(DRAFT, ISSUED, VOID)을 리스트로 받습니다.
    private List<String> statuses;

    private Long vendorId; // 공급사 ID

    // 검색어
    private String searchText; // ID 또는 공급사 이름 검색

}
