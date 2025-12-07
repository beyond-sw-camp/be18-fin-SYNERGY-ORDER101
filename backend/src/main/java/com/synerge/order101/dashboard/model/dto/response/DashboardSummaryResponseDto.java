package com.synerge.order101.dashboard.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardSummaryResponseDto {

    private long pendingPurchaseCount;
    private long lowStockSkuCount;
    private long draftAutoSmartOrderCount;
    private double recentForecastAccuracy;
}