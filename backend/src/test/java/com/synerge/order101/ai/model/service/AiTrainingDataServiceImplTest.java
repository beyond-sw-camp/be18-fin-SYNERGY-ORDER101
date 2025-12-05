package com.synerge.order101.ai.model.service;

import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("AiTrainingDataServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AiTrainingDataServiceImplTest {

    @InjectMocks
    private AiTrainingDataServiceImpl aiTrainingDataService;

    @Mock
    private PurchaseDetailRepository purchaseDetailRepository;

    // ========================
    // sendMonthlyActualSales
    // ========================
    @Test
    @Order(1)
    @DisplayName("월간 실제 판매 데이터 전송 - 데이터 없음")
    void sendMonthlyActualSales_NoData() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        given(purchaseDetailRepository.findSubmittedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        aiTrainingDataService.sendMonthlyActualSales(startDate, endDate);

        // then
        verify(purchaseDetailRepository).findSubmittedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        // WebClient 호출이 되지 않음 (데이터 없으므로)
    }

    @Test
    @Order(2)
    @DisplayName("날짜 범위 변환 확인")
    void sendMonthlyActualSales_DateConversion() {
        // given
        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 30);

        LocalDateTime expectedStart = startDate.atStartOfDay();
        LocalDateTime expectedEnd = endDate.atTime(23, 59, 59);

        given(purchaseDetailRepository.findSubmittedBetween(expectedStart, expectedEnd))
                .willReturn(Collections.emptyList());

        // when
        aiTrainingDataService.sendMonthlyActualSales(startDate, endDate);

        // then
        verify(purchaseDetailRepository).findSubmittedBetween(expectedStart, expectedEnd);
    }
}
