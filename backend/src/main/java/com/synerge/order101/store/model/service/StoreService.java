package com.synerge.order101.store.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.store.model.dto.*;
import org.springframework.data.domain.Pageable;

public interface StoreService {
    ItemsResponseDto<StoreListRes> getStores(Pageable pageable, String keyword);

    StoreRes createStore(StoreCreateReq req);

    StoreRes getStore(Long storeId);

    void updateStatus(Long storeId, boolean isActive);

    ItemsResponseDto<StoreInventoryListRes> getStoreInventoryList(Long storeId, int page, int numOfRows);

    StoreInventoryListRes createStoreInventory(Long storeId, StoreInventoryCreateReq req);

    StoreInventoryListRes updateStoreInventory(Long storeId, Long productId, StoreInventoryUpdateReq req);

    ItemsResponseDto<String> getDistinctAddresses();

    ItemsResponseDto<StoreListRes> searchStores(Pageable pageable, String address, String storeName);
}