package com.synerge.order101.purchase.model.dto;

import com.synerge.order101.common.enums.OrderStatus;
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

    private OrderStatus status;

    private List<AutoPurchaseItemDto> purchaseItems;

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AutoPurchaseItemDto{

        private Long detailId;
        private Long productId;
        private String productCode;
        private String productName;
        private BigDecimal unitPrice;
        private Integer orderQty;
        private Integer safetyQty;
        private Integer onHandQty;
        private Integer originalQty;

        public static AutoPurchaseItemDto fromEntity(PurchaseDetail detail, int safetyQty, int onHandQty, int originalQty) {
            return AutoPurchaseItemDto.builder()
                    .detailId(detail.getPurchaseOrderLineId())
                    .productId(detail.getProduct().getProductId())
                    .productCode(detail.getProduct().getProductCode())
                    .productName(detail.getProduct().getProductName())
                    .unitPrice(detail.getUnitPrice())
                    .orderQty(detail.getOrderQty())
                    .safetyQty(safetyQty)
                    .onHandQty(onHandQty)
                    .originalQty(originalQty)
                    .build();
        }

    }
}
