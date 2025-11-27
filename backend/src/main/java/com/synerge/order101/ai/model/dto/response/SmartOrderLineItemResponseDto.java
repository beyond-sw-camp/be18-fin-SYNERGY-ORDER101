package com.synerge.order101.ai.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SmartOrderLineItemResponseDto {
    private Long smartOrderId;

    private Long productId;
    private String productCode;
    private String productName;

    private Integer forecastQty;
    private Integer recommendedOrderQty;

    private BigDecimal unitPrice;
    private BigDecimal lineAmount;

    private boolean manualEdited;
}
