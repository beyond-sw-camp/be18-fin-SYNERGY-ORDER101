package com.synerge.order101.ai.model.service;

import com.synerge.order101.ai.model.dto.response.AiJobTriggerResponseDto;
import com.synerge.order101.ai.model.dto.response.AiMetricResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastListResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastResponseDto;
import com.synerge.order101.ai.model.dto.response.ForecastSeriesResponseDto;
import com.synerge.order101.ai.model.dto.response.RetrainResultResponseDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DemandForecastService {
    AiJobTriggerResponseDto triggerForecast(LocalDate targetWeek);
    RetrainResultResponseDto triggerRetrain();
    List<LocalDateTime> getSnapshotList();
    List<DemandForecastListResponseDto> getForecasts(LocalDate targetWeek);
    List<DemandForecastListResponseDto> getForecastsRange(LocalDate from, LocalDate to);
    List<ForecastSeriesResponseDto> getForecastSeries(LocalDate from, LocalDate to);
    DemandForecastResponseDto getForecast(Long id);
    AiMetricResponseDto getMetrics();
}



