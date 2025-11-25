package com.synerge.order101.ai.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SmartOrderDashboardResponseDto {
    private LocalDate targetWeek;
    private long totalRecommendedQty;
    private long totalForecastQty;
    private int draftCount;
    private int submittedCount;
}
