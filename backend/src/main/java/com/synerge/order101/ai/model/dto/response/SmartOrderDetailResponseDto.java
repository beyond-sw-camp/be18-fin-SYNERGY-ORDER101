package com.synerge.order101.ai.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SmartOrderDetailResponseDto {
    private Long supplierId;
    private String supplierName;
    private LocalDate targetWeek;
    private String requesterName;
    private List<SmartOrderLineItemResponseDto> items;
}
