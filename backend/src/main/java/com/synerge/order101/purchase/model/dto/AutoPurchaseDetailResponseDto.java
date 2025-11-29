package com.synerge.order101.purchase.model.dto;

import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AutoPurchaseDetailResponseDto {

    private Long purchaseId;

    private String poNo;

    private String supplierName;

    private String userName;

    private LocalDateTime requestedAt;

    private List<AutoPurchaseItemDto> purchaseItems;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AutoPurchaseItemDto{

        private Long detailId;
        private String productCode;
        private String productName;
        private BigDecimal unitPrice;
        private Integer orderQty;
        private Integer safetyQty;

        public static AutoPurchaseItemDto fromEntity(PurchaseDetail detail, int safetyQty){
            return AutoPurchaseItemDto.builder()
                    .detailId(detail.getPurchaseOrderLineId())
                    .productCode(detail.getProduct().getProductCode())
                    .productName(detail.getProduct().getProductName())
                    .unitPrice(detail.getUnitPrice())
                    .orderQty(detail.getOrderQty())
                    .safetyQty(safetyQty)
                    .build();
        }

    }
}
