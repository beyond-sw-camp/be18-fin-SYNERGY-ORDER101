package com.synerge.order101.warehouse.model.service;

import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.purchase.model.dto.CalculatedAutoItem;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.warehouse.model.dto.response.InventoryResponseDto;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final StoreOrderDetailRepository storeOrderDetailRepository;

    // 재고 추가
    public void createInventory(Product product) {
        Warehouse warehouse = warehouseRepository.findById(1L).orElseThrow();

        WarehouseInventory inventory = WarehouseInventory.builder()
                .warehouse(warehouse)
                .product(product)
                .updatedAt(LocalDateTime.now())
                .build();

        warehouseInventoryRepository.save(inventory);
    }

    // 재고 조회
    @Override
    @Transactional
    public Page<InventoryResponseDto> getInventoryList(int page, int numOfRows, Long largeCategoryId, Long mediumCategoryId, Long smallCategoryId) {

        Pageable pageable = PageRequest.of(page - 1, numOfRows);

        return warehouseInventoryRepository.searchInventory(largeCategoryId, mediumCategoryId, smallCategoryId, pageable);
    }

    // 출고 반영
    @Override
    @Transactional
    public void decreaseInventory(Long productId, int quantity) {
        WarehouseInventory inventory = warehouseInventoryRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new IllegalStateException("해당 상품의 재고를 찾을 수 없습니다."));

        inventory.decrease(quantity);
    }

    // 입고 반영
    @Override
    @Transactional
    public void increaseInventory(Long productId, int quantity) {
        WarehouseInventory inventory = warehouseInventoryRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new IllegalStateException("해당 상품의 재고를 찾을 수 없습니다."));

        inventory.increase(quantity);
    }

    // 안전재고 업데이트
    @Override
    @Transactional
    public void updateDailySafetyStock() {
        List<WarehouseInventory> inventoryList =
                warehouseInventoryRepository.findAllWithProductAndSupplier();

        for (WarehouseInventory inv : inventoryList) {
            Long productId = inv.getProduct().getProductId();

            List<Integer> sales = storeOrderDetailRepository.findDailySalesQtySince(
                    productId,
                    LocalDateTime.now().minusDays(30)
            );

            if (sales.isEmpty()) continue;

            // 최고판매량
            int dMax = sales.stream().mapToInt(i -> i).max().orElse(0);
            // 평균판매량
            double dAvg = sales.stream().mapToInt(i -> i).average().orElse(0);

            // 리드타임
            List<ProductSupplier> psList = inv.getProduct().getProductSupplier();

            ProductSupplier ps = psList.getFirst();
            int lt = ps.getLeadTimeDays();

            // 안전재고 계산
            int safety = (int) Math.max(0,
                    Math.ceil((dMax - dAvg) * lt)
            );

            // 재고 업데이트
            inv.updateSafetyQty(safety);
        }
    }

    // 자동 발주 대상 조회
    @Override
    @Transactional
    public List<CalculatedAutoItem> getAutoPurchaseItems() {
        List<CalculatedAutoItem> result = new ArrayList<>();

        List<WarehouseInventory> inventoryList = warehouseInventoryRepository.findAllWithProduct();

        for (WarehouseInventory inv : inventoryList) {

            Long productId = inv.getProduct().getProductId();
            int currentQty = inv.getOnHandQuantity();
            int safetyQty = inv.getSafetyQuantity();

            // 최근 30일 판매량 조회
            List<Integer> sales = storeOrderDetailRepository.findDailySalesQtySince(
                    productId,
                    LocalDateTime.now().minusDays(30)
            );

            if (sales.isEmpty()) continue;

            double avgDailySales = sales.stream().mapToInt(i -> i).average().orElse(0);

            // 리드타임 조회
            List<ProductSupplier> psList = inv.getProduct().getProductSupplier();

            ProductSupplier ps = psList.getFirst();
            int leadTime = ps.getLeadTimeDays();

            // 목표재고 계산
            int targetStock = (int) Math.ceil(safetyQty + (avgDailySales * leadTime));

            // 자동발주 필요 여부
            if (currentQty < safetyQty) {
                int orderQty = targetStock - currentQty;

                if (orderQty > 0) {
                    result.add(new CalculatedAutoItem(
                            productId,
                            orderQty,
                            ps.getSupplier().getSupplierId()
                    ));
                }
            }
        }

        return result;
    }
}
