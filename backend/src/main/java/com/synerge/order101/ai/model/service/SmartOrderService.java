package com.synerge.order101.ai.model.service;

import com.synerge.order101.ai.model.dto.request.SmartOrderUpdateRequestDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDashboardResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDetailResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderResponseDto;
import com.synerge.order101.common.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface SmartOrderService {

    List<SmartOrderResponseDto> generateSmartOrders(LocalDate targetWeek);
    List<SmartOrderResponseDto> getSmartOrders(OrderStatus status, LocalDate from, LocalDate to);
    SmartOrderResponseDto getSmartOrder(Long id);
    SmartOrderDetailResponseDto getSmartOrderDetail(Long supplierId, LocalDate targetWeek);
    SmartOrderResponseDto submitSmartOrder(Long smartOrderId, SmartOrderUpdateRequestDto request);
    SmartOrderDashboardResponseDto getSmartOrderSummary(LocalDate targetWeek);



}