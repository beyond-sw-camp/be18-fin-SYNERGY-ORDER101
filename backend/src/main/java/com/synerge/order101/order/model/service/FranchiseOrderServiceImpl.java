package com.synerge.order101.order.model.service;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.order.exception.errorcode.OrderErrorCode;
import com.synerge.order101.order.model.dto.FranchiseOrderDetailResponseDto;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.entity.StoreOrderStatusLog;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.order.model.repository.StoreOrderStatusLogRepository;
import com.synerge.order101.shipment.model.entity.Shipment;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FranchiseOrderServiceImpl implements  FranchiseOrderService {
    private final StoreOrderRepository storeOrderRepository;
    private final StoreOrderDetailRepository storeOrderDetailRepository;
    private final StoreOrderStatusLogRepository storeOrderStatusLogRepository;
    private final ShipmentRepository shipmentRepository;

    public FranchiseOrderDetailResponseDto getFranchiseOrderDetail(Long orderId){

        StoreOrder order = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        List<StoreOrderDetail> detailList =
                storeOrderDetailRepository.findByStoreOrder_StoreOrderId(orderId);

        Shipment shipment =
                shipmentRepository.findByStoreOrder(order)
                        .stream().findFirst()
                        .orElse(null);

        // 배송번호 UUID
        String trackingNo = UUID.randomUUID().toString();


        List<FranchiseOrderDetailResponseDto.ProgressDto> progressList =
                buildProgress(order, shipment, trackingNo);

        List<FranchiseOrderDetailResponseDto.ItemDto> items = detailList.stream()
                .map(d -> FranchiseOrderDetailResponseDto.ItemDto.builder()
                        .sku(d.getProduct().getProductCode())
                        .name(d.getProduct().getProductName())
                        .qty(d.getOrderQty())
                        .price(d.getUnitPrice())
                        .stock(0)
                        .build()
                )
                .toList();

        return FranchiseOrderDetailResponseDto.builder()
                .orderId(order.getStoreOrderId())
                .orderNo(order.getOrderNo())
                .storeName(order.getStore().getStoreName())
                .createdAt(order.getOrderDatetime())
                .orderStatus(order.getOrderStatus().name())
                .shipmentStatus(order.getShipmentStatus().name())
                .items(items)
                .progress(progressList)
                .build();
    }

    private List<FranchiseOrderDetailResponseDto.ProgressDto> buildProgress(
            StoreOrder order,
            Shipment shipment,
            String trackingNo
    ) {

        // SUBMITTED
        LocalDateTime submittedTime = order.getCreatedAt();

        // CONFIRMED
        LocalDateTime confirmedTime =
                storeOrderStatusLogRepository.findConfirmedTime(order.getStoreOrderId());


        // WAITING = shipment.createdAt
        LocalDateTime waitingTime = shipment != null ? shipment.getCreatedAt() : null;

        // SHIPPED = shipment.status = IN_TRANSIT 시 updatedAt
        LocalDateTime shippedTime =
                (shipment != null && shipment.getShipmentStatus() == ShipmentStatus.IN_TRANSIT)
                        ? shipment.getUpdatedAt()
                        : null;

        // DELIVERED = shipment.status = DELIVERED 시 updatedAt
        LocalDateTime deliveredTime =
                (shipment != null && shipment.getShipmentStatus() == ShipmentStatus.DELIVERED)
                        ? shipment.getUpdatedAt()
                        : null;

        return List.of(
                progress("submitted", "SUBMITTED", submittedTime),
                progress("confirmed", "CONFIRMED", confirmedTime),
                progress("waiting", "WAITING", waitingTime),
                progress("shipped", "SHIPPED", shippedTime, trackingNo),
                progress("delivered", "DELIVERED", deliveredTime)
        );
    }

    private FranchiseOrderDetailResponseDto.ProgressDto progress(
            String key, String label, LocalDateTime time
    ) {
        return FranchiseOrderDetailResponseDto.ProgressDto.builder()
                .key(key)
                .label(label)
                .time(time)
                .done(time != null)
                .build();
    }

    private FranchiseOrderDetailResponseDto.ProgressDto progress(
            String key, String label, LocalDateTime time, String note
    ) {
        return FranchiseOrderDetailResponseDto.ProgressDto.builder()
                .key(key)
                .label(label)
                .time(time)
                .done(time != null)
                .note(note)
                .build();
    }
}


