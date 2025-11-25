package com.synerge.order101.store.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreCreateReq {
        private String storeCode;

        private String storeName;

        private String address;

    private String contactNumber;

    private Long defaultWarehouseId;
}

