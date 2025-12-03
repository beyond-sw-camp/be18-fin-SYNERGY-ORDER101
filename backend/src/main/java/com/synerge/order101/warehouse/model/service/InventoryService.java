package com.synerge.order101.warehouse.model.service;

import com.synerge.order101.purchase.model.dto.CalculatedAutoItem;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.warehouse.model.dto.response.InventoryResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {
    Page<InventoryResponseDto> getInventoryList(int page, int numOfRows, Long largeCategoryId, Long mediumCategoryId, Long smallCategoryId);

    void decreaseInventory(Long productId, int quantity);

    void increaseInventory(Long productId, int quantity);

    void updateDailySafetyStock();

    List<CalculatedAutoItem> getAutoPurchaseItems();
}
