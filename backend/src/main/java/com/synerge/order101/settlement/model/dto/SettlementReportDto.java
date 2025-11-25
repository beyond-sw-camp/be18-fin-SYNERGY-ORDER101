package com.synerge.order101.settlement.model.dto;

import com.synerge.order101.settlement.model.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementReportDto {
    private List<SettlementItemDto> settlementItems;

    private Integer totalCount;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementItemDto {
        private Long id;
        private String vendorName;
        private String vendorType;
        private Long vendorId;
        private String month;
        private LocalDate paymentDate;
        private BigDecimal productAmount;
        private BigDecimal unitPrice;
        private String status;
        private Integer count;
        private String memo;

    }

    public static SettlementItemDto from(Settlement settlement) {
        return SettlementItemDto.builder()
                .id(settlement.getSettlementId())
                .vendorName(settlement.getSupplier().getSupplierName() != null ?
                        settlement.getSupplier().getSupplierName() :
                        settlement.getStore().getStoreName())
                .vendorId(settlement.getSupplier().getSupplierId() != null ?
                        settlement.getSupplier().getSupplierId() :
                        settlement.getStore().getStoreId())
                .paymentDate(settlement.getSettledDate().toLocalDate())
                .productAmount(settlement.getProductsAmount())
                .unitPrice(settlement.getProductsAmount().divide(
                        BigDecimal.valueOf(settlement.getProductsQty()),
                        RoundingMode.HALF_UP))
                .count(settlement.getProductsQty())
                .memo(settlement.getNote())
                .build();
    }
}
