package com.synerge.order101.shipment.event;

import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.shipment.model.entity.Shipment;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.entity.StoreInventory;
import com.synerge.order101.store.model.repository.StoreInventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("ShipmentInTransitListenerTest")
@ExtendWith(MockitoExtension.class)
class ShipmentInTransitListenerTest {

    @InjectMocks ShipmentInTransitListener listener;
    @Mock ShipmentRepository shipmentRepository;
    @Mock StoreOrderRepository storeOrderRepository;
    @Mock StoreOrderDetailRepository storeOrderDetailRepository;
    @Mock StoreInventoryRepository storeInventoryRepository;

    @Test
    void ApplyInTransit() {
        // shipment
        Shipment shipment = mock(Shipment.class);
        given(shipment.getShipmentId()).willReturn(10L);
        given(shipment.getShipmentStatus()).willReturn(ShipmentStatus.IN_TRANSIT);
        given(shipment.getInTransitApplied()).willReturn(false);
        given(shipmentRepository.findById(10L)).willReturn(Optional.of(shipment));

        // order & store
        var store = mock(Store.class);
        var order = mock(com.synerge.order101.order.model.entity.StoreOrder.class);
        given(order.getStoreOrderId()).willReturn(11L);
        given(order.getStore()).willReturn(store);
        given(storeOrderRepository.findById(11L)).willReturn(Optional.of(order));

        // line & product
        var product = mock(Product.class);
        var line = mock(com.synerge.order101.order.model.entity.StoreOrderDetail.class);
        given(line.getProduct()).willReturn(product);
        given(line.getOrderQty()).willReturn(5);
        given(storeOrderDetailRepository.findByStoreOrder_StoreOrderId(11L)).willReturn(List.of(line));

        // inventory
        StoreInventory inv = StoreInventory.create(store, product);
        given(storeInventoryRepository.findByStoreAndProduct(store, product)).willReturn(Optional.of(inv));

        // when
        listener.applyInTransit(new ShipmentInTransitEvent(10L, 11L, 111L));

        // then
        verify(storeInventoryRepository).save(any(StoreInventory.class));
        verify(shipmentRepository).save(shipment);
    }
}
