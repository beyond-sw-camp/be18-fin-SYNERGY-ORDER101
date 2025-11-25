package com.synerge.order101.ai.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ForecastSeriesResponseDto {
    private LocalDate week;
    private Double actualQty;
    private Double forecastQty;
}
