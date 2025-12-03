package com.synerge.order101.purchase.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AutoPurchaseSearchRequestDto {

    private Long supplierId;
    private String startDate;
    private String endDate;

    private Integer page;
    private Integer numOfRows;
}
