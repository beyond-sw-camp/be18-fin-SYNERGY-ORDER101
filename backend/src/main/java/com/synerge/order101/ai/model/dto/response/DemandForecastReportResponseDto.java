package com.synerge.order101.ai.model.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandForecastReportResponseDto {
    private List<TimeSeriesPointResponseDto> timeseries;
    private List<CategoryMetricResponseDto> categoryMetrics;
    private List<ProductDetailRowResponseDto> details;
}