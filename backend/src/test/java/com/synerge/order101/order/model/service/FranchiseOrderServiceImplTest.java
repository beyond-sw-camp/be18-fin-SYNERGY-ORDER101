package com.synerge.order101.order.model.service;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.order.model.dto.FranchiseOrderDetailResponseDto;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.order.model.repository.StoreOrderStatusLogRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.shipment.model.entity.Shipment;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import com.synerge.order101.store.model.entity.Store;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("FranchiseOrderServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FranchiseOrderServiceImplTest {

    @InjectMocks
    private FranchiseOrderServiceImpl franchiseOrderService;

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Mock
    private StoreOrderDetailRepository storeOrderDetailRepository;

    @Mock
    private StoreOrderStatusLogRepository storeOrderStatusLogRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    // ========================
    // getFranchiseOrderDetail
    // ========================
    @Test
    @Order(1)
    @DisplayName("가맹점 주문 상세 조회 성공 - 배송 없음")
    void getFranchiseOrderDetail_Success_NoShipment() {
        // given
        Long orderId = 1L;

        Store store = mock(Store.class);
        given(store.getStoreName()).willReturn("강남점");

        Product product = mock(Product.class);
        given(product.getProductCode()).willReturn("PRD001");
        given(product.getProductName()).willReturn("테스트 상품");

        StoreOrderDetail orderDetail = mock(StoreOrderDetail.class);
        given(orderDetail.getProduct()).willReturn(product);
        given(orderDetail.getOrderQty()).willReturn(10);
        given(orderDetail.getUnitPrice()).willReturn(new BigDecimal("10000"));

        StoreOrder storeOrder = mock(StoreOrder.class);
        given(storeOrder.getStoreOrderId()).willReturn(orderId);
        given(storeOrder.getOrderNo()).willReturn("ORD-001");
        given(storeOrder.getStore()).willReturn(store);
        given(storeOrder.getOrderDatetime()).willReturn(LocalDateTime.now());
        given(storeOrder.getOrderStatus()).willReturn(OrderStatus.SUBMITTED);
        given(storeOrder.getShipmentStatus()).willReturn(ShipmentStatus.WAITING);
        given(storeOrder.getCreatedAt()).willReturn(LocalDateTime.now());

        given(storeOrderRepository.findById(orderId)).willReturn(Optional.of(storeOrder));
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(orderId))
                .willReturn(List.of(orderDetail));
        given(shipmentRepository.findByStoreOrder(storeOrder)).willReturn(List.of());
        given(storeOrderStatusLogRepository.findConfirmedTime(orderId)).willReturn(null);

        // when
        FranchiseOrderDetailResponseDto result = franchiseOrderService.getFranchiseOrderDetail(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getOrderNo()).isEqualTo("ORD-001");
        assertThat(result.getStoreName()).isEqualTo("강남점");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSku()).isEqualTo("PRD001");
        assertThat(result.getProgress()).hasSize(5);
    }

    @Test
    @Order(2)
    @DisplayName("가맹점 주문 상세 조회 성공 - 배송 중")
    void getFranchiseOrderDetail_Success_InTransit() {
        // given
        Long orderId = 2L;

        Store store = mock(Store.class);
        given(store.getStoreName()).willReturn("홍대점");

        Product product = mock(Product.class);
        given(product.getProductCode()).willReturn("PRD002");
        given(product.getProductName()).willReturn("테스트 상품2");

        StoreOrderDetail orderDetail = mock(StoreOrderDetail.class);
        given(orderDetail.getProduct()).willReturn(product);
        given(orderDetail.getOrderQty()).willReturn(5);
        given(orderDetail.getUnitPrice()).willReturn(new BigDecimal("20000"));

        StoreOrder storeOrder = mock(StoreOrder.class);
        given(storeOrder.getStoreOrderId()).willReturn(orderId);
        given(storeOrder.getOrderNo()).willReturn("ORD-002");
        given(storeOrder.getStore()).willReturn(store);
        given(storeOrder.getOrderDatetime()).willReturn(LocalDateTime.now());
        given(storeOrder.getOrderStatus()).willReturn(OrderStatus.CONFIRMED);
        given(storeOrder.getShipmentStatus()).willReturn(ShipmentStatus.IN_TRANSIT);
        given(storeOrder.getCreatedAt()).willReturn(LocalDateTime.now());

        Shipment shipment = mock(Shipment.class);
        given(shipment.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(1));
        given(shipment.getUpdatedAt()).willReturn(LocalDateTime.now());
        given(shipment.getShipmentStatus()).willReturn(ShipmentStatus.IN_TRANSIT);

        given(storeOrderRepository.findById(orderId)).willReturn(Optional.of(storeOrder));
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(orderId))
                .willReturn(List.of(orderDetail));
        given(shipmentRepository.findByStoreOrder(storeOrder)).willReturn(List.of(shipment));
        given(storeOrderStatusLogRepository.findConfirmedTime(orderId))
                .willReturn(LocalDateTime.now().minusDays(2));

        // when
        FranchiseOrderDetailResponseDto result = franchiseOrderService.getFranchiseOrderDetail(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isEqualTo("ORD-002");
        assertThat(result.getShipmentStatus()).isEqualTo("IN_TRANSIT");
        assertThat(result.getProgress()).hasSize(5);

        // SHIPPED progress should be done
        var shippedProgress = result.getProgress().stream()
                .filter(p -> p.getKey().equals("shipped"))
                .findFirst();
        assertThat(shippedProgress).isPresent();
        assertThat(shippedProgress.get().isDone()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("가맹점 주문 상세 조회 성공 - 배송 완료")
    void getFranchiseOrderDetail_Success_Delivered() {
        // given
        Long orderId = 3L;

        Store store = mock(Store.class);
        given(store.getStoreName()).willReturn("부산점");

        Product product = mock(Product.class);
        given(product.getProductCode()).willReturn("PRD003");
        given(product.getProductName()).willReturn("테스트 상품3");

        StoreOrderDetail orderDetail = mock(StoreOrderDetail.class);
        given(orderDetail.getProduct()).willReturn(product);
        given(orderDetail.getOrderQty()).willReturn(15);
        given(orderDetail.getUnitPrice()).willReturn(new BigDecimal("30000"));

        StoreOrder storeOrder = mock(StoreOrder.class);
        given(storeOrder.getStoreOrderId()).willReturn(orderId);
        given(storeOrder.getOrderNo()).willReturn("ORD-003");
        given(storeOrder.getStore()).willReturn(store);
        given(storeOrder.getOrderDatetime()).willReturn(LocalDateTime.now().minusDays(5));
        given(storeOrder.getOrderStatus()).willReturn(OrderStatus.CONFIRMED);
        given(storeOrder.getShipmentStatus()).willReturn(ShipmentStatus.DELIVERED);
        given(storeOrder.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(5));

        Shipment shipment = mock(Shipment.class);
        given(shipment.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(3));
        given(shipment.getUpdatedAt()).willReturn(LocalDateTime.now().minusDays(1));
        given(shipment.getShipmentStatus()).willReturn(ShipmentStatus.DELIVERED);

        given(storeOrderRepository.findById(orderId)).willReturn(Optional.of(storeOrder));
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(orderId))
                .willReturn(List.of(orderDetail));
        given(shipmentRepository.findByStoreOrder(storeOrder)).willReturn(List.of(shipment));
        given(storeOrderStatusLogRepository.findConfirmedTime(orderId))
                .willReturn(LocalDateTime.now().minusDays(4));

        // when
        FranchiseOrderDetailResponseDto result = franchiseOrderService.getFranchiseOrderDetail(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getShipmentStatus()).isEqualTo("DELIVERED");

        // DELIVERED progress should be done
        var deliveredProgress = result.getProgress().stream()
                .filter(p -> p.getKey().equals("delivered"))
                .findFirst();
        assertThat(deliveredProgress).isPresent();
        assertThat(deliveredProgress.get().isDone()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("가맹점 주문 상세 조회 실패 - 주문 없음")
    void getFranchiseOrderDetail_NotFound() {
        // given
        Long orderId = 999L;
        given(storeOrderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> franchiseOrderService.getFranchiseOrderDetail(orderId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @Order(5)
    @DisplayName("가맹점 주문 상세 조회 - 여러 상품")
    void getFranchiseOrderDetail_MultipleItems() {
        // given
        Long orderId = 4L;

        Store store = mock(Store.class);
        given(store.getStoreName()).willReturn("대전점");

        Product product1 = mock(Product.class);
        given(product1.getProductCode()).willReturn("PRD001");
        given(product1.getProductName()).willReturn("상품1");

        Product product2 = mock(Product.class);
        given(product2.getProductCode()).willReturn("PRD002");
        given(product2.getProductName()).willReturn("상품2");

        StoreOrderDetail detail1 = mock(StoreOrderDetail.class);
        given(detail1.getProduct()).willReturn(product1);
        given(detail1.getOrderQty()).willReturn(10);
        given(detail1.getUnitPrice()).willReturn(new BigDecimal("5000"));

        StoreOrderDetail detail2 = mock(StoreOrderDetail.class);
        given(detail2.getProduct()).willReturn(product2);
        given(detail2.getOrderQty()).willReturn(20);
        given(detail2.getUnitPrice()).willReturn(new BigDecimal("8000"));

        StoreOrder storeOrder = mock(StoreOrder.class);
        given(storeOrder.getStoreOrderId()).willReturn(orderId);
        given(storeOrder.getOrderNo()).willReturn("ORD-004");
        given(storeOrder.getStore()).willReturn(store);
        given(storeOrder.getOrderDatetime()).willReturn(LocalDateTime.now());
        given(storeOrder.getOrderStatus()).willReturn(OrderStatus.SUBMITTED);
        given(storeOrder.getShipmentStatus()).willReturn(ShipmentStatus.WAITING);
        given(storeOrder.getCreatedAt()).willReturn(LocalDateTime.now());

        given(storeOrderRepository.findById(orderId)).willReturn(Optional.of(storeOrder));
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(orderId))
                .willReturn(List.of(detail1, detail2));
        given(shipmentRepository.findByStoreOrder(storeOrder)).willReturn(List.of());
        given(storeOrderStatusLogRepository.findConfirmedTime(orderId)).willReturn(null);

        // when
        FranchiseOrderDetailResponseDto result = franchiseOrderService.getFranchiseOrderDetail(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getSku()).isEqualTo("PRD001");
        assertThat(result.getItems().get(1).getSku()).isEqualTo("PRD002");
    }
}
