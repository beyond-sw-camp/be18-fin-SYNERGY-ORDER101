package com.synerge.order101.purchase.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.dto.*;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * 1. 발주 목록 조회
     * - 모든 발주 목록을 조회한다.
     */
    @GetMapping
    public ResponseEntity<Page<PurchaseSummaryResponseDto>> findPurchases(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) OrderStatus status
    ) {
        Page<PurchaseSummaryResponseDto> response = purchaseService.findPurchases(keyword, page, size, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 발주 상세 조회
     * - 특정 발주의 상세 정보를 조회한다.
     */
    @GetMapping("/{purchaseOrderId}")
    public ResponseEntity<PurchaseDetailResponseDto> findPurchaseDetailById(@PathVariable Long purchaseOrderId) {

        PurchaseDetailResponseDto response = purchaseService.findPurchaseDetailsById(purchaseOrderId);

        return ResponseEntity.ok(response);
    }


    /**
     * 3. 발주 생성
     * - 새로운 발주를 생성한다.
     */
    @PostMapping
    public ResponseEntity<Purchase> createPurchase(@RequestBody PurchaseCreateRequest purchaseCreateRequest) {

        purchaseService.createPurchase(purchaseCreateRequest);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * 4. 공급사 발주 처리
     * - 특정 발주에 대해 공급사의 승인을 처리한다.
     */
    @PatchMapping("/{purchaseOrderId}/{status}")
    public ResponseEntity<PurchaseUpdateStatusResponseDto> updatePurchase(
            @PathVariable Long purchaseOrderId,
            @PathVariable OrderStatus status) {

        PurchaseUpdateStatusResponseDto responseDto = purchaseService.updatePurchaseStatus(purchaseOrderId, status);

        return ResponseEntity.ok(responseDto);
    }

    // 자동 발주 목록 조회
    @GetMapping("/auto")
    public ResponseEntity<ItemsResponseDto<AutoPurchaseListResponseDto>> getAutoPurchases(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer numOfRows
    ) {

        List<AutoPurchaseListResponseDto> response = purchaseService.getAutoPurchases(status, page, numOfRows);
        int totalCount = response.size();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, response, page, totalCount));
    }

    // 자동 발주 상세 조회
    @GetMapping("/auto/{purchaseId}")
    public ResponseEntity<BaseResponseDto<AutoPurchaseDetailResponseDto>> getAutoPurchaseDetail(@PathVariable Long purchaseId) {

        AutoPurchaseDetailResponseDto response = purchaseService.getAutoPurchaseDetail(purchaseId);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, response));
    }

    // 자동 발주 수정 & 제출
    @PatchMapping("/auto/{purchaseId}/submit")
    public ResponseEntity<BaseResponseDto<AutoPurchaseDetailResponseDto>> updateAutoPurchaseDetail(@PathVariable Long purchaseId,
                                                                                                   @RequestBody AutoPurchaseSubmitRequestDto request) {

        AutoPurchaseDetailResponseDto response = purchaseService.submitAutoPurchase(purchaseId, request);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, response));
    }

}

