package com.synerge.order101.ai.controller;

import com.synerge.order101.ai.model.dto.response.AiJobTriggerResponseDto;
import com.synerge.order101.ai.model.dto.response.AiMetricResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastListResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastResponseDto;
import com.synerge.order101.ai.model.dto.response.ForecastSeriesResponseDto;
import com.synerge.order101.ai.model.dto.response.RetrainResultResponseDto;
import com.synerge.order101.ai.model.service.DemandForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DemandForecastController {

    private final DemandForecastService demandForecastService;

    // 수요 예측 실행 트리거
    @PostMapping("/internal/ai/forecasts")
    public ResponseEntity<AiJobTriggerResponseDto> triggerForecast(
            @RequestParam("targetWeek")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetWeek
    ){
        return ResponseEntity.ok(demandForecastService.triggerForecast(targetWeek));
    }

    // 수요 예측 목록 조회
    @GetMapping("/forecasts")
    public ResponseEntity<List<DemandForecastListResponseDto>> getForecasts(
            @RequestParam("targetWeek")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetWeek
    ) {
        return ResponseEntity.ok(demandForecastService.getForecasts(targetWeek));
    }

    //특정 기간 수요 예측 목록 조회
    @GetMapping("/forecasts/range")
    public ResponseEntity<List<DemandForecastListResponseDto>> getForecastsRange(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ResponseEntity.ok(demandForecastService.getForecastsRange(from, to));
    }

    // 차트용 시계열
    @GetMapping("/forecasts/series")
    public ResponseEntity<List<ForecastSeriesResponseDto>> getForecastSeries(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ResponseEntity.ok(demandForecastService.getForecastSeries(from, to));
    }

    // 수요 예측 상세 조회
    @GetMapping("/forecasts/{demandForecastId}")
    public ResponseEntity<DemandForecastResponseDto> getForecast(
            @PathVariable Long demandForecastId
    ) {
        return ResponseEntity.ok(demandForecastService.getForecast(demandForecastId));
    }

    // 스냅샷 리스트 조회
    @GetMapping("/forecasts/snapshots")
    public ResponseEntity<List<LocalDateTime>>getSnapshotList(){
        return ResponseEntity.ok(demandForecastService.getSnapshotList());
    }


    //모델 메트릭 조회
    @GetMapping("/internal/ai/metrics")
    public ResponseEntity<AiMetricResponseDto> getMetrics(){
        return ResponseEntity.ok(demandForecastService.getMetrics());
    }



    // 모델 재학습 트리거
    @PostMapping("/internal/ai/model/retrain")
    public ResponseEntity<RetrainResultResponseDto> triggerRetrain() {
        return ResponseEntity.ok(demandForecastService.triggerRetrain());
    }


}
