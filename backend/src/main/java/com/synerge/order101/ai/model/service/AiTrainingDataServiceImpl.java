package com.synerge.order101.ai.model.service;

import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTrainingDataServiceImpl implements AiTrainingDataService {

    private final PurchaseDetailRepository purchaseDetailRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8000")       // Python 서버
            .build();

    @Override
    @Transactional
    public void sendMonthlyActualSales(LocalDate startDate, LocalDate endDate) {

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(23, 59, 59);

        List<PurchaseDetail> list =
                purchaseDetailRepository.findSubmittedBetween(from, to);

        if (list.isEmpty()) {
            log.warn("[AI] No SUBMITTED purchase details found for {} ~ {}", startDate, endDate);
            return;
        }

        // JSON 변환
        var payload = list.stream().map(pd -> {
            var p = pd.getPurchase();
            var prod = pd.getProduct();

            return new ActualSalesRow(
                    prod.getProductId(),
                    pd.getOrderQty(),
                    p.getWarehouse().getWarehouseId(),
                    p.getUser() != null ? p.getUser().getUserId() : 1L,
                    pd.getCreatedAt().toLocalDate()
            );
        }).collect(Collectors.toList());

        log.info("[AI] sending {} rows to Python...", payload.size());

        webClient.post()
                .uri("/internal/ai/add-actual-sales")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("[AI] monthly sales synced successfully.");
    }

    record ActualSalesRow(
            Long productId,
            Integer qty,
            Long warehouseId,
            Long userId,
            LocalDate date
    ) {}
}