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
    public ItemsResponseDto<StoreListRes> getStores(Pageable pageable, String keyword) {
        // 기본 정렬이 없으면 createdAt DESC를 기본으로 적용
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Store> resultPage;
        if (keyword != null && !keyword.isBlank()) {
            resultPage = storeRepository.findByStoreNameContainingIgnoreCase(keyword, effective);
        } else {
            resultPage = storeRepository.findAll(effective);
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
        // 클라이언트에서 사용하던 1-based 페이지 번호 유지
        int responsePage = effective.getPageNumber() + 1;
        return new ItemsResponseDto<>(HttpStatus.OK, items, responsePage, totalCount);
    }

    @Override
    @Transactional
    public StoreRes createStore(StoreCreateReq req) {
        Warehouse wh = null;
        if (req.getDefaultWarehouseId() != null) {
            wh = warehouseRepository.findById(req.getDefaultWarehouseId()).orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
        }

        // initially persist with a placeholder storeCode (DB requires non-null)
        Store s = Store.builder()
                .storeCode("")
                .storeName(req.getStoreName())
                .address(req.getAddress())
                .contactNumber(req.getContactNumber())
                .defaultWarehouse(wh)
                .isActive(true)
                .build();

        Store saved = storeRepository.save(s);

        // generate storeCode based on the generated storeId, e.g. ST011
        String generatedCode = String.format("ST%03d", saved.getStoreId());
        saved.updateStoreCode(generatedCode);
        Store updated = storeRepository.save(saved);

        return StoreRes.builder()
                .storeId(updated.getStoreId())
                .storeCode(updated.getStoreCode())
                .storeName(updated.getStoreName())
                .address(updated.getAddress())
                .contactNumber(updated.getContactNumber())
                .defaultWarehouseId(updated.getDefaultWarehouse() != null ? updated.getDefaultWarehouse().getWarehouseId() : null)
                .isActive(updated.isActive())
                .createdAt(updated.getCreatedAt())
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

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto<String> getDistinctAddresses() {
        List<String> addresses = storeRepository.findDistinctAddresses();
        int totalCount = addresses.size();
        // 페이지 번호는 1로 고정
        return new ItemsResponseDto<>(HttpStatus.OK, addresses, 1, totalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto<StoreListRes> searchStores(Pageable pageable, String address, String storeName) {
        // 기본 정렬 적용
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Store> resultPage;
        boolean hasAddress = address != null && !address.isBlank();
        boolean hasStoreName = storeName != null && !storeName.isBlank();

        if (hasAddress && hasStoreName) {
            resultPage = storeRepository.findByAddressAndStoreNameContainingIgnoreCase(address, storeName, effective);
        } else if (hasAddress) {
            resultPage = storeRepository.findByAddress(address, effective);
        } else if (hasStoreName) {
            resultPage = storeRepository.findByStoreNameContainingIgnoreCase(storeName, effective);
        } else {
            resultPage = storeRepository.findAll(effective);
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
        int responsePage = effective.getPageNumber() + 1;
        return new ItemsResponseDto<>(HttpStatus.OK, items, responsePage, totalCount);
    }
}
