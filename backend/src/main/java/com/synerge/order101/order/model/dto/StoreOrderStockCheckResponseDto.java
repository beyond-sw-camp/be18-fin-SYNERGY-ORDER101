package com.synerge.order101.order.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StoreOrderStockCheckResponseDto {
    
    private boolean hasEnoughStock;  // 모든 품목의 재고가 충분한지 여부
    private List<InsufficientStockItem> insufficientItems;  // 재고 부족 품목 목록

    @Getter
    @Builder
    public static class InsufficientStockItem {
        private Long productId;
        private String productName;
        private String productCode;
        private Integer requestedQty;    // 요청 수량
        private Integer availableQty;    // 창고 가용 재고
        private Integer shortageQty;     // 부족 수량
    }
}
