package com.synerge.order101.ai.service;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.ai.model.dto.request.SmartOrderUpdateRequest;
import com.synerge.order101.ai.model.dto.response.SmartOrderResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.repository.DemandForecastRepository;
import com.synerge.order101.ai.repository.SmartOrderRepository;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.supplier.exception.SupplierErrorCode;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartOrderService {
    private final SmartOrderRepository smartOrderRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final UserRepository userRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final NotificationService notificationService;

    //AI가 스마트 발주 초안 작성
    // targetWeek을 기준 demand_forecst 조회해서 smart_order DRAFT 생성함
    //TODO
    //실제 추천 수량 로직
    //지금은 예측 수요량 = forecast_qty = recommended_order_qty
    //재고/안전재고 반영해야함

    @Transactional
    public List<SmartOrderResponseDto> generateSmartOrders(LocalDate targetWeek){
        List<DemandForecast> forecasts = demandForecastRepository.findByTargetWeek(targetWeek);
        if (forecasts.isEmpty()){
            throw new CustomException(AiErrorCode.FORECAST_NOT_FOUND);
        }
        List<SmartOrder> saved = forecasts.stream()
                .map(df -> SmartOrder.builder()
                        .store(df.getStore())
                        .product(df.getProduct())
                        .demandForecast(df)
                        .targetWeek(df.getTargetWeek())
                        .forecastQty(df.getYPred())
                        .recommendedOrderQty(df.getYPred())
                        .smartOrderStatus(SmartOrder.SmartOrderStatus.DRAFT)
                        .build())
                .map(smartOrderRepository::save)
                .toList();

        List<User> hqList = userRepository.findByRole(Role.HQ);

        // smartorder -> supplier 공급사별 매핑
        Map<Long, List<SmartOrder>> bySupplier = new HashMap<>();

        for (SmartOrder smartOrder : saved) {
            Product product = smartOrder.getProduct();

            ProductSupplier ps = productSupplierRepository.findTop1ByProduct(product).orElseThrow(() ->
                    new CustomException(SupplierErrorCode.SUPPLIER_NOT_FOUND));

            Supplier supplier = ps.getSupplier();

            bySupplier.computeIfAbsent(supplier.getSupplierId(), k -> new ArrayList<>()).add(smartOrder);
        }

        // 공급사별로 알림 1개씩 전송
        for (Map.Entry<Long, List<SmartOrder>> entry : bySupplier.entrySet()) {
            List<SmartOrder> smartOrders = entry.getValue();
            Supplier supplier = productSupplierRepository.findTop1ByProduct(smartOrders.get(0).getProduct())
                    .get().getSupplier();

            notificationService.notifySmartOrderCreatedToHq(hqList, supplier, smartOrders);
        }

        return saved.stream().map(this::toResponse).toList();
    }


    public List<SmartOrderResponseDto> getSmartOrders(Long storeId, SmartOrder.SmartOrderStatus status) {
        List<SmartOrder> list;
        if (storeId != null && status != null) {
            list = smartOrderRepository.findByStore_StoreIdAndSmartOrderStatus(storeId, status);
        } else if (storeId != null) {
            list = smartOrderRepository.findByStore_StoreIdOrderByTargetWeekDesc(storeId);
        } else {
            list = smartOrderRepository.findAll();
        }
        return list.stream().map(this::toResponse).toList();
    }



    public SmartOrderResponseDto getSmartOrder(Long id){
        SmartOrder entity = smartOrderRepository.findById(id)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));
        return toResponse(entity);
    }

    @Transactional
    public SmartOrderResponseDto updateDraft(Long smartOrderId, SmartOrderUpdateRequest request) {
        SmartOrder entity = smartOrderRepository.findById(smartOrderId)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));

        entity.updateDraft(request.getRecommendedOrderQty());

        return toResponse(entity);
    }

    @Transactional
    public SmartOrderResponseDto submit(Long smartOrderId) {
        SmartOrder entity = smartOrderRepository.findById(smartOrderId)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));
        entity.submit();

        List<User> admins = userRepository.findByRole(Role.HQ_ADMIN);

        if (!admins.isEmpty()){
            notificationService.notifySmartOrderApprovalToAdmins(entity, admins);
        }

        return toResponse(entity);
    }


    private SmartOrderResponseDto toResponse(SmartOrder so) {
        return SmartOrderResponseDto.builder()
                .id(so.getSmartOrderId())
                .storeId(so.getStore().getStoreId())
                .productId(so.getProduct().getProductId())
                .demandForecastId(so.getDemandForecast().getDemandForecastId())
                .targetWeek(so.getTargetWeek())
                .recommendedOrderQty(so.getRecommendedOrderQty())
                .forecastQty(so.getForecastQty())
                .smartOrderStatus(so.getSmartOrderStatus())
                .snapshotAt(so.getSnapshotAt())
                .updatedAt(so.getUpdatedAt())
                .build();
    }
}
