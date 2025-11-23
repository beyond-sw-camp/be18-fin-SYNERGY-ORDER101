package com.synerge.order101.ai.service;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.ai.model.dto.response.*;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.repository.DemandForecastRepository;
import com.synerge.order101.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandForecastService {
    private final DemandForecastRepository demandForecastRepository;
    private final WebClient webClient;

    @Transactional
    public AiJobTriggerResponseDto triggerForecast(LocalDate targetWeek) {

        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/ai/forecasts")         //파이썬
                            .queryParam("target_week", targetWeek.toString())
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            throw new CustomException(AiErrorCode.AI_SERVER_ERROR);
        } catch (Exception ex) {
            throw new CustomException(AiErrorCode.AI_SERVER_ERROR);
        }

        return AiJobTriggerResponseDto.builder()
                .jobType("FORECAST")
                .status("ACCEPTED")
                .message("Python AI 서버에 예측 요청 전송 완료. targetWeek = " + targetWeek)
                .build();
    }


    //모델 재학습
    @Transactional
    public AiJobTriggerResponseDto triggerRetrain(){
        try {
            webClient.post()
                    .uri("/internal/ai/model/retrain") //파이썬 엔드포인트
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            throw new CustomException(AiErrorCode.AI_SERVER_ERROR);
        } catch (Exception ex) {
            throw new CustomException(AiErrorCode.AI_SERVER_ERROR);
        }

        return AiJobTriggerResponseDto.builder()
                .jobType("RETRAIN")
                .status("ACCEPTED")
                .message("Python AI 서버에 재학습 요청 전송 완료.")
                .build();
    }


    // 스냅샷 리스트
    public List<LocalDateTime> getSnapshotList() {
        return demandForecastRepository
                .findDistinctBySnapshotAtIsNotNullOrderBySnapshotAtDesc()
                .stream()
                .map(DemandForecast::getSnapshotAt)
                .distinct()
                .toList();
    }


    //특정 주차 수요 예측 목록
    public List<DemandForecastListResponseDto> getForecasts(LocalDate targetWeek) {
        List<DemandForecast> list = demandForecastRepository.findByTargetWeek(targetWeek);

        if (list.isEmpty()) {
            throw new CustomException(AiErrorCode.FORECAST_NOT_FOUND);
        }

        return list.stream()
                .map(this::toListDto)
                .toList();
    }

    // 특정 기간 수요예측 목록
    public List<DemandForecastListResponseDto> getForecastsRange(LocalDate from, LocalDate to) {
        List<DemandForecast> list =
                demandForecastRepository.findByTargetWeekBetween(from, to);

        if (list.isEmpty()) {
            throw new CustomException(AiErrorCode.FORECAST_NOT_FOUND);
        }

        return list.stream()
                .map(this::toListDto)
                .toList();
    }

    //차트용 시계열 -  판매 내역 + 예측
    public List<ForecastSeriesResponseDto> getForecastSeries(LocalDate from, LocalDate to) {
        List<DemandForecast> list =
                demandForecastRepository.findByTargetWeekBetween(from, to);

        return list.stream()
                .map(df -> ForecastSeriesResponseDto.builder()
                        .week(df.getTargetWeek())
                        .actualQty(df.getActualOrderQty() != null
                                ? df.getActualOrderQty().doubleValue()
                                : null)
                        .forecastQty(df.getYPred() != null
                                ? df.getYPred().doubleValue()
                                : null)
                        .build())
                .toList();
    }

    // 단건 상세 조회
    public DemandForecastResponseDto getForecast(Long id) {
        DemandForecast entity = demandForecastRepository.findById(id)
                .orElseThrow(() -> new CustomException(AiErrorCode.FORECAST_NOT_FOUND));
        return toDetailDto(entity);
    }

    // 모델 매트릭 조회
    public AiMetricResponseDto getMetrics() {
        List<DemandForecast> all = demandForecastRepository.findAll();

        double mae = 0.0;
        double mape = 0.0;
        double smape = 0.0;
        int n = 0;

        for (DemandForecast df : all) {
            if (df.getActualOrderQty() == null || df.getYPred() == null) continue;
            double y = df.getActualOrderQty();
            double yhat = df.getYPred();

            double absErr = Math.abs(y - yhat);
            mae += absErr;
            if (y != 0) {
                mape += absErr / Math.abs(y);
            }
            smape += absErr / ((Math.abs(y) + Math.abs(yhat)) / 2.0);
            n++;
        }

        if (n == 0) {
            return AiMetricResponseDto.builder()
                    .mae(0.0).mape(0.0).smape(0.0)
                    .build();
        }

        mae /= n;
        mape = mape / n * 100.0;
        smape = smape / n * 100.0;

        return AiMetricResponseDto.builder()
                .mae(mae)
                .mape(mape)
                .smape(smape)
                .build();
    }

    private DemandForecastListResponseDto toListDto(DemandForecast df) {
        var product = df.getProduct();

        return DemandForecastListResponseDto.builder()
                .demandForecastId(df.getDemandForecastId())
                .productId(product != null ? product.getProductId() : null)
                .productCode(product != null ? product.getProductCode() : null)
                .productName(product != null ? product.getProductName() : null)
                .targetWeek(df.getTargetWeek())
                .yPred(df.getYPred())
                .snapshotAt(df.getSnapshotAt())
                .updatedAt(df.getUpdatedAt())
                .build();
    }

    private DemandForecastResponseDto toDetailDto(DemandForecast df) {
        Long warehouseId = (df.getWarehouse() != null)
                ? df.getWarehouse().getWarehouseId()
                : null;

        Long storeId = (df.getStore() != null)
                ? df.getStore().getStoreId()
                : null;

        Long productId = (df.getProduct() != null)
                ? df.getProduct().getProductId()
                : null;

        return DemandForecastResponseDto.builder()
                .id(df.getDemandForecastId())
                .warehouseId(warehouseId)
                .storeId(storeId)
                .productId(productId)
                .targetWeek(df.getTargetWeek())
                .predictedQty(df.getYPred())
                .snapshotAt(df.getSnapshotAt())
                .updatedAt(df.getUpdatedAt())
                .build();
    }
}



