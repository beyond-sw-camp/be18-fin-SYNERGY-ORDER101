package com.synerge.order101.store.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.product.exception.ProductErrorCode;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.store.exception.StoreErrorCode;
import com.synerge.order101.store.model.dto.*;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.entity.StoreInventory;
import com.synerge.order101.store.model.repository.StoreInventoryRepository;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto<StoreListRes> getStores(int page, int numOfRows, String keyword) {
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, numOfRows, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Store> resultPage;
        if (keyword != null && !keyword.isBlank()) {
            resultPage = storeRepository.findByStoreNameContainingIgnoreCase(keyword, pageable);
        } else {
            resultPage = storeRepository.findAll(pageable);
        }

        List<StoreListRes> items = resultPage.getContent().stream()
                .map(s -> StoreListRes.builder()
                        .storeId(s.getStoreId())
                        .storeCode(s.getStoreCode())
                        .storeName(s.getStoreName())
                        .address(s.getAddress())
                        .contactNumber(s.getContactNumber())
                        .isActive(s.isActive())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        int totalCount = (int) resultPage.getTotalElements();
        return new ItemsResponseDto<>(HttpStatus.OK, items, page, totalCount);
    }

    @Override
    @Transactional
    public StoreRes createStore(StoreCreateReq req) {
        Warehouse wh = null;
        if (req.getDefaultWarehouseId() != null) {
            wh = warehouseRepository.findById(req.getDefaultWarehouseId()).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
        }

        Store s = Store.builder()
                .storeCode(req.getStoreCode())
                .storeName(req.getStoreName())
                .address(req.getAddress())
                .contactNumber(req.getContactNumber())
                .defaultWarehouse(wh)
                .isActive(true)
                .build();

        Store saved = storeRepository.save(s);

        return StoreRes.builder()
                .storeId(saved.getStoreId())
                .storeCode(saved.getStoreCode())
                .storeName(saved.getStoreName())
                .address(saved.getAddress())
                .contactNumber(saved.getContactNumber())
                .defaultWarehouseId(saved.getDefaultWarehouse() != null ? saved.getDefaultWarehouse().getWarehouseId() : null)
                .isActive(saved.isActive())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreRes getStore(Long storeId) {
        Store s = storeRepository.findById(storeId).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

        return StoreRes.builder()
                .storeId(s.getStoreId())
                .storeCode(s.getStoreCode())
                .storeName(s.getStoreName())
                .address(s.getAddress())
                .contactNumber(s.getContactNumber())
                .defaultWarehouseId(s.getDefaultWarehouse() != null ? s.getDefaultWarehouse().getWarehouseId() : null)
                .isActive(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void updateStatus(Long storeId, boolean isActive) {
        Store s = storeRepository.findById(storeId).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
        s.updateActive(isActive);
        storeRepository.save(s);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto<StoreInventoryListRes> getStoreInventoryList(Long storeId, int page, int numOfRows) {
        Store s = storeRepository.findById(storeId).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, numOfRows);

        Page<StoreInventory> invPage = storeInventoryRepository.findByStore(s, pageable);

        List<StoreInventoryListRes> items = invPage.getContent().stream()
                .map(inv -> StoreInventoryListRes.builder()
                        .storeInventoryId(inv.getStoreInventoryId())
                        .productId(inv.getProduct() != null ? inv.getProduct().getProductId() : null)
                        .productCode(inv.getProduct() != null ? inv.getProduct().getProductCode() : null)
                        .productName(inv.getProduct() != null ? inv.getProduct().getProductName() : null)
                        .onHandQty(inv.getOnHandQty())
                        .inTransitQty(inv.getInTransitQty())
                        .safetyQty(inv.getSafetyQuantity())
                        .updatedAt(inv.getUpdatedAt())
                        .build())
                .toList();

        int totalCount = (int) invPage.getTotalElements();
        return new ItemsResponseDto<>(HttpStatus.OK, items, page, totalCount);
    }

    @Override
    @Transactional
    public StoreInventoryListRes createStoreInventory(Long storeId, StoreInventoryCreateReq req) {
        Store s = storeRepository.findById(storeId).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
        Product p = productRepository.findById(req.getProductId()).orElseThrow(() -> new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Optional<StoreInventory> existing = storeInventoryRepository.findByStoreAndProduct(s, p);
        if (existing.isPresent()) {
            throw new CustomException(StoreErrorCode.STORE_NOT_FOUND);
        }

        StoreInventory inv = StoreInventory.builder()
                .store(s)
                .product(p)
                .onHandQty(req.getOnHandQty() != null ? req.getOnHandQty() : 0)
                .inTransitQty(req.getInTransitQty() != null ? req.getInTransitQty() : 0)
                .safetyQuantity(req.getSafetyQty())
                .build();

        StoreInventory saved = storeInventoryRepository.save(inv);

        return StoreInventoryListRes.builder()
                .storeInventoryId(saved.getStoreInventoryId())
                .productId(saved.getProduct() != null ? saved.getProduct().getProductId() : null)
                .productCode(saved.getProduct() != null ? saved.getProduct().getProductCode() : null)
                .productName(saved.getProduct() != null ? saved.getProduct().getProductName() : null)
                .onHandQty(saved.getOnHandQty())
                .inTransitQty(saved.getInTransitQty())
                .safetyQty(saved.getSafetyQuantity())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public StoreInventoryListRes updateStoreInventory(Long storeId, Long productId, StoreInventoryUpdateReq req) {
        Store s = storeRepository.findById(storeId).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
        Product p = productRepository.findById(productId).orElseThrow(() -> new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

        StoreInventory inv = storeInventoryRepository.findByStoreAndProduct(s, p)
                .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

        if (req.getOnHandQty() != null) inv.updateOnHandQty(req.getOnHandQty());
        if (req.getInTransitQty() != null) inv.setInTransitQty(req.getInTransitQty());
        if (req.getSafetyQty() != null) inv.updateSafetyQty(req.getSafetyQty());

        StoreInventory saved = storeInventoryRepository.save(inv);

        return StoreInventoryListRes.builder()
                .storeInventoryId(saved.getStoreInventoryId())
                .productId(saved.getProduct() != null ? saved.getProduct().getProductId() : null)
                .productCode(saved.getProduct() != null ? saved.getProduct().getProductCode() : null)
                .productName(saved.getProduct() != null ? saved.getProduct().getProductName() : null)
                .onHandQty(saved.getOnHandQty())
                .inTransitQty(saved.getInTransitQty())
                .safetyQty(saved.getSafetyQuantity())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
