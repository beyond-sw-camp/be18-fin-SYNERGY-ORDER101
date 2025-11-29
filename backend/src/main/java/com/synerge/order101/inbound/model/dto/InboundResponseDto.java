package com.synerge.order101.inbound.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class InboundResponseDto {

    private Long inboundId;

    private String inboundNo;

    private LocalDateTime inboundDatetime;

    private String supplierName;
    
    private Integer itemCount;

    private Integer totalReceivedQty;

}
