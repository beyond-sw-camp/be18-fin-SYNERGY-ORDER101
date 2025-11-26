package com.synerge.order101.ai.model.service;

import com.synerge.order101.ai.model.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface DemandForecastReportService {

    DemandForecastReportResponseDto buildForecastReport(LocalDate targetWeek);

    List<TimeSeriesPointResponseDto> getProductSeries(Long productId, LocalDate targetWeek);
}
