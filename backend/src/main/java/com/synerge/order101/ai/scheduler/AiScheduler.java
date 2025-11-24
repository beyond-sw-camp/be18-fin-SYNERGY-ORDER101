package com.synerge.order101.ai.scheduler;

import com.synerge.order101.ai.model.service.DemandForecastService;
import com.synerge.order101.ai.model.service.SmartOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiScheduler {
    private final DemandForecastService demandForecastService;
    private final SmartOrderService smartOrderService;

    // 한달에 한번 재학습
    //매달 1일 새벽 02:00으로 모델 재학습 트리거
    //cron : 초 분 시 일 월 요일
    @Scheduled(cron = "0 0 2 1 * ?")
    public void monthlyRetrain() {
        log.info("[AI] Monthly retrain job start");
        demandForecastService.triggerRetrain();
    }


    // 스마트 발주 생성은 일주일에 한번
    // 매주 월요일 새벽 03:00에 그 주차의 스마트 발주 생성
    // targetWeek은 그럼 다음주 월요일 날짜
    @Scheduled(cron = "0 0 3 ? * MON")
    public void weeklySmartOrderGenerate() {
        LocalDate today = LocalDate.now();
        // 다음 주 월요일 기준
        LocalDate nextWeekMonday = today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        log.info("[AI] Weekly smart order generation start. targetWeek={}", nextWeekMonday);

        // 이미 Python 예측이 끝났다고 가정하고 smart_order 생성
        // 실제로는 이 스케줄보다 더 앞서 예측 작업이 돌게 해야함
        smartOrderService.generateSmartOrders(nextWeekMonday);
    }
}
