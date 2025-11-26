package com.synerge.order101.outbound.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class OutboundResponseDto {
    private Long outboundId;

    private String outboundNo;

    private LocalDateTime outboundDatetime;

    private String storeName;

    private Integer itemCount;

    private Integer totalShippedQty;
}
