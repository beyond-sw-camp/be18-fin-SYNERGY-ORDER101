package com.synerge.order101.ai.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailRowResponseDto {
    private Long productId;
    private String sku;
    private String name;
    private Integer forecast;
    private Integer actual;
    private Double metric;
    private Integer recommendedOrderQty;
}