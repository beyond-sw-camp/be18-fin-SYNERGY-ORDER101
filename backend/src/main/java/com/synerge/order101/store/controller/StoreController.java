package com.synerge.order101.store.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.store.model.dto.*;
import com.synerge.order101.store.model.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Validated
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<ItemsResponseDto<StoreListRes>> getStores(
            Pageable pageable,
            @RequestParam(required = false) String keyword
    ) {
        ItemsResponseDto<StoreListRes> body = storeService.getStores(pageable, keyword);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/search")
    public ResponseEntity<ItemsResponseDto<StoreListRes>> searchStores(
            Pageable pageable,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, name = "store_name") String storeName
    ) {
        ItemsResponseDto<StoreListRes> body = storeService.searchStores(pageable, address, storeName);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<BaseResponseDto<StoreRes>> createStore(@RequestBody  StoreCreateReq request) {
        StoreRes res = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BaseResponseDto<>(HttpStatus.CREATED, res));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponseDto<StoreRes>> getStore(@PathVariable Long storeId) {
        StoreRes res = storeService.getStore(storeId);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, res));
    }

    @PatchMapping("/{storeId}/toggle-active")
    public ResponseEntity<BaseResponseDto<StoreRes>> toggleStoreActive(@PathVariable Long storeId) {
        // 읽어서 현재 상태 반전 후 업데이트
        StoreRes current = storeService.getStore(storeId);
        Boolean currActive = current.getIsActive();
        boolean newActive = currActive == null || !currActive;
        storeService.updateStatus(storeId, newActive);
        StoreRes updated = storeService.getStore(storeId);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, updated));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ItemsResponseDto<String>> getDistinctAddresses() {
        ItemsResponseDto<String> body = storeService.getDistinctAddresses();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{storeId}/inventory")
    public ResponseEntity<ItemsResponseDto<StoreInventoryListRes>> getStoreInventory(@PathVariable Long storeId,
                                                                                     @RequestParam(defaultValue = "1") int page,
                                                                                     @RequestParam(defaultValue = "10") int numOfRows) {
        ItemsResponseDto<StoreInventoryListRes> body = storeService.getStoreInventoryList(storeId, page, numOfRows);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{storeId}/inventory")
    public ResponseEntity<BaseResponseDto<StoreInventoryListRes>> createStoreInventory(@PathVariable Long storeId,
                                                                                         @RequestBody  StoreInventoryCreateReq req) {
        StoreInventoryListRes res = storeService.createStoreInventory(storeId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BaseResponseDto<>(HttpStatus.CREATED, res));
    }

    @PutMapping("/{storeId}/product/{productId}")
    public ResponseEntity<BaseResponseDto<StoreInventoryListRes>> updateStoreInventory(@PathVariable Long storeId,
                                                                                        @PathVariable Long productId, @RequestBody  StoreInventoryUpdateReq req) {
        StoreInventoryListRes res = storeService.updateStoreInventory(storeId, productId, req);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, res));
    }

}
