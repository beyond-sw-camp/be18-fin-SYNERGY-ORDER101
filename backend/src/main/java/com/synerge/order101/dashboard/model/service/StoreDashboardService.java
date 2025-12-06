package com.synerge.order101.dashboard.model.service;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.dashboard.model.dto.response.StoreDashboardSummaryResponseDto;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import com.synerge.order101.store.model.repository.StoreInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreDashboardService {

    private final StoreOrderRepository storeOrderRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ShipmentRepository shipmentRepository;

    public StoreDashboardSummaryResponseDto getSummary(Long storeId) {

        LocalDateTime from = YearMonth.now()
                .atDay(1)
                .atStartOfDay();

        LocalDateTime to = YearMonth.now()
                .atEndOfMonth()
                .atTime(23, 59, 59);

        return StoreDashboardSummaryResponseDto.builder()
                // 1. 승인 대기 발주
                .pendingOrderCount(
                        storeOrderRepository.countByStore_StoreIdAndOrderStatus(
                                storeId, OrderStatus.SUBMITTED
                        )
                )

                // 2. 입고 예정 수량
                .inTransitQtySum(
                        storeInventoryRepository.sumInTransitQty(storeId)
                )

                // 3. 배송 중 발주
                .inTransitShipmentCount(
                        shipmentRepository.countByStore_StoreIdAndShipmentStatus(
                                storeId, ShipmentStatus.IN_TRANSIT
                        )
                )

                // 4. 이번 달 반려 발주
                .canceledThisMonthCount(
                        storeOrderRepository.countCanceledThisMonth(storeId, from, to)
                )
                .build();
    }
}

