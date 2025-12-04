package com.synerge.order101.shipment.event;

import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.shipment.exception.ShipmentErrorCode;
import com.synerge.order101.shipment.model.entity.Shipment;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.entity.StoreInventory;
import com.synerge.order101.store.model.repository.StoreInventoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("ShipmentInventoryListenerTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShipmentInventoryListenerTest {

    @InjectMocks
    private ShipmentInventoryListener listener;

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private StoreOrderRepository storeOrderRepository;
    @Mock private StoreOrderDetailRepository storeOrderDetailRepository;
    @Mock private StoreInventoryRepository storeInventoryRepository;

    @Test
    @Order(1)
    //delivered_정상_적재__inTransit_이미_반영됨
    void AlreadyApplyDeliveredInTransit() {
        // given
        // shipment: DELIVERED / inventoryApplied=false / inTransitApplied=true
        Shipment shipment = mock(Shipment.class);

        given(shipment.getShipmentStatus()).willReturn(ShipmentStatus.DELIVERED);
        given(shipment.getInventoryApplied()).willReturn(false);
        given(shipment.getInTransitApplied()).willReturn(true);
        given(shipmentRepository.findById(10L)).willReturn(Optional.of(shipment));

        // 주문/가맹점
        Store store = mock(Store.class);
        StoreOrder order = mock(StoreOrder.class);
        given(order.getStoreOrderId()).willReturn(11L);
        given(order.getStore()).willReturn(store);
        given(storeOrderRepository.findById(11L)).willReturn(Optional.of(order));

        // 주문 상세 1줄 (qty=5, 특정 상품)
        Product product = mock(Product.class);
        StoreOrderDetail line = mock(StoreOrderDetail.class);
        given(line.getProduct()).willReturn(product);
        given(line.getOrderQty()).willReturn(5);
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(11L)).willReturn(List.of(line));

        // 기존 재고: 입고예정 5, 현재고 0
        StoreInventory inv = StoreInventory.create(store, product);
        inv.increaseInTransit(5);
        given(storeInventoryRepository.findByStoreAndProduct(store, product)).willReturn(Optional.of(inv));

        // when
        listener.applyInventory(new ShipmentDeliveredEvent(10L, 11L, 111L));

        // then (입고예정 －＞ 현재고 이동)
        assertThat(inv.getInTransitQty()).isZero();
        assertThat(inv.getOnHandQty()).isEqualTo(5);
        verify(storeInventoryRepository, times(1)).save(inv);
        verify(shipmentRepository, times(1)).save(shipment);
    }

    @Test
    @Order(2)
    //delivered 정상_적재 InTransit 미반영 바로 현재고만증가
    void DeliveredInTransitAddOn_hand_qty() {
        // given
        Shipment shipment = mock(Shipment.class);
        given(shipment.getShipmentStatus()).willReturn(ShipmentStatus.DELIVERED);
        given(shipment.getInventoryApplied()).willReturn(false);
        given(shipment.getInTransitApplied()).willReturn(false); // 중간단계는 미적용
        given(shipmentRepository.findById(20L)).willReturn(Optional.of(shipment));

        Store store = mock(Store.class);
        StoreOrder order = mock(StoreOrder.class);
        given(order.getStoreOrderId()).willReturn(21L);
        given(order.getStore()).willReturn(store);
        given(storeOrderRepository.findById(21L)).willReturn(Optional.of(order));

        Product product = mock(Product.class);
        StoreOrderDetail line = mock(StoreOrderDetail.class);
        given(line.getProduct()).willReturn(product);
        given(line.getOrderQty()).willReturn(3);
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(21L)).willReturn(List.of(line));

        // 기존 재고: 입고예정 0, 현재고 0
        StoreInventory inv = StoreInventory.create(store, product);
        given(storeInventoryRepository.findByStoreAndProduct(store, product)).willReturn(Optional.of(inv));

        // when
        listener.applyInventory(new ShipmentDeliveredEvent(20L, 21L, 111L));

        // then (입고예정은 그대로, 현재고만 +3)
        assertThat(inv.getInTransitQty()).isZero();
        assertThat(inv.getOnHandQty()).isEqualTo(3);
        verify(storeInventoryRepository).save(inv);
        verify(shipmentRepository).save(shipment);
    }

    @Test
    @Order(3)
    //delivered 이미 적재됨 - 멱등
    void DeliveredAlreadyApplyIdempotence () {
        // given
        Shipment shipment = mock(Shipment.class);
        given(shipment.getInventoryApplied()).willReturn(true);  // 이미 반영됨
        given(shipmentRepository.findById(30L)).willReturn(Optional.of(shipment));

        // when
        listener.applyInventory(new ShipmentDeliveredEvent(30L, 31L, 111L));

        // then (어떤 저장 동작도 없어야 함)
        verifyNoInteractions(storeOrderRepository, storeOrderDetailRepository, storeInventoryRepository);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @Order(4)
    //delivered 아님  - 예외
    void NotDeliveredException() {
        // given
        Shipment shipment = mock(Shipment.class);

        given(shipment.getInventoryApplied()).willReturn(false);
        given(shipment.getShipmentStatus()).willReturn(ShipmentStatus.IN_TRANSIT); // DELIVERED 아님
        given(shipmentRepository.findById(40L)).willReturn(Optional.of(shipment));

        // when & then
        assertThatThrownBy(() ->
                listener.applyInventory(new ShipmentDeliveredEvent(40L, 41L, 111L))
        )
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ShipmentErrorCode.SHIPMENT_NOT_DELIVERED.getMessage());

        verifyNoInteractions(storeOrderRepository, storeOrderDetailRepository, storeInventoryRepository);
        verify(shipmentRepository, never()).save(any());
    }
}
