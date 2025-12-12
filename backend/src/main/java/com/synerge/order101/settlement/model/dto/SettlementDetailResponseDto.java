package com.synerge.order101.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SettlementDetailResponseDto {
    private String settlementNo;
    private String settlementType;  // AR or AP
    private String settlementStatus;
    private String vendorName;  // supplier or store name
    private LocalDateTime createdAt;
    private LocalDateTime settledDate;
    private BigDecimal settlementAmount;
    private Integer settlementQty;
    private LocalDateTime periodStart;  // 정산 기간 시작
    private LocalDateTime periodEnd;    // 정산 기간 종료
    private List<SettlementItemDto> items;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SettlementItemDto {
        private String productName;
        private String productCode;
        private Integer qty;
        private BigDecimal price;
        private BigDecimal amount;
    }
}
