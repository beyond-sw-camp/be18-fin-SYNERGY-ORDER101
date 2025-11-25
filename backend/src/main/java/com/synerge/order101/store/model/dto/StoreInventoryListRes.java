package com.synerge.order101.store.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreInventoryListRes {
    private Long storeInventoryId;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer onHandQty;
    private Integer inTransitQty;
    private Integer safetyQty;
    private LocalDateTime updatedAt;
}

