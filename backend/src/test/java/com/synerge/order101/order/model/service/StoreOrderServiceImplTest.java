package com.synerge.order101.order.model.service;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.order.exception.errorcode.OrderErrorCode;
import com.synerge.order101.order.model.dto.StoreOrderCreateRequest;
import com.synerge.order101.order.model.dto.StoreOrderCreateResponseDto;
import com.synerge.order101.order.model.dto.StoreOrderDetailResponseDto;
import com.synerge.order101.order.model.dto.StoreOrderSummaryResponseDto;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.order.model.repository.StoreOrderStatusLogRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
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
@DisplayName("StoreOrderServiceImplTest")
class StoreOrderServiceImplTest {

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Mock
    private StoreOrderDetailRepository storeOrderDetailRepository;

    @Mock
    private StoreOrderStatusLogRepository storeOrderStatusLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StoreOrderServiceImpl storeOrderService;

    private Store store;
    private Warehouse warehouse;
    private User user;
    private Product product;
    private StoreOrder storeOrder;
    private StoreOrderDetail orderDetail;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .storeId(1L)
                .storeName("테스트 가맹점")
                .storeCode("ST001")
                .address("서울시 강남구")
                .contactNumber("02-1234-5678")
                .isActive(true)
                .build();

        warehouse = new Warehouse();

        user = User.create(
                "hong@test.com",
                "encodedPassword",
                "홍길동",
                Role.STORE_ADMIN,
                "010-1234-5678",
                null
        );

        product = Product.builder()
                .productId(1L)
                .productName("테스트 상품")
                .productCode("TEST-001")
                .price(BigDecimal.valueOf(10000))
                .build();

        storeOrder = StoreOrder.builder()
                .storeOrderId(1L)
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .orderNo("OR202412030001")
                .orderStatus(OrderStatus.SUBMITTED)
                .shipmentStatus(ShipmentStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        orderDetail = StoreOrderDetail.builder()
                .storeOrderDetailId(1L)
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(10)
                .unitPrice(BigDecimal.valueOf(10000))
                .amount(BigDecimal.valueOf(100000))
                .build();
    }

    @Test
    @DisplayName("주문 목록 조회 성공")
    void findOrders_Success() {
        // given
        TradeSearchCondition condition = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        List<StoreOrder> orders = List.of(storeOrder);
        Page<StoreOrder> orderPage = new PageImpl<>(orders, pageable, 1);

        given(storeOrderRepository.search(condition, pageable)).willReturn(orderPage);

        // when
        Page<StoreOrderSummaryResponseDto> result = storeOrderService.findOrders(condition, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(storeOrderRepository, times(1)).search(condition, pageable);
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void findStoreOrderDetails_Success() {
        // given
        Long orderId = 1L;
        List<StoreOrderDetail> details = List.of(orderDetail);

        given(storeOrderRepository.findById(orderId)).willReturn(Optional.of(storeOrder));
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(orderId)).willReturn(details);

        // when
        StoreOrderDetailResponseDto result = storeOrderService.findStoreOrderDetails(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreOrderId()).isEqualTo(orderId);
        assertThat(result.getStoreOrderNo()).isEqualTo("OR202412030001");
        assertThat(result.getRequesterName()).isEqualTo("홍길동");
        assertThat(result.getOrderItems()).hasSize(1);
        verify(storeOrderRepository, times(1)).findById(orderId);
        verify(storeOrderDetailRepository, times(1)).findByStoreOrder_StoreOrderId(orderId);
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 존재하지 않는 주문")
    void findStoreOrderDetails_NotFound() {
        // given
        Long orderId = 999L;
        given(storeOrderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeOrderService.findStoreOrderDetails(orderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);

        verify(storeOrderRepository, times(1)).findById(orderId);
        verify(storeOrderDetailRepository, never()).findByStoreOrder_StoreOrderId(anyLong());
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        StoreOrderCreateRequest.Item item = StoreOrderCreateRequest.Item.builder()
                .productId(1L)
                .orderQty(10)
                .build();

        StoreOrderCreateRequest request = StoreOrderCreateRequest.builder()
                .storeId(1L)
                .warehouseId(1L)
                .userId(1L)
                .remark("테스트 주문")
                .items(List.of(item))
                .build();

        given(storeRepository.findById(1L)).willReturn(Optional.of(store));
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(storeOrderRepository.save(any(StoreOrder.class))).willReturn(storeOrder);
        given(storeOrderDetailRepository.save(any(StoreOrderDetail.class))).willReturn(orderDetail);

        // when
        StoreOrderCreateResponseDto result = storeOrderService.createOrder(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreOrderId()).isEqualTo(1L);
        verify(storeRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).findById(1L);
        verify(storeOrderRepository, times(1)).save(any(StoreOrder.class));
        verify(storeOrderDetailRepository, times(1)).save(any(StoreOrderDetail.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 가맹점")
    void createOrder_StoreNotFound() {
        // given
        StoreOrderCreateRequest request = StoreOrderCreateRequest.builder()
                .storeId(999L)
                .warehouseId(1L)
                .userId(1L)
                .items(new ArrayList<>())
                .build();

        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeOrderService.createOrder(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.STORE_NOT_FOUND);

        verify(storeRepository, times(1)).findById(999L);
        verify(storeOrderRepository, never()).save(any(StoreOrder.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 사용자")
    void createOrder_UserNotFound() {
        // given
        StoreOrderCreateRequest request = StoreOrderCreateRequest.builder()
                .storeId(1L)
                .warehouseId(1L)
                .userId(999L)
                .items(new ArrayList<>())
                .build();

        given(storeRepository.findById(1L)).willReturn(Optional.of(store));
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeOrderService.createOrder(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);

        verify(userRepository, times(1)).findById(999L);
        verify(storeOrderRepository, never()).save(any(StoreOrder.class));
    }

    @Test
    @DisplayName("빈 주문 목록 조회")
    void findOrders_EmptyResult() {
        // given
        TradeSearchCondition condition = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<StoreOrder> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(storeOrderRepository.search(condition, pageable)).willReturn(emptyPage);

        // when
        Page<StoreOrderSummaryResponseDto> result = storeOrderService.findOrders(condition, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(storeOrderRepository, times(1)).search(condition, pageable);
    }
}
