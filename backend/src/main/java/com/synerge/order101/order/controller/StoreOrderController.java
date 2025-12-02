package com.synerge.order101.order.controller;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.order.model.dto.*;
import com.synerge.order101.order.model.service.FranchiseOrderService;
import com.synerge.order101.order.model.service.StoreOrderService;
import com.synerge.order101.purchase.model.dto.PurchaseSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController // ResponseBody를 선언할 필요가 없어 코드가 간결해진다.
@RequestMapping("/api/v1/store-orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService storeOrderService;
    private final FranchiseOrderService franchiseOrderService;

    @GetMapping("/detail/{orderId}")
    public ResponseEntity<FranchiseOrderDetailResponseDto> getFranchiseOrderDetail(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(franchiseOrderService.getFranchiseOrderDetail(orderId));

    }


    /**
     * 1. 가맹점 주문 목록 조회 (GET /orders)
     * - 요청 파라미터(RequestParam)로 상태(status)와 페이지(page)를 받아 해당 조건에 맞는 주문 목록을 반환합니다.
     */
    @GetMapping
    public ResponseEntity<Page<StoreOrderSummaryResponseDto>> findStoreOrders(
            @ModelAttribute TradeSearchCondition cond,
            Pageable pageable
            ) {

        Page<StoreOrderSummaryResponseDto> response = storeOrderService.findOrders(cond, pageable);

        return ResponseEntity.ok(response);
    }


    /**
     * 2. 주문 상세 조회 (GET /orders/{orderId})
     * - 경로 변수(PathVariable)로 받은 주문 ID에 해당하는 상세 정보를 반환합니다.
     */
    @GetMapping("/{storeOrderId}")
    public ResponseEntity<StoreOrderDetailResponseDto> findStoreOrderById(@PathVariable Long storeOrderId) {

        StoreOrderDetailResponseDto response = storeOrderService.findStoreOrderDetails(storeOrderId);

        return ResponseEntity.ok(response);
    }

    /**
     * 3. 가맹점 주문 생성 (POST /orders)
     * - 요청 본문(RequestBody)에서 주문 정보를 받아 새로운 주문을 생성합니다.
     */
    @PostMapping
    public ResponseEntity<StoreOrderCreateResponseDto> createStoreOrder(@RequestBody StoreOrderCreateRequest request) {

        StoreOrderCreateResponseDto responseDto = storeOrderService.createOrder(request);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * 4. 주문 승인 (PUT /orders/{storeOrderId}/{status})
     * 5. 주문 반려 (PUT /orders/{storeOrderId}/{status})
     * - 특정 주문의 상태를 'status'으로 변경합니다.
     */
    @PatchMapping("/{storeOrderId}/{status}")
    public ResponseEntity<StoreOrderUpdateStatusResponseDto> approveStoreOrder(
            @PathVariable Long storeOrderId,
            @PathVariable OrderStatus status) {

        StoreOrderUpdateStatusResponseDto responseDto = storeOrderService.updateOrderStatus(storeOrderId,status);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 6. 주문 승인 전 창고 재고 확인 (GET /orders/{storeOrderId}/stock-check)
     * - 주문에 포함된 품목들의 창고 재고를 확인하여 부족한 품목 목록을 반환합니다.
     */
    @GetMapping("/{storeOrderId}/stock-check")
    public ResponseEntity<StoreOrderStockCheckResponseDto> checkStockForOrder(
            @PathVariable Long storeOrderId) {

        StoreOrderStockCheckResponseDto responseDto = storeOrderService.checkStockForOrder(storeOrderId);

        return ResponseEntity.ok(responseDto);
    }
}
