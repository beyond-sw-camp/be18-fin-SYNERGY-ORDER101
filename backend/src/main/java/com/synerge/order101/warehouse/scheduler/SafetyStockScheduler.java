package com.synerge.order101.warehouse.scheduler;

import com.synerge.order101.warehouse.model.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafetyStockScheduler {

    private final InventoryService inventoryService;

    /**
     * 매일 새벽 3시 안전재고 업데이트
     * CRON: 0 0 3 * * *
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void updateSafetyStockDaily() {
        log.info("▶ [안전재고 스케줄러] 실행 시작");

        try {
            inventoryService.updateDailySafetyStock();
            log.info("▶ [안전재고 스케줄러] 안전재고 업데이트 완료");

        } catch (Exception e) {
            log.error("[안전재고 스케줄러] 오류 발생: {}", e.getMessage(), e);
        }

        log.info("▶ [안전재고 스케줄러] 실행 종료");
    }
}
