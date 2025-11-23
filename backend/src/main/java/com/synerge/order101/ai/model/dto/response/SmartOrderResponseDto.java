package com.synerge.order101.ai.model.dto.response;

import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@AllArgsConstructor
@Builder
public class SmartOrderResponseDto {
    private Long id;
    private Long supplierId;
    private Long productId;
    private Long demandForecastId;
    private LocalDate targetWeek;
    private Integer recommendedOrderQty;
    private Integer forecastQty;
    private OrderStatus smartOrderStatus;
    private LocalDateTime snapshotAt;
    private LocalDateTime updatedAt;
}
