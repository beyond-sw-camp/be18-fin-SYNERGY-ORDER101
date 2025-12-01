package com.synerge.order101.warehouse.model.dto.response;

import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class InventoryResponseDto {

    private Long warehouseInventoryId;

    private Long productId;

    private String productCode;

    private String productCategory;

    private String productName;

    private Integer onHandQty;

    private Integer safetyQty;

    private BigDecimal price;

    public static InventoryResponseDto fromEntity (WarehouseInventory inventory) {
        return new InventoryResponseDto(
                inventory.getInventoryId(),
                inventory.getProduct().getProductId(),
                inventory.getProduct().getProductCode(),
                inventory.getProduct().getProductCategory().getCategoryName(),
                inventory.getProduct().getProductName(),
                inventory.getOnHandQuantity(),
                inventory.getSafetyQuantity(),
                inventory.getProduct().getPrice()

        );
    }
}
