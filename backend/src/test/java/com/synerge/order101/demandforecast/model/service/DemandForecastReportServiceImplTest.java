package com.synerge.order101.demandforecast.model.service;


import com.synerge.order101.ai.model.dto.response.CategoryMetricResponseDto;
import com.synerge.order101.ai.model.dto.response.DemandForecastReportResponseDto;
import com.synerge.order101.ai.model.dto.response.ProductDetailRowResponseDto;
import com.synerge.order101.ai.model.dto.response.TimeSeriesPointResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.repository.SmartOrderRepository;
import com.synerge.order101.ai.model.service.DemandForecastReportServiceImpl;
import com.synerge.order101.product.model.entity.CategoryLevel;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductCategory;
import com.synerge.order101.product.model.repository.ProductRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemandForecastReportServiceImplTest {

    @InjectMocks
    private DemandForecastReportServiceImpl reportService;

    @Mock private DemandForecastRepository demandForecastRepository;
    @Mock private SmartOrderRepository smartOrderRepository;
    @Mock private ProductRepository productRepository;


    @Test
    @Order(1)
    @DisplayName("buildForecastReport_success")
    void buildForecastReport_success() {

        LocalDate targetWeek = LocalDate.of(2025, 1, 6);


        ProductCategory mid = mock(ProductCategory.class);
        given(mid.getCategoryName()).willReturn("영상가전");
        given(mid.getCategoryLevel()).willReturn(CategoryLevel.MEDIUM);
        given(mid.getParent()).willReturn(null);


        Product product = mock(Product.class);
        given(product.getProductId()).willReturn(100L);
        given(product.getProductCode()).willReturn("TV-001");
        given(product.getProductName()).willReturn("삼성 TV");
        given(product.getProductCategory()).willReturn(mid);


        DemandForecast df1 = DemandForecast.builder()
                .demandForecastId(1L)
                .product(product)
                .targetWeek(LocalDate.of(2025, 1, 6))
                .yPred(80)
                .actualOrderQty(100)
                .build();

        List<DemandForecast> dfList = List.of(df1);

        given(demandForecastRepository.findByTargetWeekBetween(any(), any()))
                .willReturn(dfList);

        SmartOrder so = SmartOrder.builder()
                .smartOrderId(10L)
                .recommendedOrderQty(120)
                .build();

        given(smartOrderRepository.findTopByDemandForecast_DemandForecastId(1L))
                .willReturn(Optional.of(so));

        DemandForecastReportResponseDto result =
                reportService.buildForecastReport(targetWeek);

        assertThat(result.getTimeseries()).hasSize(1);
        TimeSeriesPointResponseDto ts = result.getTimeseries().get(0);
        assertThat(ts.getActual()).isEqualTo(100);
        assertThat(ts.getForecast()).isEqualTo(80.0);

        assertThat(result.getCategoryMetrics()).hasSize(1);
        CategoryMetricResponseDto cat = result.getCategoryMetrics().get(0);
        assertThat(cat.getCategory()).isEqualTo("영상가전");

        assertThat(result.getDetails()).hasSize(1);
        ProductDetailRowResponseDto row = result.getDetails().get(0);

        assertThat(row.getProductId()).isEqualTo(100L);
        assertThat(row.getSku()).isEqualTo("TV-001");
        assertThat(row.getForecast()).isEqualTo(80);
        assertThat(row.getActual()).isEqualTo(100);
        assertThat(row.getRecommendedOrderQty()).isEqualTo(120);
    }

    @Test
    @Order(2)
    @DisplayName("getProductSeries_success")
    void getProductSeries_success() {

        Long productId = 100L;
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);

        ProductCategory mid = mock(ProductCategory.class);
        given(mid.getCategoryLevel()).willReturn(CategoryLevel.MEDIUM);
        given(mid.getCategoryName()).willReturn("영상가전");

        Product product = mock(Product.class);
        given(product.getProductId()).willReturn(productId);
        given(product.getProductCategory()).willReturn(mid);

        DemandForecast df1 = DemandForecast.builder()
                .product(product)
                .targetWeek(LocalDate.of(2025, 1, 6))
                .yPred(50)
                .actualOrderQty(70)
                .build();

        DemandForecast df2 = DemandForecast.builder()
                .product(product)
                .targetWeek(LocalDate.of(2025, 1, 13))
                .yPred(40)
                .actualOrderQty(60)
                .build();

        given(demandForecastRepository.findWithProductAndCategoryByTargetWeekBetween(any(), any()))
                .willReturn(List.of(df1, df2));

        List<TimeSeriesPointResponseDto> result =
                reportService.getProductSeries(productId, targetWeek);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getForecast()).isEqualTo(50.0);
        assertThat(result.get(0).getActual()).isEqualTo(70);

        assertThat(result.get(1).getForecast()).isEqualTo(40.0);
        assertThat(result.get(1).getActual()).isEqualTo(60);
    }
}