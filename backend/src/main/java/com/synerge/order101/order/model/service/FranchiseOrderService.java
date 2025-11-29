package com.synerge.order101.order.model.service;

import com.synerge.order101.order.model.dto.FranchiseOrderDetailResponseDto;

public interface FranchiseOrderService {
    public FranchiseOrderDetailResponseDto getFranchiseOrderDetail(Long orderId);
}
