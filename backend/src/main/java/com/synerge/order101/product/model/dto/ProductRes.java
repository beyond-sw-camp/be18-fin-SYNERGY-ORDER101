package com.synerge.order101.product.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRes {
    private String productName;
    private String productCode;
    private String categorySmallName;
    private String categoryMediumName;
    private String categoryLargeName;
    private String imageUrl;
    private Boolean status;
    private BigDecimal price;
    private String description;
}
