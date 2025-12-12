package com.synerge.order101.settlement.model.dto;

import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.settlement.model.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class SettlementSummaryDto {

    Long settlementId;
    SettlementType settlementType;
    String settlementNo;
    String supplierName;
    String storeName;
    LocalDateTime createdAt;
    LocalDateTime settledAt;
    BigDecimal settlementAmount;
    Integer settlementQty;
    String settlementStatus;

    public static SettlementSummaryDto fromEntity(Settlement settlement){

        if(settlement == null) {
            return null;
        }

        // LAZY 로딩된 엔터티 안전하게 접근
        String supplierName = null;
        String storeName = null;
        
        try {
            if (settlement.getSupplier() != null) {
                supplierName = settlement.getSupplier().getSupplierName();
            }
        } catch (Exception e) {
            supplierName = "삭제된 공급사";
        }
        
        try {
            if (settlement.getStore() != null) {
                storeName = settlement.getStore().getStoreName();
            }
        } catch (Exception e) {
            storeName = "삭제된 가맹점";
        }

        return SettlementSummaryDto.builder()
                .settlementId(settlement.getSettlementId())
                .settlementNo(settlement.getSettlementNo())
                .settlementType(settlement.getSettlementType())
                .settledAt(settlement.getSettledDate())
                .supplierName(supplierName)
                .storeName(storeName)
                .createdAt(settlement.getCreatedAt())
                .settlementAmount(settlement.getProductsAmount())
                .settlementQty(settlement.getProductsQty())
                .settlementStatus(settlement.getSettlementStatus().name())
                .build();
    }
}
