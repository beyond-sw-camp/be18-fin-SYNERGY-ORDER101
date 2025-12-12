package com.synerge.order101.settlement.model.service;

import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.common.exception.errorcode.CommonErrorCode;
import com.synerge.order101.settlement.event.SettlementReqEvent;
import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.settlement.model.dto.SettlementDetailResponseDto;
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

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService{

    private final SettlementRepository settlementRepository;

    @Override
    public Page<SettlementSummaryDto> getSettlements(TradeSearchCondition cond, Pageable pageable) {

        Page<Settlement> page = settlementRepository.search(cond, pageable);

        return page.map(SettlementSummaryDto::fromEntity);

    }

    @Override
    @Transactional(readOnly = true)
    public com.synerge.order101.settlement.model.dto.SettlementDetailResponseDto getSettlementDetail(String settlementNo) {
        Settlement settlement = settlementRepository.findBySettlementNo(settlementNo)
                .orElseThrow(() -> new CustomException(CommonErrorCode.INVALID_REQUEST));

        // 정산 유형에 따라 vendor 정보 결정
        String vendorName;
        if (settlement.getSettlementType() == SettlementType.AP) {
            vendorName = settlement.getSupplier() != null ? 
                settlement.getSupplier().getSupplierName() : "Unknown Supplier";
        } else {
            vendorName = settlement.getStore() != null ? 
                settlement.getStore().getStoreName() : "Unknown Store";
        }

        // 정산 항목(items) 생성
        List<SettlementDetailResponseDto.SettlementItemDto> items;
        
        // 정산 기간: Purchase/StoreOrder 생성일 ~ Settlement 생성일
        LocalDateTime periodStart = null;
        LocalDateTime periodEnd = settlement.getCreatedAt(); // 정산이 생성된 날짜

        if (settlement.getSettlementType() == SettlementType.AP) {
            // AP 타입: Purchase의 PurchaseDetail 조회
            if (settlement.getPurchase() != null) {
                periodStart = settlement.getPurchase().getCreatedAt(); // 발주 생성일
                
                items = settlement.getPurchase().getPurchaseDetails().stream()
                        .map(detail -> com.synerge.order101.settlement.model.dto.SettlementDetailResponseDto.SettlementItemDto.builder()
                                .productName(detail.getProduct().getProductName())
                                .productCode(detail.getProduct().getProductCode())
                                .qty(detail.getOrderQty())
                                .price(detail.getUnitPrice())
                                .amount(detail.getUnitPrice().multiply(java.math.BigDecimal.valueOf(detail.getOrderQty())))
                                .build())
                        .collect(java.util.stream.Collectors.toList());
            } else {
                items = new java.util.ArrayList<>();
            }
        } else {
            // AR 타입: StoreOrder의 StoreOrderDetail 조회
            if (settlement.getStoreOrder() != null) {
                periodStart = settlement.getStoreOrder().getCreatedAt(); // 주문 생성일
                
                items = settlement.getStoreOrder().getStoreOrderDetails().stream()
                        .map(detail -> com.synerge.order101.settlement.model.dto.SettlementDetailResponseDto.SettlementItemDto.builder()
                                .productName(detail.getProduct().getProductName())
                                .productCode(detail.getProduct().getProductCode())
                                .qty(detail.getOrderQty())
                                .price(detail.getUnitPrice())
                                .amount(detail.getAmount())
                                .build())
                        .collect(java.util.stream.Collectors.toList());
            } else {
                items = new java.util.ArrayList<>();
            }
        }

        return SettlementDetailResponseDto.builder()
                .settlementNo(settlement.getSettlementNo())
                .settlementType(settlement.getSettlementType().name())
                .settlementStatus(settlement.getSettlementStatus().name())
                .vendorName(vendorName)
                .createdAt(settlement.getCreatedAt())
                .settledDate(settlement.getSettledDate())
                .settlementAmount(settlement.getProductsAmount())
                .settlementQty(settlement.getProductsQty())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .items(items)
                .build();
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
