package com.synerge.order101.ai.model.service;

import com.synerge.order101.ai.model.dto.response.CategoryMetricResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastReportResponseDto;
import com.synerge.order101.ai.model.dto.response.ProductDetailRowResponseDto;
import com.synerge.order101.ai.model.dto.response.TimeSeriesPointResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.repository.SmartOrderRepository;
import com.synerge.order101.common.cache.CachedPerf;
import com.synerge.order101.product.model.entity.CategoryLevel;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductCategory;
import com.synerge.order101.product.model.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandForecastReportServiceImpl implements DemandForecastReportService {

    private final DemandForecastRepository demandForecastRepository;
    private final ProductRepository productRepository;
    private final SmartOrderRepository smartOrderRepository;

    @Override
    @Cacheable(value = "forecastReport", key = "#targetWeek")
    @CachedPerf("ForecastReport")
    public DemandForecastReportResponseDto buildForecastReport(LocalDate targetWeek) {

        LocalDate from = targetWeek.minusMonths(11).withDayOfMonth(1);
        LocalDate to = targetWeek.plusMonths(1).withDayOfMonth(1)
                .plusMonths(1).minusDays(1);

        List<DemandForecast> dfList =
                demandForecastRepository.findByTargetWeekBetween(from, to);

        // Lazy Proxy 초기화 (직렬화 문제 해결)
        dfList.forEach(df -> {
            var p = df.getProduct();
            if (p != null) {
                p.getProductId();
                var cat = p.getProductCategory();
                if (cat != null) {
                    cat.getCategoryName();
                    if (cat.getParent() != null) {
                        cat.getParent().getCategoryName();
                    }
                }
            }
        });

        // 1) Timeseries (좌측 그래프)
        Map<LocalDate, List<DemandForecast>> groupedByWeek = dfList.stream()
                .collect(Collectors.groupingBy(DemandForecast::getTargetWeek));

        List<TimeSeriesPointResponseDto> timeseries =
                groupedByWeek.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            LocalDate week = entry.getKey();
                            List<DemandForecast> rows = entry.getValue();

                            int totalPred = (int) Math.round(
                                    rows.stream()
                                            .map(DemandForecast::getYPred)
                                            .filter(v -> v != null)
                                            .mapToDouble(v -> v)
                                            .sum()
                            );


                            List<Integer> actualList = rows.stream()
                                    .map(DemandForecast::getActualOrderQty)
                                    .filter(v -> v != null)
                                    .toList();

                            Integer totalActual = actualList.isEmpty()
                                    ? null
                                    : actualList.stream().mapToInt(v -> v).sum();

                            if (totalActual == null) {
                                return null;
                            }

                            return TimeSeriesPointResponseDto.builder()
                                    .date(week.toString())
                                    .forecast((double) totalPred)
                                    .actual(totalActual)
                                    .build();
                        })
                        .filter(dto -> dto != null)
                        .toList();




        // 2) Category Metrics (중분류 기준)
        Map<String, List<DemandForecast>> grouped = dfList.stream()
                .map(df -> {
                    String midCat = extractMiddleCategoryName(df.getProduct().getProductCategory());
                    return Map.entry(midCat, df);
                })
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        List<CategoryMetricResponseDto> categoryMetrics =
                grouped.entrySet().stream()
                        .map(entry -> {

                            double avgMAPE = entry.getValue().stream()
                                    .filter(df -> df.getActualOrderQty() != null && df.getActualOrderQty() > 0)
                                    .filter(df -> df.getYPred() != null)
                                    .mapToDouble(df -> safeMAPE(df.getActualOrderQty(), df.getYPred()))
                                    .average()
                                    .orElse(Double.NaN);

                            return CategoryMetricResponseDto.builder()
                                    .category(entry.getKey())
                                    .metric(Double.isNaN(avgMAPE) ? null : avgMAPE)
                                    .build();
                        })
                        .filter(c -> c.getMetric() != null)
                        .sorted(Comparator.comparing(CategoryMetricResponseDto::getCategory))
                        .toList();



        // 3) 상세 테이블 (최근 targetWeek 기준)
        LocalDate fromWeek = targetWeek.withDayOfMonth(1);
        LocalDate toWeek = targetWeek.withDayOfMonth(1).plusMonths(1).minusDays(1);

//        List<DemandForecast> latest =
//                demandForecastRepository.findByTargetWeekBetween(fromWeek, toWeek);
        // 이번 달 데이터 중 가장 최신 날짜 구하기
        LocalDate maxActualWeek = demandForecastRepository
                .findByTargetWeekBetween(fromWeek, toWeek)
                .stream()
                .filter(df -> df.getActualOrderQty() != null)
                .map(DemandForecast::getTargetWeek)
                .max(LocalDate::compareTo)
                .orElse(null);

        // 최신 주차 데이터만 필터링
        List<DemandForecast> latest =
                demandForecastRepository.findByTargetWeekBetween(fromWeek, toWeek)
                        .stream()
                        .filter(df -> df.getTargetWeek().equals(maxActualWeek))
                        .toList();


        List<ProductDetailRowResponseDto> details = latest.stream()
                .map(df -> {
                    Product p = df.getProduct();
                    SmartOrder so = smartOrderRepository.findTopByDemandForecast_DemandForecastId(df.getDemandForecastId())
                            .orElse(null);

                    return ProductDetailRowResponseDto.builder()
                            .productId(p.getProductId())
                            .sku(p.getProductCode())
                            .name(p.getProductName())
                            .forecast(df.getYPred())
                            .actual(df.getActualOrderQty())
                            .metric(safeMAPE(df.getActualOrderQty(), df.getYPred()))
                            .recommendedOrderQty(
                                    so != null ? so.getRecommendedOrderQty() : null
                            )
                            .build();
                })
                .sorted(Comparator.comparing(ProductDetailRowResponseDto::getSku))
                .toList();

        return DemandForecastReportResponseDto.builder()
                .timeseries(timeseries)
                .categoryMetrics(categoryMetrics)
                .details(details)
                .build();
    }

//    @Override
//    public List<TimeSeriesPointResponseDto> getProductSeries(Long productId, LocalDate targetWeek) {
//
//        LocalDate from = targetWeek.minusMonths(11).withDayOfMonth(1);
//        LocalDate to = targetWeek.plusMonths(1).withDayOfMonth(1)
//                .plusMonths(1).minusDays(1);
//
//        List<DemandForecast> dfList =
//                demandForecastRepository.findWithProductAndCategoryByTargetWeekBetween(
//                        from, to
//                );
//
//        return dfList.stream()
//                .sorted(Comparator.comparing(DemandForecast::getTargetWeek))
//                .map(df -> TimeSeriesPointResponseDto.builder()
//                        .date(df.getTargetWeek().toString())
//                        .forecast(df.getYPred() == null ? null : df.getYPred().doubleValue())
//                        .actual(df.getActualOrderQty())
//                        .build())
//                .toList();
//    }
    @Override
    public List<TimeSeriesPointResponseDto> getProductSeries(Long productId, LocalDate targetWeek) {

        LocalDate from = targetWeek.minusMonths(11).withDayOfMonth(1);
        LocalDate to = targetWeek.plusMonths(1).withDayOfMonth(1)
                .plusMonths(1).minusDays(1);

        List<DemandForecast> dfList =
                demandForecastRepository.findWithProductAndCategoryByTargetWeekBetween(from, to)
                        .stream()
                        .filter(df -> df.getProduct().getProductId().equals(productId))
                        .toList();

        // 1) 주차 기준 그룹화
        Map<LocalDate, List<DemandForecast>> grouped =
                dfList.stream().collect(Collectors.groupingBy(DemandForecast::getTargetWeek));

        // 2) 주차별 1개 포인트 만들기
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate week = entry.getKey();
                    List<DemandForecast> rows = entry.getValue();

                    // 예측(평균)
                    Double pred = rows.stream()
                            .map(DemandForecast::getYPred)
                            .filter(v -> v != null)
                            .mapToDouble(v -> v)
                            .average()
                            .orElse(Double.NaN);

                    // 실제(합계)
                    Integer actual = rows.stream()
                            .map(DemandForecast::getActualOrderQty)
                            .filter(v -> v != null)
                            .mapToInt(v -> v)
                            .sum();

                    // 실제가 없으면 포인트 제거
                    if (actual == 0) return null;

                    return TimeSeriesPointResponseDto.builder()
                            .date(week.toString())
                            .forecast(Double.isNaN(pred) ? null : pred)
                            .actual(actual)
                            .build();
                })
                .filter(dto -> dto != null)
                .toList();
    }




    // 카테고리 중분류 추출
    private String extractMiddleCategoryName(ProductCategory category) {

        if (category == null) return null;


        if (category.getCategoryLevel() == CategoryLevel.MEDIUM) {
            return category.getCategoryName();
        }

        if (category.getCategoryLevel() == CategoryLevel.SMALL &&
                category.getParent() != null &&
                category.getParent().getCategoryLevel() == CategoryLevel.MEDIUM) {
            return category.getParent().getCategoryName();
        }


        return null;
    }


    // 안전한 MAPE 계산 (actual null 가능)
    private double safeMAPE(Integer actual, Integer forecast) {
        if (actual == null || actual == 0 || forecast == null) {
            return Double.NaN;
        }
        return Math.abs((actual - forecast) / (double) actual);
    }
}