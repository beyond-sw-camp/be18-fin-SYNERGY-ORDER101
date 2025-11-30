package com.synerge.order101.product.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateReq {
    private String productName;
    private String description;
    private BigDecimal price;
    private Boolean status;
    private String imageUrl;

    private Long categoryLargeId;
    private Long categoryMediumId;
    private Long categorySmallId;

    private Boolean removeImage;
}
