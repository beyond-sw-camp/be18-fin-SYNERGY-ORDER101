package com.synerge.order101.store.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.store.model.dto.*;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.entity.StoreInventory;
import com.synerge.order101.store.model.repository.StoreInventoryRepository;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@DisplayName("StoreServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreServiceImplTest {

    @InjectMocks
    private StoreServiceImpl storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreInventoryRepository storeInventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    // ========================
    // getStores
    // ========================
    @Test
    @Order(1)
    @DisplayName("가맹점 목록 조회 - 키워드 없음")
    void getStores_withoutKeyword_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(1L);
        given(store.getStoreCode()).willReturn("ST001");
        given(store.getStoreName()).willReturn("테스트 가맹점");
        given(store.getAddress()).willReturn("서울시 강남구");
        given(store.getContactNumber()).willReturn("02-1234-5678");
        given(store.isActive()).willReturn(true);
        given(store.getCreatedAt()).willReturn(LocalDateTime.now());

        Page<Store> storePage = new PageImpl<>(List.of(store), pageable, 1);
        given(storeRepository.findAll(any(Pageable.class))).willReturn(storePage);

        // when
        ItemsResponseDto<StoreListRes> result = storeService.getStores(pageable, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStoreName()).isEqualTo("테스트 가맹점");
        verify(storeRepository).findAll(any(Pageable.class));
    }

    @Test
    @Order(2)
    @DisplayName("가맹점 목록 조회 - 키워드 있음")
    void getStores_withKeyword_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "강남";

        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(1L);
        given(store.getStoreCode()).willReturn("ST001");
        given(store.getStoreName()).willReturn("강남점");
        given(store.getAddress()).willReturn("서울시 강남구");
        given(store.getContactNumber()).willReturn("02-1234-5678");
        given(store.isActive()).willReturn(true);
        given(store.getCreatedAt()).willReturn(LocalDateTime.now());

        Page<Store> storePage = new PageImpl<>(List.of(store), pageable, 1);
        given(storeRepository.findByStoreNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .willReturn(storePage);

        // when
        ItemsResponseDto<StoreListRes> result = storeService.getStores(pageable, keyword);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStoreName()).isEqualTo("강남점");
        verify(storeRepository).findByStoreNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
    }

    // ========================
    // createStore
    // ========================
    @Test
    @Order(3)
    @DisplayName("가맹점 생성 성공")
    void createStore_Success() {
        // given
        StoreCreateReq req = StoreCreateReq.builder()
                .storeName("신규 가맹점")
                .address("서울시 서초구")
                .contactNumber("02-9999-8888")
                .defaultWarehouseId(1L)
                .build();

        Warehouse warehouse = mock(Warehouse.class);
        given(warehouse.getWarehouseId()).willReturn(1L);
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        Store savedStore = mock(Store.class);
        given(savedStore.getStoreId()).willReturn(1L);
        given(savedStore.getStoreCode()).willReturn("ST001");
        given(savedStore.getStoreName()).willReturn("신규 가맹점");
        given(savedStore.getAddress()).willReturn("서울시 서초구");
        given(savedStore.getContactNumber()).willReturn("02-9999-8888");
        given(savedStore.getDefaultWarehouse()).willReturn(warehouse);
        given(savedStore.isActive()).willReturn(true);
        given(savedStore.getCreatedAt()).willReturn(LocalDateTime.now());

        given(storeRepository.save(any(Store.class))).willReturn(savedStore);

        // when
        StoreRes result = storeService.createStore(req);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreName()).isEqualTo("신규 가맹점");
        assertThat(result.getDefaultWarehouseId()).isEqualTo(1L);
        verify(storeRepository, times(2)).save(any(Store.class));
    }

    // ========================
    // getStore
    // ========================
    @Test
    @Order(4)
    @DisplayName("가맹점 상세 조회 성공")
    void getStore_Success() {
        // given
        Long storeId = 1L;
        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);
        given(store.getStoreCode()).willReturn("ST001");
        given(store.getStoreName()).willReturn("테스트 가맹점");
        given(store.getAddress()).willReturn("서울시 강남구");
        given(store.getContactNumber()).willReturn("02-1234-5678");
        given(store.getDefaultWarehouse()).willReturn(null);
        given(store.isActive()).willReturn(true);
        given(store.getCreatedAt()).willReturn(LocalDateTime.now());

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // when
        StoreRes result = storeService.getStore(storeId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreId()).isEqualTo(storeId);
        assertThat(result.getStoreName()).isEqualTo("테스트 가맹점");
        verify(storeRepository).findById(storeId);
    }

    @Test
    @Order(5)
    @DisplayName("가맹점 상세 조회 실패 - 존재하지 않음")
    void getStore_NotFound() {
        // given
        Long storeId = 999L;
        given(storeRepository.findById(storeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getStore(storeId))
                .isInstanceOf(CustomException.class);
    }

    // ========================
    // updateStatus
    // ========================
    @Test
    @Order(6)
    @DisplayName("가맹점 상태 변경 성공")
    void updateStatus_Success() {
        // given
        Long storeId = 1L;
        Store store = mock(Store.class);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(storeRepository.save(store)).willReturn(store);

        // when
        storeService.updateStatus(storeId, false);

        // then
        verify(store).updateActive(false);
        verify(storeRepository).save(store);
    }

    // ========================
    // getStoreInventoryList
    // ========================
    @Test
    @Order(7)
    @DisplayName("가맹점 재고 목록 조회 성공")
    void getStoreInventoryList_Success() {
        // given
        Long storeId = 1L;
        int page = 1;
        int numOfRows = 10;

        Store store = mock(Store.class);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        Product product = mock(Product.class);
        given(product.getProductId()).willReturn(1L);
        given(product.getProductCode()).willReturn("PRD001");
        given(product.getProductName()).willReturn("테스트 상품");

        StoreInventory inventory = mock(StoreInventory.class);
        given(inventory.getStoreInventoryId()).willReturn(1L);
        given(inventory.getProduct()).willReturn(product);
        given(inventory.getOnHandQty()).willReturn(100);
        given(inventory.getInTransitQty()).willReturn(10);
        given(inventory.getSafetyQuantity()).willReturn(20);

        Page<StoreInventory> inventoryPage = new PageImpl<>(List.of(inventory));
        given(storeInventoryRepository.findByStore(eq(store), any(Pageable.class)))
                .willReturn(inventoryPage);

        // when
        ItemsResponseDto<StoreInventoryListRes> result = 
                storeService.getStoreInventoryList(storeId, page, numOfRows);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("테스트 상품");
        assertThat(result.getItems().get(0).getOnHandQty()).isEqualTo(100);
    }
}
