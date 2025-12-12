package com.synerge.order101.settlement.model.service;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.settlement.model.dto.SettlementDetailResponseDto;
import com.synerge.order101.settlement.model.dto.SettlementSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementService {
    public Page<SettlementSummaryDto> getSettlements(TradeSearchCondition cond, Pageable pageable);
    
    SettlementDetailResponseDto getSettlementDetail(String settlementNo);
}
