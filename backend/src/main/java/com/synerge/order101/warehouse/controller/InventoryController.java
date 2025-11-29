package com.synerge.order101.warehouse.controller;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.warehouse.model.dto.response.InventoryResponseDto;
import com.synerge.order101.warehouse.model.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    // 창고 재고 조회
    @GetMapping("/inventory")
    public ResponseEntity<ItemsResponseDto<InventoryResponseDto>> getInventory(
            @RequestParam int page,
            @RequestParam int numOfRows,
            @RequestParam(required = false) Long largeId,
            @RequestParam(required = false) Long mediumId,
            @RequestParam(required = false) Long smallId
    ) {

        Page<InventoryResponseDto> inventory = inventoryService.getInventoryList(
                page, numOfRows, largeId, mediumId, smallId
        );
        int totalCount = (int) inventory.getTotalElements();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, inventory.getContent(), page, totalCount));
    }


}
