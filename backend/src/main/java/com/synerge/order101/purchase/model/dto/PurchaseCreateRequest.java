package com.synerge.order101.purchase.model.dto;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.entity.Purchase;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseCreateRequest {

    @NonNull
    Long supplierId;

    @NonNull
    Long userId;

    @NonNull
    Long warehouseId;

    @NonNull
    Purchase.OrderType orderType;

    @NonNull
    OrderStatus orderStatus;

    @NonNull
    LocalDate deadline;

    @Builder.Default
    List<Item> items = List.of();

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NonNull
        private Long productId;

        @NonNull
        private Integer orderQty;
    }
}
