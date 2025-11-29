package com.synerge.order101.order.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FranchiseOrderDetailResponseDto {
    private Long orderId;
    private String orderNo;
    private String storeName;
    private LocalDateTime createdAt;
    private String orderStatus;
    private String shipmentStatus;

    private List<ItemDto> items;
    private List<ProgressDto> progress;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDto {
        private String sku;
        private String name;
        private int stock;
        private int qty;
        private BigDecimal price;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressDto {
        private String key;
        private String label;
        private LocalDateTime time;
        private boolean done;
        private String note;     // 배송번호
    }
}
