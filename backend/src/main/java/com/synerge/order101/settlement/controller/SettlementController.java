package com.synerge.order101.settlement.controller;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.settlement.model.dto.*;
import com.synerge.order101.settlement.model.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<Page<SettlementSummaryDto>> getSettlements(
            @ModelAttribute TradeSearchCondition cond,
            Pageable pageable
    ) {
        Page<SettlementSummaryDto> results = settlementService.getSettlements(cond, pageable);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementDetailResponseDto> getSettlementDetail(
            @PathVariable("settlementId") String settlementId) {

        return null;
    }

}
