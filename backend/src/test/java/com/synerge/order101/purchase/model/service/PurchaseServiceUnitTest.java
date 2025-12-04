package com.synerge.order101.purchase.model.service;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.purchase.exception.PurchaseErrorCode;
import com.synerge.order101.purchase.model.dto.PurchaseCreateRequest;
import com.synerge.order101.purchase.model.dto.PurchaseDetailResponseDto;
import com.synerge.order101.purchase.model.dto.PurchaseSummaryResponseDto;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.purchase.model.repository.PurchaseDetailHistoryRepository;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import com.synerge.order101.purchase.model.repository.PurchaseRepository;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import com.synerge.order101.warehouse.model.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseServiceUnitTest")
class PurchaseServiceUnitTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseDetailRepository purchaseDetailRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PurchaseDetailHistoryRepository purchaseDetailHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Supplier supplier;
    private User user;
    private Warehouse warehouse;
    private Product product;
    private Purchase purchase;
    private PurchaseDetail purchaseDetail;

    @BeforeEach
    void setUp() {
        supplier = new Supplier(
                1L,
                "SUP001",
                "테스트 공급사",
                "홍길동",
                "02-1234-5678",
                "서울시 강남구",
                null,
                null
        );

        user = User.create(
                "manager@test.com",
                "encodedPassword",
                "관리자",
                Role.HQ_ADMIN,
                "010-1234-5678",
                null
        );

        warehouse = new Warehouse();

        product = Product.builder()
                .productId(1L)
                .productName("테스트 상품")
                .productCode("PROD001")
                .price(BigDecimal.valueOf(10000))
                .build();

        purchase = Purchase.builder()
                .purchaseId(1L)
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .poNo("PO202412030001")
                .poDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        purchaseDetail = PurchaseDetail.builder()
                .purchaseOrderLineId(1L)
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.valueOf(8000))
                .deadline(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("발주 목록 조회 성공")
    void findPurchases_Success() {
        // given
        TradeSearchCondition condition = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        List<Purchase> purchases = List.of(purchase);
        Page<Purchase> purchasePage = new PageImpl<>(purchases, pageable, 1);

        given(purchaseRepository.search(condition, pageable)).willReturn(purchasePage);

        // when
        Page<PurchaseSummaryResponseDto> result = purchaseService.findPurchases(condition, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(purchaseRepository, times(1)).search(condition, pageable);
    }

    @Test
    @DisplayName("발주 상세 조회 성공")
    void findPurchaseDetailsById_Success() {
        // given
        Long purchaseId = 1L;
        List<PurchaseDetail> details = List.of(purchaseDetail);
        
        ProductSupplier productSupplier = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .purchasePrice(BigDecimal.valueOf(8000))
                .build();

        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.of(purchase));
        given(purchaseDetailRepository.findByPurchase_PurchaseId(purchaseId)).willReturn(details);
        given(productSupplierRepository.findByProduct_ProductIdAndSupplier_SupplierId(anyLong(), anyLong()))
                .willReturn(Optional.of(productSupplier));

        // when
        PurchaseDetailResponseDto result = purchaseService.findPurchaseDetailsById(purchaseId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPurchaseId()).isEqualTo(purchaseId);
        assertThat(result.getPoNo()).isEqualTo("PO202412030001");
        assertThat(result.getSupplierName()).isEqualTo("테스트 공급사");
        assertThat(result.getRequesterName()).isEqualTo("관리자");
        assertThat(result.getPurchaseItems()).hasSize(1);
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(purchaseDetailRepository, times(1)).findByPurchase_PurchaseId(purchaseId);
    }

    @Test
    @DisplayName("발주 상세 조회 실패 - 존재하지 않는 발주")
    void findPurchaseDetailsById_NotFound() {
        // given
        Long purchaseId = 999L;
        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> purchaseService.findPurchaseDetailsById(purchaseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PurchaseErrorCode.PURCHASE_NOT_FOUND);

        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(purchaseDetailRepository, never()).findByPurchase_PurchaseId(anyLong());
    }

    @Test
    @DisplayName("발주 생성 성공")
    void createPurchase_Success() {
        // given
        PurchaseCreateRequest.Item item = PurchaseCreateRequest.Item.builder()
                .productId(1L)
                .orderQty(100)
                .build();

        PurchaseCreateRequest request = PurchaseCreateRequest.builder()
                .userId(1L)
                .warehouseId(1L)
                .supplierId(1L)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .deadline(LocalDate.now().plusDays(7))
                .items(List.of(item))
                .build();

        ProductSupplier productSupplier = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .purchasePrice(BigDecimal.valueOf(8000))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productSupplierRepository.findByProductAndSupplier(product, supplier))
                .willReturn(Optional.of(productSupplier));
        given(purchaseRepository.save(any(Purchase.class))).willReturn(purchase);
        given(purchaseDetailRepository.saveAll(anyList())).willReturn(List.of(purchaseDetail));

        // when
        purchaseService.createPurchase(request);

        // then
        verify(userRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).findById(1L);
        verify(productSupplierRepository, times(1)).findByProductAndSupplier(product, supplier);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
        verify(purchaseDetailRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("발주 생성 실패 - 존재하지 않는 사용자")
    void createPurchase_UserNotFound() {
        // given
        PurchaseCreateRequest request = PurchaseCreateRequest.builder()
                .userId(999L)
                .warehouseId(1L)
                .supplierId(1L)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .deadline(LocalDate.now().plusDays(7))
                .items(new ArrayList<>())
                .build();

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PurchaseErrorCode.PURCHASE_CREATION_FAILED);

        verify(userRepository, times(1)).findById(999L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("발주 생성 실패 - 존재하지 않는 공급사")
    void createPurchase_SupplierNotFound() {
        // given
        PurchaseCreateRequest request = PurchaseCreateRequest.builder()
                .userId(1L)
                .warehouseId(1L)
                .supplierId(999L)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .deadline(LocalDate.now().plusDays(7))
                .items(new ArrayList<>())
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(supplierRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PurchaseErrorCode.PURCHASE_CREATION_FAILED);

        verify(supplierRepository, times(1)).findById(999L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("발주 생성 실패 - 존재하지 않는 창고")
    void createPurchase_WarehouseNotFound() {
        // given
        PurchaseCreateRequest request = PurchaseCreateRequest.builder()
                .userId(1L)
                .warehouseId(999L)
                .supplierId(1L)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .deadline(LocalDate.now().plusDays(7))
                .items(new ArrayList<>())
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(warehouseRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PurchaseErrorCode.PURCHASE_CREATION_FAILED);

        verify(warehouseRepository, times(1)).findById(999L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("빈 발주 목록 조회")
    void findPurchases_EmptyResult() {
        // given
        TradeSearchCondition condition = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Purchase> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(purchaseRepository.search(condition, pageable)).willReturn(emptyPage);

        // when
        Page<PurchaseSummaryResponseDto> result = purchaseService.findPurchases(condition, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(purchaseRepository, times(1)).search(condition, pageable);
    }

    @Test
    @DisplayName("Pageable이 null일 때 기본값 사용")
    void findPurchases_NullPageable() {
        // given
        TradeSearchCondition condition = new TradeSearchCondition();
        List<Purchase> purchases = List.of(purchase);
        Page<Purchase> purchasePage = new PageImpl<>(purchases, PageRequest.of(0, 10), 1);

        given(purchaseRepository.search(any(TradeSearchCondition.class), any(Pageable.class)))
                .willReturn(purchasePage);

        // when
        Page<PurchaseSummaryResponseDto> result = purchaseService.findPurchases(condition, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(purchaseRepository, times(1)).search(any(TradeSearchCondition.class), any(Pageable.class));
    }
}
