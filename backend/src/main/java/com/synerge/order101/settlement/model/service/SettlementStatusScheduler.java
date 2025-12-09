package com.synerge.order101.settlement.model.service;

import com.synerge.order101.settlement.model.entity.Settlement;
import com.synerge.order101.settlement.model.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that promotes Settlement entries from DRAFT to ISSUED every 30 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementStatusScheduler {

    private final SettlementRepository settlementRepository;

    /**
     * Runs every 10 minutes.
     * - ISSUED -> DRAFT
     * - (existing) DRAFT -> VOID
     * Both lists are captured before any changes to avoid cascading conversions within the same run.
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void rotateSettlementStatuses() {
        log.info("SettlementStatusScheduler: running rotateSettlementStatuses at {}", LocalDateTime.now());

        List<Settlement> issuedList = settlementRepository.findBySettlementStatus(Settlement.SettlementStatus.ISSUED);
        List<Settlement> draftList = settlementRepository.findBySettlementStatus(Settlement.SettlementStatus.DRAFT);

        int toDraft = 0;
        int toVoid = 0;

        if (issuedList != null && !issuedList.isEmpty()) {
            for (Settlement s : issuedList) {
                try {
                    s.markAsDraft();
                    settlementRepository.save(s);
                    toDraft++;
                } catch (Exception ex) {
                    log.error("SettlementStatusScheduler: failed to convert ISSUED->DRAFT id={}", s.getSettlementId(), ex);
                }
            }
        }

        if (draftList != null && !draftList.isEmpty()) {
            for (Settlement s : draftList) {
                try {
                    s.markAsVoid();
                    settlementRepository.save(s);
                    toVoid++;
                } catch (Exception ex) {
                    log.error("SettlementStatusScheduler: failed to convert DRAFT->VOID id={}", s.getSettlementId(), ex);
                }
            }
        }

        log.info("SettlementStatusScheduler: converted {} ISSUED->DRAFT and {} DRAFT->VOID", toDraft, toVoid);
    }

}
