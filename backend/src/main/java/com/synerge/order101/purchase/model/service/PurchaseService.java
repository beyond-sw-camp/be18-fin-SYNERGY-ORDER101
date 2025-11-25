package com.synerge.order101.purchase.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PurchaseService {


    // 발주 목록 조회
    Page<PurchaseSummaryResponseDto> findPurchases(String keyword, Integer page, Integer size, OrderStatus status);

    // 발주 상세 조회
    PurchaseDetailResponseDto findPurchaseDetailsById(Long purchaseOrderId);

    // 발주 생성
    void createPurchase(PurchaseCreateRequest purchaseCreateRequest);

    // 발주 상태 업데이트
    PurchaseUpdateStatusResponseDto updatePurchaseStatus(Long purchaseOrderId, OrderStatus status);

    // 자동 발주 생성
    void createAutoPurchase();

    List<AutoPurchaseListResponseDto> getAutoPurchases(OrderStatus status, Integer page, Integer size);

    AutoPurchaseDetailResponseDto getAutoPurchaseDetail(Long purchaseOrderId);

    AutoPurchaseDetailResponseDto submitAutoPurchase(Long purchaseId, AutoPurchaseSubmitRequestDto request);
}

