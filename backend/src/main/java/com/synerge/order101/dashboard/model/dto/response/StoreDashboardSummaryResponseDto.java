package com.synerge.order101.dashboard.model.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreDashboardSummaryResponseDto {

    private int pendingOrderCount;
    private int inTransitQtySum;
    private int inTransitShipmentCount;
    private int canceledThisMonthCount;
}