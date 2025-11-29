package com.synerge.order101.warehouse.scheduler;

import com.synerge.order101.purchase.model.service.PurchaseService;
import com.synerge.order101.warehouse.model.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPurchaseScheduler {
    private final InventoryService inventoryService;
    private final PurchaseService purchaseService;

    /**
     * 매일 새벽 3시 안전재고 업데이트 + 자동발주 트리거 수행
     *
     * CRON: 0 0 3 * * *  → 3:00 AM 매일 실행
     */
    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul")
    public void runDailyReplenishmentBatch() {
        log.info("▶ [자동발주 스케줄러] 실행 시작");

        try {
            log.info("1) 안전재고 업데이트 시작");
            inventoryService.updateDailySafetyStock();  // 안전재고 계산 및 업데이트
            log.info("1) 안전재고 업데이트 완료");

            log.info("2) 자동발주 트리거 시작");
            purchaseService.createAutoPurchase();  // 자동발주 생성
            log.info("2) 자동발주 트리거 완료");

        } catch (Exception e) {
            log.error("[자동발주 스케줄러] 오류 발생: {}", e.getMessage(), e);
        }

        log.info("▶ [자동발주 스케줄러] 실행 종료");
    }
}
