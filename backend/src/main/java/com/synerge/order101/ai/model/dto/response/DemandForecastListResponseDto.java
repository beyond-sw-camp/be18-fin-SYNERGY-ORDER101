package com.synerge.order101.ai.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Builder
public class DemandForecastListResponseDto {
    private Long demandForecastId;
    private Long productId;
    private String productCode;
    private String productName;

    private LocalDate targetWeek;
    private Integer yPred;

    private LocalDateTime snapshotAt;
    private LocalDateTime updatedAt;
}
