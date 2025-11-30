package com.synerge.order101.purchase.model.dto;

import java.util.List;

public record AutoPurchaseSubmitRequestDto(List<Item> items) {
    public record Item(
            Long purchaseOrderLineId,
            Long productId,
            Integer orderQty
    ) {}
}

