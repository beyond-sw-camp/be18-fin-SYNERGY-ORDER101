package com.synerge.order101.dashboard.model.service;

import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.repository.SmartOrderRepository;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.dashboard.model.dto.response.DashboardSummaryResponseDto;
import com.synerge.order101.purchase.model.repository.PurchaseRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PurchaseRepository purchaseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final SmartOrderRepository smartOrderRepository;
    private final DemandForecastRepository demandForecastRepository;

    public DashboardSummaryResponseDto getSummary() {

        long pendingPurchase =
                purchaseRepository.countByOrderStatus(OrderStatus.SUBMITTED);

        long lowStockSku =
                warehouseInventoryRepository.countLowStockSku();

        long draftAutoSmartOrder =
                smartOrderRepository.countBySmartOrderStatus(OrderStatus.DRAFT_AUTO);

        double recentForecastAccuracy = calculateRecentForecastAccuracy();

        return new DashboardSummaryResponseDto(
                pendingPurchase,
                lowStockSku,
                draftAutoSmartOrder,
                recentForecastAccuracy
        );
    }


    private double calculateRecentForecastAccuracy() {

        List<DemandForecast> latest =
                demandForecastRepository.findLatestSnapshotForecasts();

        return latest.stream()
                .filter(df -> df.getActualOrderQty() != null && df.getActualOrderQty() > 0)
                .filter(df -> df.getYPred() != null)
                .mapToDouble(df -> {
                    double mape = Math.abs(
                            (df.getActualOrderQty() - df.getYPred())
                                    / (double) df.getActualOrderQty()
                    );
                    return 100.0 - (mape * 100.0);
                })
                .average()
                .orElse(0.0);
    }
}
