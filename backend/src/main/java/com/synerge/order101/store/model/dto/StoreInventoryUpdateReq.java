package com.synerge.order101.store.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventoryUpdateReq {
        private Integer onHandQty;

    private Integer inTransitQty;

    private Integer safetyQty;
}

