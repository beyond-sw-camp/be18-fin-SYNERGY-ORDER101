package com.synerge.order101.demandforecast.model.service;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.ai.model.dto.response.*;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.service.DemandForecastServiceImpl;
import com.synerge.order101.common.exception.CustomException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@DisplayName("DemandForecastServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemandForecastServiceImplTest {

    @Mock
    DemandForecastRepository demandForecastRepository;

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @InjectMocks
    DemandForecastServiceImpl demandForecastService;


    // 수요예측 트리거 관련

    @Test
    @Order(1)
    @DisplayName("triggerForecast")
    void triggerForecast_success() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);

        given(webClient.post()).willReturn(requestBodyUriSpec);

        given(requestBodyUriSpec.uri(any(Function.class)))
                .willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.toBodilessEntity())
                .willReturn(Mono.just(ResponseEntity.ok().build()));

        // when
        AiJobTriggerResponseDto result = demandForecastService.triggerForecast(targetWeek);

        // then
        assertThat(result.getJobType()).isEqualTo("FORECAST");
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(result.getMessage()).contains(targetWeek.toString());

        verify(webClient).post();
        verify(requestBodyUriSpec).uri(any(Function.class));
        verify(requestBodySpec).retrieve();
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    @Order(2)
    @DisplayName("triggerForecast")
    //Python AI 서버 오류 AI_SERVER_ERROR
    void triggerForecast_aiError() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);

        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(any(Function.class)))
                .willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);

        given(responseSpec.toBodilessEntity())
                .willThrow(new WebClientResponseException(
                        500, "SERVER_ERROR", null, null, null));

        // when & then
        assertThatThrownBy(() -> demandForecastService.triggerForecast(targetWeek))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AiErrorCode.AI_SERVER_ERROR.getMessage());
    }

    // 재학습 트리거
    @Test
    @Order(3)
    @DisplayName("triggerRetrain - Python AI 서버 재학습 요청 성공")
    void triggerRetrain_success() {
        // given
        RetrainResultResponseDto dummy = RetrainResultResponseDto.builder()
                .jobType("RETRAIN")
                .status("ACCEPTED")
                .mae(10.0)
                .mape(20.0)
                .smape(15.0)
                .bestIteration(800)
                .forecastGenerated(true)
                .message("Model retrain completed.")
                .build();

        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("/internal/ai/model/retrain"))
                .willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(RetrainResultResponseDto.class))
                .willReturn(Mono.just(dummy));

        // when
        RetrainResultResponseDto result = demandForecastService.triggerRetrain();

        // then
        assertThat(result.getJobType()).isEqualTo("RETRAIN");
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        // 파이썬 쪽 메시지 형태에 맞게 assert
        assertThat(result.getMessage()).contains("Model retrain completed");

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/internal/ai/model/retrain");
        verify(requestBodySpec).retrieve();
        verify(responseSpec).bodyToMono(RetrainResultResponseDto.class);
    }


    @Test
    @Order(4)
    @DisplayName("triggerRetrain - 오류")
    void triggerRetrain_aiError() {

        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("/internal/ai/model/retrain"))
                .willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);

        // ✔ 서비스가 실제 호출하는 부분: bodyToMono()
        given(responseSpec.bodyToMono(RetrainResultResponseDto.class))
                .willThrow(new RuntimeException("connection error"));

        assertThatThrownBy(() -> demandForecastService.triggerRetrain())
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AiErrorCode.AI_SERVER_ERROR.getMessage());
    }


    // Repo
    @Test
    @Order(5)
    @DisplayName("getSnapshotList")
    //스냅샷 리스트 조회 - 내부용
    void getSnapshotList_success() {
        // given
        LocalDateTime s1 = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime s2 = LocalDateTime.of(2025, 1, 8, 0, 0);

        DemandForecast df1 = DemandForecast.builder().snapshotAt(s1).build();
        DemandForecast df2 = DemandForecast.builder().snapshotAt(s2).build();
        DemandForecast df3 = DemandForecast.builder().snapshotAt(s1).build();

        given(demandForecastRepository
                .findDistinctBySnapshotAtIsNotNullOrderBySnapshotAtDesc())
                .willReturn(List.of(df1, df2, df3));

        // when
        List<LocalDateTime> list = demandForecastService.getSnapshotList();

        // then
        assertThat(list).containsExactlyInAnyOrder(s1, s2);
    }

    @Test
    @Order(6)
    @DisplayName("getForecasts")
    //특정 주차 수요예측 목록 조회 성공
    void getForecasts_success() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);
        DemandForecast df = DemandForecast.builder()
                .demandForecastId(1L)
                .targetWeek(targetWeek)
                .yPred(100)
                .snapshotAt(LocalDateTime.now())
                .build();

        given(demandForecastRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of(df));

        // when
        List<DemandForecastListResponseDto> result =
                demandForecastService.getForecasts(targetWeek);

        // then
        assertThat(result).hasSize(1);
        DemandForecastListResponseDto dto = result.get(0);
        assertThat(dto.getDemandForecastId()).isEqualTo(1L);
        assertThat(dto.getTargetWeek()).isEqualTo(targetWeek);
        assertThat(dto.getYPred()).isEqualTo(100);
    }

    @Test
    @Order(7)
    @DisplayName("getForecasts")
    //데이터 없을 때 FORECAST_NOT_FOUND
    void getForecasts_notFound() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);
        given(demandForecastRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> demandForecastService.getForecasts(targetWeek))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AiErrorCode.FORECAST_NOT_FOUND.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("getForecastsRange")
    //기간 수요예측 목록 조회 성공
    void getForecastsRange_success() {
        // given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);

        DemandForecast df = DemandForecast.builder()
                .demandForecastId(1L)
                .targetWeek(LocalDate.of(2025, 1, 6))
                .yPred(50)
                .snapshotAt(LocalDateTime.now())
                .build();

        given(demandForecastRepository.findByTargetWeekBetween(from, to))
                .willReturn(List.of(df));

        // when
        List<DemandForecastListResponseDto> result =
                demandForecastService.getForecastsRange(from, to);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getYPred()).isEqualTo(50);
    }

    @Test
    @Order(9)
    @DisplayName("getForecastsRange")
    //데이터 없을 때 FORECAST_NOT_FOUND
    void getForecastsRange_notFound() {
        // given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);

        given(demandForecastRepository.findByTargetWeekBetween(from, to))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> demandForecastService.getForecastsRange(from, to))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AiErrorCode.FORECAST_NOT_FOUND.getMessage());
    }

    @Test
    @Order(10)
    @DisplayName("getForecastSeries")
    //차트용 시계열 조회
    void getForecastSeries_success() {
        // given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);

        DemandForecast df = DemandForecast.builder()
                .targetWeek(LocalDate.of(2025, 1, 6))
                .actualOrderQty(80)
                .yPred(100)
                .build();

        given(demandForecastRepository.findByTargetWeekBetween(from, to))
                .willReturn(List.of(df));

        // when
        List<ForecastSeriesResponseDto> result =
                demandForecastService.getForecastSeries(from, to);

        // then
        assertThat(result).hasSize(1);
        ForecastSeriesResponseDto dto = result.get(0);
        assertThat(dto.getWeek()).isEqualTo(LocalDate.of(2025, 1, 6));
        assertThat(dto.getActualQty()).isEqualTo(80.0);
        assertThat(dto.getForecastQty()).isEqualTo(100.0);
    }

    @Test
    @Order(11)
    @DisplayName("getForecast")
    //단건 상세 조회 성공
    void getForecast_success() {
        // given
        DemandForecast df = DemandForecast.builder()
                .demandForecastId(1L)
                .targetWeek(LocalDate.of(2025, 1, 6))
                .yPred(120)
                .snapshotAt(LocalDateTime.now())
                .build();

        given(demandForecastRepository.findById(1L))
                .willReturn(java.util.Optional.of(df));

        // when
        DemandForecastResponseDto dto = demandForecastService.getForecast(1L);

        // then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTargetWeek()).isEqualTo(LocalDate.of(2025, 1, 6));
        assertThat(dto.getPredictedQty()).isEqualTo(120);
    }

    @Test
    @Order(12)
    @DisplayName("getForecast")
    //존재하지 않을 때 FORECAST_NOT_FOUND
    void getForecast_notFound() {
        // given
        given(demandForecastRepository.findById(1L))
                .willReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> demandForecastService.getForecast(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AiErrorCode.FORECAST_NOT_FOUND.getMessage());
    }

    @Test
    @Order(13)
    @DisplayName("getMetrics")
    // 데이터 없으면 0
    void getMetrics_noData() {
        // given
        given(demandForecastRepository.findAll())
                .willReturn(List.of());

        // when
        AiMetricResponseDto dto = demandForecastService.getMetrics();

        // then
        assertThat(dto.getMae()).isZero();
        assertThat(dto.getMape()).isZero();
        assertThat(dto.getSmape()).isZero();
    }

    @Test
    @Order(14)
    @DisplayName("getMetrics")
    //MAE / MAPE / SMAPE 계산
    void getMetrics_withData() {
        // given
        DemandForecast df1 = DemandForecast.builder()
                .actualOrderQty(100)
                .yPred(90)
                .build();

        DemandForecast df2 = DemandForecast.builder()
                .actualOrderQty(200)
                .yPred(220)
                .build();

        given(demandForecastRepository.findAll())
                .willReturn(List.of(df1, df2));

        // when
        AiMetricResponseDto dto = demandForecastService.getMetrics();

        // then - 범위로 대략 확인
        assertThat(dto.getMae()).isBetween(10.0, 20.0);
        assertThat(dto.getMape()).isGreaterThan(0.0);
        assertThat(dto.getSmape()).isGreaterThan(0.0);
    }
}