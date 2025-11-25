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
public class StoreListRes {
    private Long storeId;
    private String storeCode;
    private String storeName;
    private String address;
    private String contactNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

