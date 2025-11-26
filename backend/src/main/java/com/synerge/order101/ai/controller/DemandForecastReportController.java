package com.synerge.order101.ai.controller;

import com.synerge.order101.ai.model.dto.response.DemandForecastReportResponseDto;
import com.synerge.order101.ai.model.dto.response.TimeSeriesPointResponseDto;
import com.synerge.order101.ai.model.service.DemandForecastReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/demand-forecast")
@RequiredArgsConstructor
public class DemandForecastReportController {

    private final DemandForecastReportService reportService;

    // 전체 대시보드용 리포트
    @GetMapping("/report")
    public ResponseEntity<DemandForecastReportResponseDto> getReport(
            @RequestParam("targetWeek") LocalDate targetWeek
    ) {
        return ResponseEntity.ok(
                reportService.buildForecastReport(targetWeek)
        );
    }

    // SKU 상세 차트용 시계열 데이터
    @GetMapping("/product-series")
    public ResponseEntity<List<TimeSeriesPointResponseDto>> getProductSeries(
            @RequestParam("productId") Long productId,
            @RequestParam("targetWeek") LocalDate targetWeek
    ) {
        return ResponseEntity.ok(
                reportService.getProductSeries(productId, targetWeek)
        );
    }
}
