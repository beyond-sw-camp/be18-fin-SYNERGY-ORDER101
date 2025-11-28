package com.synerge.order101.order.model.service;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.order.model.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface StoreOrderService {
    public Page<StoreOrderSummaryResponseDto> findOrders(TradeSearchCondition cond, Pageable pageable);

    public StoreOrderCreateResponseDto createOrder(StoreOrderCreateRequest request);

    public StoreOrderDetailResponseDto findStoreOrderDetails(Long storeOrderId);

    public StoreOrderUpdateStatusResponseDto updateOrderStatus(Long storeOrderId, OrderStatus newStatus);

}
