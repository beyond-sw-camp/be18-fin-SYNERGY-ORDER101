package com.synerge.order101.inbound.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class InboundDetailResponseDto {

    private String inboundNo;

    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String productCode;
        private String productName;
        private Integer receivedQty;
    }
}
