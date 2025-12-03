package com.synerge.order101.supplier.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierProductItemRes {
    private Long productId;
    private String productCode;
    private String supplierProductCode;
    private String productName;
    private BigDecimal purchasePrice;  // 공급가 (product_supplier.테이블)
    private BigDecimal price;          // 판매가 (product 테이블)
    private Integer leadTimeDays;
}
