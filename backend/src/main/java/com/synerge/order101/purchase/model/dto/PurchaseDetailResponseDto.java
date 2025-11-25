package com.synerge.order101.purchase.model.dto;

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
        private BigDecimal unitPrice;
        private Integer orderQty;

        public static PurchaseItemDto fromEntity(PurchaseDetail detail){
            return PurchaseItemDto.builder()
                    .productCode(detail.getProduct().getProductCode())
                    .productName(detail.getProduct().getProductName())
                    .unitPrice(detail.getUnitPrice())
                    .orderQty(detail.getOrderQty())
                    .build();
        }

    }

}
