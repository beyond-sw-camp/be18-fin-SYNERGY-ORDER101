package com.synerge.order101.settlement.model.service;

import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.common.exception.errorcode.CommonErrorCode;
import com.synerge.order101.settlement.event.SettlementReqEvent;
import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.settlement.model.dto.SettlementSummaryDto;
import com.synerge.order101.settlement.model.entity.Settlement;
import com.synerge.order101.settlement.model.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;


@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService{

    private final SettlementRepository settlementRepository;

    @Override
    public Page<SettlementSummaryDto> getSettlements(TradeSearchCondition cond, Pageable pageable) {

        Page<Settlement> page = settlementRepository.search(cond, pageable);

        return page.map(SettlementSummaryDto::fromEntity);

    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSettlementRequired(SettlementReqEvent event){

        if(event.existSettlement(settlementRepository)){
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        // 정산 실행
        Settlement settlement = Settlement.builder()
                .settlementType(event.settlementType())
                .productsAmount(event.settlementAmount())
                .productsQty(event.settlementQty())
                .store(event.store())
                .supplier(event.supplier())
                .purchase(event.purchase())
                .storeOrder(event.storeOrder())
                .build();

        settlementRepository.save(settlement);
    }

}
