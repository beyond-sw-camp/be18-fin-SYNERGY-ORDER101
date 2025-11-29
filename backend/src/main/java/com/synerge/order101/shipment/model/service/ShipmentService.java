package com.synerge.order101.shipment.model.service;


import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.shipment.event.ShipmentDeliveredEvent;
import com.synerge.order101.shipment.event.ShipmentInTransitEvent;
import com.synerge.order101.shipment.model.entity.Shipment;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StoreOrderRepository storeOrderRepository;



    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void updateShipmentStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 일단 테스트 용으로 3분 설정.
        int w2t = shipmentRepository.updateFromCreatedAt(
                ShipmentStatus.WAITING, ShipmentStatus.IN_TRANSIT,
                now.minusMinutes(3), now
        );

        if (w2t > 0) {
            entityManager.clear();

            List<Shipment> list = shipmentRepository.findByShipmentStatus(ShipmentStatus.IN_TRANSIT);
            for (Shipment sh : list) {
                StoreOrder order = sh.getStoreOrder();
                order.updateShipmentStatus(ShipmentStatus.IN_TRANSIT);
                storeOrderRepository.save(order);
            }
        }


        // 일단 테스트 용으로 30분 설정.
        int t2d = shipmentRepository.updateFromUpdatedAt(
                ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED,
                now.minusMinutes(30), now
        );

        if (t2d > 0) {
            entityManager.clear();

            List<Shipment> list = shipmentRepository.findByShipmentStatus(ShipmentStatus.DELIVERED);
            for (Shipment sh : list) {
                StoreOrder order = sh.getStoreOrder();
                order.updateShipmentStatus(ShipmentStatus.DELIVERED);
                storeOrderRepository.save(order);
            }
        }



        // IN_TRANSIT 입고예정 반영 이벤트
        for (Shipment s : shipmentRepository.findInTransitNotApplied()) {
            eventPublisher.publishEvent(new ShipmentInTransitEvent(
                    s.getShipmentId(), s.getStoreOrder().getStoreOrderId(), s.getStore().getStoreId()
            ));
        }

        // DELIVERED 재고 반영 이벤트
        for (Shipment s : shipmentRepository.findDeliveredNotApplied()) {
            eventPublisher.publishEvent(new ShipmentDeliveredEvent(
                    s.getShipmentId(), s.getStoreOrder().getStoreOrderId(), s.getStore().getStoreId()
            ));
        }
    }
}