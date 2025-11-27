package com.synerge.order101.ai.model.service;

import com.synerge.order101.ai.model.dto.response.CategoryMetricResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastReportResponseDto;
import com.synerge.order101.ai.model.dto.response.ProductDetailRowResponseDto;
import com.synerge.order101.ai.model.dto.response.TimeSeriesPointResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.repository.SmartOrderRepository;
import com.synerge.order101.product.model.entity.CategoryLevel;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductCategory;
import com.synerge.order101.product.model.repository.ProductRepository;
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
    public DemandForecastReportResponseDto buildForecastReport(LocalDate targetWeek) {

        LocalDate from = targetWeek.minusMonths(11).withDayOfMonth(1);
        LocalDate to = targetWeek.plusMonths(1).withDayOfMonth(1)
                .plusMonths(1).minusDays(1);

        List<DemandForecast> dfList =
                demandForecastRepository.findByTargetWeekBetween(from, to);

        // 1) Timeseries (좌측 그래프)
        List<TimeSeriesPointResponseDto> timeseries = dfList.stream()
                .sorted(Comparator.comparing(DemandForecast::getTargetWeek))
                .map(df -> TimeSeriesPointResponseDto.builder()
                        .date(df.getTargetWeek().toString())
                        .forecast(df.getYPred())
                        .actual(df.getActualOrderQty()) // 미래면 자동 null
                        .build())
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

        List<CategoryMetricResponseDto> categoryMetrics = grouped.entrySet().stream()
                .map(entry -> {
                    double avgMAPE = entry.getValue().stream()
                            .filter(df -> df.getActualOrderQty() != null)
                            .mapToDouble(df ->
                                    safeMAPE(df.getActualOrderQty(), df.getYPred()))
                            .average()
                            .orElse(0.0);

                    return CategoryMetricResponseDto.builder()
                            .category(entry.getKey())
                            .metric(avgMAPE)
                            .build();
                })
                .sorted(Comparator.comparing(CategoryMetricResponseDto::getCategory))
                .toList();



        // 3) 상세 테이블 (최근 targetWeek 기준)
        LocalDate fromWeek = targetWeek.withDayOfMonth(1);
        LocalDate toWeek = targetWeek.withDayOfMonth(1).plusMonths(1).minusDays(1);

        List<DemandForecast> latest =
                demandForecastRepository.findByTargetWeekBetween(fromWeek, toWeek);


        List<ProductDetailRowResponseDto> details = latest.stream()
                .map(df -> {
                    Product p = df.getProduct();
                    SmartOrder so = smartOrderRepository.findTopByDemandForecast_DemandForecastId(df.getDemandForecastId())
                            .orElse(null);

                    return ProductDetailRowResponseDto.builder()
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

    @Override
    public List<TimeSeriesPointResponseDto> getProductSeries(Long productId, LocalDate targetWeek) {

        LocalDate from = targetWeek.minusMonths(11).withDayOfMonth(1);
        LocalDate to = targetWeek.plusMonths(1).withDayOfMonth(1)
                .plusMonths(1).minusDays(1);

        List<DemandForecast> dfList =
                demandForecastRepository.findWithProductAndCategoryByTargetWeekBetween(
                        from, to
                );

        return dfList.stream()
                .sorted(Comparator.comparing(DemandForecast::getTargetWeek))
                .map(df -> TimeSeriesPointResponseDto.builder()
                        .date(df.getTargetWeek().toString())
                        .forecast(df.getYPred())
                        .actual(df.getActualOrderQty())
                        .build())
                .toList();
    }



    // 카테고리 중분류 추출 (대/중/소 자동 처리)
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
    private Double safeMAPE(Integer actual, Integer forecast) {
        if (actual == null || actual == 0 || forecast == null) {
            return null;
        }
        return Math.abs((actual - forecast) / (double) actual);
    }
}