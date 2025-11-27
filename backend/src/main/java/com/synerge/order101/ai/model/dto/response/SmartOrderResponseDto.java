package com.synerge.order101.ai.model.dto.response;

import com.synerge.order101.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class SmartOrderResponseDto {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long productId;
    private String productCode;
    private String productName;
    private Long demandForecastId;
    private LocalDate targetWeek;
    private Integer recommendedOrderQty;
    private Integer forecastQty;
    private OrderStatus smartOrderStatus;
    private LocalDateTime snapshotAt;
    private LocalDateTime updatedAt;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private String poNumber;
}
