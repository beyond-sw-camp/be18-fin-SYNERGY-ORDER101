package com.synerge.order101.warehouse.model.service;

import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.purchase.model.dto.CalculatedAutoItem;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.warehouse.model.dto.response.InventoryResponseDto;
import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import com.synerge.order101.warehouse.model.service.InventoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
@DisplayName("InventoryServiceImplTest")
@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;
    @Mock
    private StoreOrderDetailRepository storeOrderDetailRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;


    // ========================
    // getInventoryList
    // ========================
    @Test
    void getInventoryList_shouldReturnPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryResponseDto> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(warehouseInventoryRepository.searchInventory(null, null, null, pageable))
                .thenReturn(mockPage);

        Page<InventoryResponseDto> result =
                inventoryService.getInventoryList(1, 10, null, null, null);

        assertThat(result).isEqualTo(mockPage);
        verify(warehouseInventoryRepository, times(1))
                .searchInventory(null, null, null, pageable);
    }


    // ========================
    // decreaseInventory
    // ========================
    @Test
    void decreaseInventory_shouldDecreaseQuantity() {
        WarehouseInventory inventory = mock(WarehouseInventory.class);

        when(warehouseInventoryRepository.findByProduct_ProductId(1L))
                .thenReturn(Optional.of(inventory));

        inventoryService.decreaseInventory(1L, 5);

        verify(inventory, times(1)).decrease(5);
    }


    // ========================
    // increaseInventory
    // ========================
    @Test
    void increaseInventory_shouldIncreaseQuantity() {
        Long productId = 1L;
        int quantity = 10;

        WarehouseInventory inventory = mock(WarehouseInventory.class);
        when(warehouseInventoryRepository.findByProduct_ProductId(productId))
                .thenReturn(Optional.of(inventory));

        inventoryService.increaseInventory(productId, quantity);

        verify(inventory, times(1)).increase(quantity);
    }


    // ========================
    // getAutoPurchaseItems
    // ========================
    @Test
    void getAutoPurchaseItems_shouldReturnCalculatedAutoItems() {
        Product product = mock(Product.class);
        when(product.getProductId()).thenReturn(1L);

        Supplier supplier = mock(Supplier.class);
        when(supplier.getSupplierId()).thenReturn(100L);

        ProductSupplier productSupplier = mock(ProductSupplier.class);
        when(productSupplier.getLeadTimeDays()).thenReturn(5);
        when(productSupplier.getSupplier()).thenReturn(supplier);
        when(product.getProductSupplier()).thenReturn(List.of(productSupplier));

        WarehouseInventory inventory = mock(WarehouseInventory.class);
        when(inventory.getProduct()).thenReturn(product);
        when(inventory.getOnHandQuantity()).thenReturn(5);
        when(inventory.getSafetyQuantity()).thenReturn(15);

        when(warehouseInventoryRepository.findAllWithProduct()).thenReturn(List.of(inventory));
        when(storeOrderDetailRepository.findDailySalesQtySince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(10, 12, 8));   // 평균 10

        List<CalculatedAutoItem> result = inventoryService.getAutoPurchaseItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
        assertThat(result.get(0).getSupplierId()).isEqualTo(100L);
        assertThat(result.get(0).getOrderQty()).isEqualTo(60);
    }
}
