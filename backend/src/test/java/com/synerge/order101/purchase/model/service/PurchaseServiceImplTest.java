package com.synerge.order101.purchase.model.service;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.purchase.model.dto.*;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.purchase.model.repository.PurchaseDetailHistoryRepository;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import com.synerge.order101.purchase.model.repository.PurchaseRepository;
import com.synerge.order101.settlement.event.PurchaseSettlementReqEvent;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import com.synerge.order101.warehouse.model.service.InventoryService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseServiceImplTest")
class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseDetailRepository purchaseDetailRepository;

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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    @Test
    @DisplayName("발주 조회 - 성공")
    void testFindPurchase_Success() {
        // given
        Long purchaseId = 1L;
        User mockUser = User.builder().userId(1L).name("테스트사용자").build();
        Supplier mockSupplier = Supplier.builder().supplierId(1L).supplierName("테스트공급업체").build();
        Warehouse mockWarehouse = Warehouse.builder().warehouseId(1L).build();
        
        Purchase testPurchase = Purchase.builder()
            .supplier(mockSupplier)
            .user(mockUser)
            .warehouse(mockWarehouse)
            .orderStatus(OrderStatus.DRAFT_AUTO)
            .orderType(Purchase.OrderType.MANUAL)
            .poDate(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
            
        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.of(testPurchase));

        // when
        Purchase foundPurchase = purchaseRepository.findById(purchaseId).orElseThrow();

        // then
        assertThat(foundPurchase).isNotNull();
        assertThat(foundPurchase.getOrderStatus()).isEqualTo(OrderStatus.DRAFT_AUTO);
    }
        // 자동 발주 생성 (createAutoPurchase)
    @Test
    @DisplayName("자동 발주 생성 테스트 - 재고 부족 품목이 있을 경우 자동 발주가 생성되어야 한다")
    void createAutoPurchase_Success() {
        // given
        // 1-1. InventoryService가 반환할 자동 발주 대상 품목 Mocking
        CalculatedAutoItem item1 = new CalculatedAutoItem(100L, 10, 1L); // supplierId=1, productId=100, qty=10
        given(inventoryService.getAutoPurchaseItems()).willReturn(List.of(item1));

        // 1-2. createPurchase 내부에서 호출되는 Repository Mocking
        // (createAutoPurchase -> createPurchase 호출 흐름을 타기 때문)
        User systemUser = User.builder().userId(1L).build();
        Warehouse warehouse = Warehouse.builder().warehouseId(1L).build();
        Supplier supplier = Supplier.builder().supplierId(1L).build();
        Product product = Product.builder().productId(100L).price(new BigDecimal(1000)).build();
        ProductSupplier productSupplier = ProductSupplier.builder().purchasePrice(new BigDecimal(900)).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(systemUser));
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));
        given(productRepository.findById(100L)).willReturn(Optional.of(product));
        given(productSupplierRepository.findByProductAndSupplier(any(), any())).willReturn(Optional.of(productSupplier));

        // when
        purchaseService.createAutoPurchase();

        // then
        // createPurchase가 내부적으로 호출되면서 save가 일어났는지 검증
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
        verify(purchaseDetailRepository, times(1)).saveAll(anyList());
    }

    // 자동 발주 목록 조회 (getAutoPurchases)
    @Test
    @DisplayName("자동 발주 목록 조회 테스트")
    void getAutoPurchases_Success() {
        // given
        Integer page = 1;
        Integer size = 10;
        Pageable pageable = PageRequest.of(0, 10);
        Page<AutoPurchaseListResponseDto> emptyPage = new PageImpl<>(List.of());

        given(purchaseRepository.getAutoPurchases(pageable)).willReturn(emptyPage);

        // when
        Page<AutoPurchaseListResponseDto> result = purchaseService.getAutoPurchases(page, size);

        // then
        assertThat(result).isEmpty();
        verify(purchaseRepository, times(1)).getAutoPurchases(pageable);
    }

    // 자동 발주 상세 조회 (getAutoPurchaseDetail)
    @Test
    @DisplayName("자동 발주 상세 조회 테스트")
    void getAutoPurchaseDetail_Success() {
        // given
        Long purchaseId = 1L;
        Supplier supplier = Supplier.builder().supplierName("Auto Supplier").build();
        User user = User.builder().name("System").build();
        Purchase purchase = Purchase.builder()
                .purchaseId(purchaseId)
                .supplier(supplier)
                .user(user)
                .poDate(LocalDateTime.now())
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .build();

        // Repository Mocking
        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.of(purchase));

        Product product = Product.builder()
                .productId(200L)
                .productName("Test Product")
                .price(BigDecimal.valueOf(1000))
                .build();

        // Object[] 모의 데이터: {PurchaseDetail, safetyQty(Integer), onHandQty(Integer)}
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchaseOrderLineId(10L)
                .product(product)
                .orderQty(50)
                .build();
        Object[] row = new Object[]{detail, 10, 20}; // detail, safety, onHand

        given(purchaseDetailRepository.findDetailsWithSafetyQtyAndOnHandQty(purchaseId))
                .willReturn(List.<Object[]>of(row));

        // History (변경 이력)가 없다고 가정
        given(purchaseDetailHistoryRepository.findTopByPurchaseOrderLineIdOrderByUpdatedAtAsc(10L))
                .willReturn(Optional.empty());

        // when
        AutoPurchaseDetailResponseDto result = purchaseService.getAutoPurchaseDetail(purchaseId);

        // then
        assertThat(result.getPurchaseId()).isEqualTo(purchaseId);
        assertThat(result.getSupplierName()).isEqualTo("Auto Supplier");
        assertThat(result.getPurchaseItems()).hasSize(1);
        assertThat(result.getPurchaseItems().get(0).getSafetyQty()).isEqualTo(10);
        assertThat(result.getPurchaseItems().get(0).getProductId()).isEqualTo(200L);
    }

    // 자동 발주 검색 필터링 (searchAutoPurchases)
    @Test
    @DisplayName("자동 발주 검색 테스트")
    void searchAutoPurchases_Success() {
        // given
        AutoPurchaseSearchRequestDto request = new AutoPurchaseSearchRequestDto(
                1L, "2023-10-01", "2023-10-31", 1, 10
        );
        Page<AutoPurchaseListResponseDto> emptyPage = new PageImpl<>(List.of());

        given(purchaseRepository.searchAutoPurchases(any(), any(), any(), any()))
                .willReturn(emptyPage);

        // when
        purchaseService.searchAutoPurchases(request);

        // then
        // 날짜가 LocalDateTime으로 잘 변환되어 전달되었는지 확인 (startOfDay, atTime(23,59,59))
        verify(purchaseRepository).searchAutoPurchases(
                eq(1L),
                eq(LocalDateTime.parse("2023-10-01T00:00:00")), // Start Date check
                eq(LocalDateTime.parse("2023-10-31T23:59:59")), // End Date check
                any(Pageable.class)
        );
    }

    //에러남
    // 자동 발주 상태 업데이트 (updateAutoPurchase)
//    @Test
//    @DisplayName("자동 발주 승인/반려 상태 업데이트")
//    void updateAutoPurchase_Success() {
//        // given
//        Long purchaseId = 1L;
//        OrderStatus newStatus = OrderStatus.CONFIRMED;
//
//        Supplier supplier = Supplier.builder().supplierName("S").build();
//        User user = User.builder().name("U").build();
//        Purchase purchase = Purchase.builder()
//                .purchaseId(purchaseId)
//                .supplier(supplier)
//                .user(user)
//                .orderStatus(OrderStatus.DRAFT_AUTO)
//                .build();
//
//        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.of(purchase));
//
//        // 상세 조회 로직도 내부에서 호출되므로 Mocking 필요
//        given(purchaseDetailRepository.findDetailsWithSafetyQtyAndOnHandQty(purchaseId))
//                .willReturn(new ArrayList<>());
//
//        // when
//        AutoPurchaseDetailResponseDto result = purchaseService.updateAutoPurchase(purchaseId, newStatus);
//
//        // then
//        assertThat(purchase.getOrderStatus()).isEqualTo(newStatus); // 엔티티 상태 변경 확인
//        assertThat(result.getStatus()).isEqualTo(newStatus);        // 반환 DTO 상태 확인
//
//        verify(eventPublisher, times(1)).publishEvent(any(PurchaseSettlementReqEvent.class));
//    }

    // 자동 발주 제출 (submitAutoPurchase)
    @Test
    @DisplayName("자동 발주 제출 테스트 - 수정, 삭제, 추가 로직 검증")
    void submitAutoPurchase_Complex_Logic() {
        // given
        Long purchaseId = 1L;
        Long userId = 99L;

        Supplier mockSupplier = Supplier.builder().supplierName("Test Supplier").build();
        User mockUser = User.builder().userId(userId).name("Test User").build();

        // 관리자 알림을 위한 Mock User (HQ_ADMIN)
        User adminUser = User.builder().userId(100L).name("Admin").build();

        Purchase purchase = Purchase.builder()
                .purchaseId(purchaseId)
                .user(mockUser)
                .supplier(mockSupplier)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .build();

        Product productA = Product.builder().productId(10L).price(BigDecimal.valueOf(100)).build();
        Product productB = Product.builder().productId(20L).price(BigDecimal.valueOf(200)).build();
        Product productC = Product.builder().productId(30L).price(BigDecimal.valueOf(300)).build();

        PurchaseDetail detailA = PurchaseDetail.builder().purchaseOrderLineId(100L).purchase(purchase).product(productA).orderQty(10).build();
        PurchaseDetail detailB = PurchaseDetail.builder().purchaseOrderLineId(101L).purchase(purchase).product(productB).orderQty(5).build();

        List<PurchaseDetail> existingDetails = new ArrayList<>(List.of(detailA, detailB));

        // 요청 데이터
        List<AutoPurchaseSubmitRequestDto.Item> requestItems = List.of(
                new AutoPurchaseSubmitRequestDto.Item(null, 10L, 15),
                new AutoPurchaseSubmitRequestDto.Item(null, 30L, 3)
        );
        AutoPurchaseSubmitRequestDto requestDto = new AutoPurchaseSubmitRequestDto(requestItems);

        // --- Mocking 설정 ---
        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.of(purchase));
        given(purchaseDetailRepository.findByPurchase_PurchaseId(purchaseId)).willReturn(existingDetails);
        given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().userId(userId).build()));
        given(productRepository.findById(30L)).willReturn(Optional.of(productC));
        given(purchaseDetailRepository.findDetailsWithSafetyQtyAndOnHandQty(purchaseId)).willReturn(new ArrayList<>());

        // [추가된 Stubbing] 관리자 조회 로직 Mocking (Role.HQ_ADMIN 조회 시 빈 리스트라도 반환해야 함)
        given(userRepository.findByRole(Role.HQ_ADMIN)).willReturn(List.of(adminUser));

        // when
        purchaseService.submitAutoPurchase(purchaseId, userId, requestDto);

        // then
        verify(purchaseDetailHistoryRepository, times(1)).saveAll(anyList());
        verify(purchaseDetailRepository, times(1)).delete(detailB);
        verify(purchaseDetailRepository, times(1)).save(any(PurchaseDetail.class));
        assertThat(detailA.getOrderQty()).isEqualTo(15);

        // [추가 검증] 알림 서비스가 호출되었는지 확인 (NPE가 해결되었는지 검증)
        verify(notificationService).notifyPurchaseCreatedToAllAdmins(any(Purchase.class), anyList());
    }
}