package com.synerge.order101.order.model.service;

import com.synerge.order101.common.enums.OrderStatus;
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
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.settlement.event.StoreOrderSettlementReqEvent;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** TODO : 페이지 조회 로직 성능 TEST 및 개선
 *  TODO : 페이지 동적 쿼리 변경
 *  #박진우
*/

@Service
@RequiredArgsConstructor
public class StoreOrderServiceImpl implements StoreOrderService {

    private final StoreOrderRepository storeOrderRepository;
    private final StoreOrderDetailRepository storeOrderDetailRepository;
    private final StoreOrderStatusLogRepository storeOrderStatusLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    private final NotificationService notificationService;

    /**
     * 주문 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)

    public List<StoreOrderSummaryResponseDto> findOrders(OrderStatus status, Integer page, Integer size) {

        int pageNum = (page == null || page < 0) ? 0 : page;
        int pageSize = (size != null && size > 0) ? size : 10; // 기본 페이지 크기 설정
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "orderDatetime"));
        Page<StoreOrderSummaryResponseDto> pageResult;

        if (status == null) {
            pageResult = storeOrderRepository.findOrderAllStatus(pageable);
        } else {
            pageResult = storeOrderRepository.findByOrderStatus(status, pageable);
        }


        return pageResult.getContent();
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
                .storeName(order.getStore() == null ? null : order.getStore().getStoreName())
                .status(order.getOrderStatus() == null ? null : order.getOrderStatus().name())
                .orderDate(order.getOrderDatetime())
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
        Store store = null;
        store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.STORE_NOT_FOUND));

        // warehouse 조회
        Warehouse warehouse = null;
        warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.WAREHOUSE_NOT_FOUND));

        // user 조회
        User user = null;
        user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        StoreOrder order = new StoreOrder(store, warehouse, user, request.getRemark());
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

        if(!hqList.isEmpty()){
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

        if(curStatus == OrderStatus.CONFIRMED) {
            // 주문 완료 이벤트 발행
            eventPublisher.publishEvent(new StoreOrderSettlementReqEvent(order));
        }

        StoreOrderStatusLog log = StoreOrderStatusLog.builder()
                .storeOrder(order)
                .prevStatus(prev)
                .curStatus(curStatus)
                .build();

        storeOrderStatusLogRepository.save(log);

        if(curStatus ==  OrderStatus.CONFIRMED || curStatus == OrderStatus.REJECTED) {
            notificationService.notifyStoreOrderResult(order);
        }


        return StoreOrderUpdateStatusResponseDto.builder()
                .storeOrderId(order.getStoreOrderId())
                .status(order.getOrderStatus().name())
                .build();
    }
}