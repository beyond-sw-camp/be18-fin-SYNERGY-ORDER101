package com.synerge.order101.store.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryCreateReq {
    private Long productId;

    private Integer onHandQty = 0;

    private Integer inTransitQty = 0;

    private Integer safetyQty;
}

