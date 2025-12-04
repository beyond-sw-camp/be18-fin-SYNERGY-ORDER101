package com.synerge.order101.order.model.service;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.repository.NotificationRepository;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.order.exception.errorcode.OrderErrorCode;
import com.synerge.order101.order.model.dto.*;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.entity.StoreOrderStatusLog;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.order.model.repository.StoreOrderStatusLogRepository;
import com.synerge.order101.outbound.model.service.OutboundService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.purchase.model.dto.PurchaseSummaryResponseDto;
import com.synerge.order101.settlement.event.StoreOrderSettlementReqEvent;
import com.synerge.order101.settlement.model.dto.SettlementSummaryDto;
import com.synerge.order101.settlement.model.entity.Settlement;
import com.synerge.order101.shipment.model.entity.Shipment;
import com.synerge.order101.shipment.model.repository.ShipmentRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * TODO : 페이지 조회 로직 성능 TEST 및 개선
 *  TODO : 페이지 동적 쿼리 변경
 *  #박진우
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOrderServiceImpl implements StoreOrderService {

    private final StoreOrderRepository storeOrderRepository;
    private final StoreOrderDetailRepository storeOrderDetailRepository;
    private final StoreOrderStatusLogRepository storeOrderStatusLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final ShipmentRepository shipmentRepository;

    private final NotificationService notificationService;
    private final OutboundService outboundService;

    /**
     * 주문 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StoreOrderSummaryResponseDto> findOrders(TradeSearchCondition cond, Pageable pageable){

        Page<StoreOrder> page = storeOrderRepository.search(cond, pageable);

        return page.map(StoreOrderSummaryResponseDto::fromEntity);
    }


    /**
     * 특정 ID의 주문 상세 정보를 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public StoreOrderDetailResponseDto findStoreOrderDetails(Long storeOrderId) {

        StoreOrder order = storeOrderRepository.findById(storeOrderId).orElseThrow(() ->
                new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        List<StoreOrderDetail> details = storeOrderDetailRepository.findByStoreOrder_StoreOrderId(storeOrderId);

        StoreOrderDetailResponseDto.OrderItemDto[] items = details.stream()
                .map(StoreOrderDetailResponseDto.OrderItemDto::fromEntity)
                .toArray(StoreOrderDetailResponseDto.OrderItemDto[]::new);

        return StoreOrderDetailResponseDto.builder()
                .storeOrderId(order.getStoreOrderId())
                .storeOrderNo(order.getOrderNo())
                .requesterName(order.getUser().getName())
                .storeName(order.getStore() == null ? null : order.getStore().getStoreName())
                .status(order.getOrderStatus() == null ? null : order.getOrderStatus().name())
                .orderDate(order.getCreatedAt())
                .orderItems(List.of(items))
                .build();

    }

    /**
     * 새로운 주문을 생성합니다.
     */
    @Transactional
    @Override
    public StoreOrderCreateResponseDto createOrder(StoreOrderCreateRequest request) {

        // store 조회
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.STORE_NOT_FOUND));

        // warehouse 선택 로직: 요청에 warehouseId가 있으면 사용, 없으면 store의 defaultWarehouse 사용
        // TODO: 추후 거리/재고 기반 창고 선택 로직 추가 예정
        Warehouse warehouse = selectWarehouse(request.getWarehouseId(), store);

        // user 조회
        User user = null;
        user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        StoreOrder order = StoreOrder
                .builder()
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .orderStatus(OrderStatus.SUBMITTED)
                .remark(request.getRemark())
                .build();
        StoreOrder savedOrder = storeOrderRepository.save(order);

        // create and persist details
        if (request.getItems() != null) {
            for (StoreOrderCreateRequest.Item item : request.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

                BigDecimal unitPrice = product.getPrice();
                BigDecimal amount = unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(item.getOrderQty()));

                StoreOrderDetail detail = new StoreOrderDetail(savedOrder, product, item.getOrderQty(), unitPrice, amount);
                // TODO : saveALl로 수정. 박진우
                storeOrderDetailRepository.save(detail);
            }
        }

        // 가맹점 발주 알림(테스트 X)
        List<User> hqList = userRepository.findByRole(Role.HQ);

        if (!hqList.isEmpty()) {
            notificationService.notifyOrderCreatedToHQ(hqList, savedOrder);
        }

        return StoreOrderCreateResponseDto.builder()
                .storeOrderId(savedOrder.getStoreOrderId())
                .orderNo(savedOrder.getOrderNo())
                .build();
    }


    /**
     * 주문 상태를 업데이트합니다.
     */
    @Override
    @Transactional
    public StoreOrderUpdateStatusResponseDto updateOrderStatus(Long storeOrderId, OrderStatus newStatus) {
        StoreOrder order = storeOrderRepository.findById(storeOrderId).orElseThrow(() ->
                new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        OrderStatus prev = order.getOrderStatus();

        order.updateOrderState(newStatus);

        OrderStatus curStatus = order.getOrderStatus();

        if (curStatus == OrderStatus.CONFIRMED) {
            // 출고 처리
            outboundService.createOutbound(order);

            // 주문 완료 이벤트 발행
            eventPublisher.publishEvent(new StoreOrderSettlementReqEvent(order));

            //배송 대기 시작
            Shipment shipment = Shipment.builder()
                    .storeOrder(order)
                    .store(order.getStore())
                    .shipmentStatus(ShipmentStatus.WAITING)
                    .inventoryApplied(false)
                    .inTransitApplied(false)
                    .build();
            shipmentRepository.save(shipment);

            log.info("Shipment(WAITING) created for storeOrderId={}", storeOrderId);
        }

        StoreOrderStatusLog log = StoreOrderStatusLog.builder()
                .storeOrder(order)
                .prevStatus(prev)
                .curStatus(curStatus)
                .build();

        storeOrderStatusLogRepository.save(log);

        if (curStatus == OrderStatus.CONFIRMED || curStatus == OrderStatus.REJECTED) {
            notificationService.notifyStoreOrderResult(order);
        }


        return StoreOrderUpdateStatusResponseDto.builder()
                .storeOrderId(order.getStoreOrderId())
                .status(order.getOrderStatus().name())
                .build();
    }

    /**
     * 창고 선택 로직
     * 1. 요청에 warehouseId가 있으면 해당 창고 사용
     * 2. 없으면 store의 defaultWarehouse 사용
     * 3. defaultWarehouse도 없으면 예외 발생
     * 
     * TODO: 추후 거리/재고 기반 창고 선택 로직 추가 예정
     */
    private Warehouse selectWarehouse(Long warehouseId, Store store) {
        // 1. 요청에 warehouseId가 명시된 경우
        if (warehouseId != null) {
            return warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new CustomException(OrderErrorCode.WAREHOUSE_NOT_FOUND));
        }
        
        // 2. store의 defaultWarehouse 사용
        Warehouse defaultWarehouse = store.getDefaultWarehouse();
        if (defaultWarehouse != null) {
            return defaultWarehouse;
        }
        
        // 3. 기본 창고도 없는 경우 예외 발생
        throw new CustomException(OrderErrorCode.WAREHOUSE_NOT_FOUND);
    }

    /**
     * 주문 승인 전 창고 재고 확인
     * - 주문에 연결된 창고의 재고를 확인하여 부족한 품목 목록을 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public StoreOrderStockCheckResponseDto checkStockForOrder(Long storeOrderId) {
        StoreOrder order = storeOrderRepository.findById(storeOrderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        List<StoreOrderDetail> details = storeOrderDetailRepository.findByStoreOrder_StoreOrderId(storeOrderId);

        List<StoreOrderStockCheckResponseDto.InsufficientStockItem> insufficientItems = new java.util.ArrayList<>();

        for (StoreOrderDetail detail : details) {
            Long productId = detail.getProduct().getProductId();
            int requestedQty = detail.getOrderQty().intValue();

            // 창고 재고 조회
            int availableQty = warehouseInventoryRepository.findByProduct_ProductId(productId)
                    .map(WarehouseInventory::getOnHandQuantity)
                    .orElse(0);

            if (availableQty < requestedQty) {
                insufficientItems.add(StoreOrderStockCheckResponseDto.InsufficientStockItem.builder()
                        .productId(productId)
                        .productName(detail.getProduct().getProductName())
                        .productCode(detail.getProduct().getProductCode())
                        .requestedQty(requestedQty)
                        .availableQty(availableQty)
                        .shortageQty(requestedQty - availableQty)
                        .build());
            }
        }

        return StoreOrderStockCheckResponseDto.builder()
                .hasEnoughStock(insufficientItems.isEmpty())
                .insufficientItems(insufficientItems)
                .build();
    }
}