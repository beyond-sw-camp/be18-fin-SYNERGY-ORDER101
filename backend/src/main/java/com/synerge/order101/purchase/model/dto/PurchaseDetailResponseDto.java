package com.synerge.order101.purchase.model.dto;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PurchaseDetailResponseDto {

    // 발주 ID
    private Long purchaseId;

    // 발주 타임
    private Purchase.OrderType orderType;

    // 발주 상태
    private OrderStatus orderStatus;

    // 발주 번호
    private String poNo;

    // 공급업체
    private String supplierName;

    // 마감일
    private String deadline;

    // 요청 담당자
    private String requesterName;

    // 요청일자
    private String requestedAt;

    //  품목 세부 정보
    private List<PurchaseItemDto> purchaseItems;

    @Getter
    @Builder
    public static class PurchaseItemDto{

        // 코드 이름 단가 수량 금액
        private String productCode;
        private String productName;
        private BigDecimal unitPrice;      // 판매가 (product.price)
        private BigDecimal purchasePrice;  // 공급가 (product_supplier.purchase_price)
        private Integer orderQty;

        public static PurchaseItemDto fromEntity(PurchaseDetail detail, BigDecimal purchasePrice){
            return PurchaseItemDto.builder()
                    .productCode(detail.getProduct().getProductCode())
                    .productName(detail.getProduct().getProductName())
                    .unitPrice(detail.getProduct().getPrice())  // 판매가
                    .purchasePrice(purchasePrice)                // 공급가
                    .orderQty(detail.getOrderQty())
                    .build();
        }

    }

}
