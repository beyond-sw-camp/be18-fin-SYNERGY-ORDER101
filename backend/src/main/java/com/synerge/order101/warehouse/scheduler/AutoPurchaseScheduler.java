package com.synerge.order101.warehouse.scheduler;

import com.synerge.order101.purchase.model.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPurchaseScheduler {

    private final PurchaseService purchaseService;

    /**
     * 매일 새벽 3시 자동 발주 생성
     * CRON: 0 10 3 * * *
     * 안전재고 업데이트 후 여유 시간 두고 실행시킬 수도 있음
     */
    @Scheduled(cron = "0 10 3 * * *", zone = "Asia/Seoul")
    public void triggerAutoPurchaseDaily() {
        log.info("▶ [자동발주 스케줄러] 실행 시작");

        try {
            purchaseService.createAutoPurchase();
            log.info("▶ [자동발주 스케줄러] 자동발주 생성 완료");

        } catch (Exception e) {
            log.error("[자동발주 스케줄러] 오류 발생: {}", e.getMessage(), e);
        }

        log.info("▶ [자동발주 스케줄러] 실행 종료");
    }
}
