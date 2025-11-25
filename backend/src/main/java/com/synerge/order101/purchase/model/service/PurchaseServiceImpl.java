package com.synerge.order101.purchase.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.purchase.exception.PurchaseErrorCode;
import com.synerge.order101.purchase.model.dto.*;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.purchase.model.entity.PurchaseDetailHistory;
import com.synerge.order101.purchase.model.repository.PurchaseDetailHistoryRepository;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import com.synerge.order101.purchase.model.repository.PurchaseRepository;
import com.synerge.order101.settlement.event.PurchaseSettlementReqEvent;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import com.synerge.order101.warehouse.model.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * TODO : 발주 목록 조회에 공급가 적용 필요.
 *  TODO : 발주 목록 조회 페이지 동적 쿼리 변경
 *  TODO : 발주 생성 시 공급가 가져오는 로직 필요.
 *  TODO : 발주 목록 조회 최적화 필요
 *  TODO : 주 변경 로그 필요 유무 스펙 확인 필요.
 *  # 박진우
 */
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseDetailRepository purchaseDetailRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PurchaseDetailHistoryRepository purchaseDetailHistoryRepository;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductSupplierRepository productSupplierRepository;

    private final InventoryService inventoryService;

    private final NotificationService notificationService;

    // 발주 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseSummaryResponseDto> findPurchases(String keyword, Integer page, Integer size, OrderStatus status) {

        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PurchaseSummaryResponseDto> pageResult;

        if (keyword.isBlank() && status == null) {
            return purchaseRepository.findAll(pageable).map(PurchaseSummaryResponseDto::fromEntity);
        }

        return purchaseRepository.findByDynamicSearch(keyword, status, pageable).map(PurchaseSummaryResponseDto::fromEntity);
    }

    // 발주 상세 조회
    @Override
    @Transactional(readOnly = true)
    public PurchaseDetailResponseDto findPurchaseDetailsById(Long purchaseId) {

        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow(
                () -> new CustomException(PurchaseErrorCode.PURCHASE_NOT_FOUND
                ));

        List<PurchaseDetail> details = purchaseDetailRepository.findByPurchase_PurchaseId(purchaseId);

        // DTO 변환
        PurchaseDetailResponseDto.PurchaseItemDto[] items = details.stream()
                .map(PurchaseDetailResponseDto.PurchaseItemDto::fromEntity)
                .toArray(PurchaseDetailResponseDto.PurchaseItemDto[]::new);

        return PurchaseDetailResponseDto.builder()
                .purchaseId(purchase.getPurchaseId())
                .poNo(purchase.getPoNo())
                .supplierName(purchase.getSupplier().getSupplierName())
                .requesterName(purchase.getUser().getName())
                .requestedAt(purchase.getCreatedAt().toString())
                .purchaseItems(List.of(items))
                .build();

    }

    // 발주 생성
    @Override
    @Transactional
    public void createPurchase(PurchaseCreateRequest request) {

        // 유저 GET
        User user = userRepository.findById(request.getUserId()).orElseThrow(
                () -> new CustomException(PurchaseErrorCode.PURCHASE_CREATION_FAILED)
        );

        // 창고 GET
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId()).orElseThrow(
                () -> new CustomException(PurchaseErrorCode.PURCHASE_CREATION_FAILED)
        );

        // 공급사 GET
        Supplier supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow(
                () -> new CustomException(PurchaseErrorCode.PURCHASE_CREATION_FAILED)
        );

        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(request.getOrderStatus())
                .orderType(request.getOrderType())
                .build();

        purchaseRepository.save(purchase);

        List<PurchaseDetail> detailsToSave = new ArrayList<>();
        for (PurchaseCreateRequest.Item item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow(
                    () -> new CustomException(PurchaseErrorCode.PURCHASE_CREATION_FAILED));

            ProductSupplier productSupplier = productSupplierRepository
                    .findByProductAndSupplier(product, supplier) // supplier 객체는 이미 상위에서 조회됨
                    .orElseThrow(() -> new CustomException(PurchaseErrorCode.PURCHASE_NOT_FOUND));

            PurchaseDetail detail = PurchaseDetail.builder()
                    .product(product)
                    .purchase(purchase)
                    .orderQty(item.getOrderQty())
                    .deadline(request.getDeadline())
                    .unitPrice(productSupplier.getPurchasePrice())
                    .build();

            detailsToSave.add(detail);
        }

        purchaseDetailRepository.saveAll(detailsToSave);

        if (request.getOrderType() == Purchase.OrderType.AUTO) {
            // HQ 일반 사원 조회 (Role 이름/필드명은 실제 코드에 맞게 수정)
            List<User> hqStaffList = userRepository.findByRole(Role.HQ);

            if (!hqStaffList.isEmpty()) {
                notificationService.notifyAutoPurchaseCreatedToHqStaff(purchase, hqStaffList);
            }
        }

        if(request.getOrderType() == Purchase.OrderType.MANUAL){
            // 일반 발주 관리자한테 알림 (테스트 X)
            List<User> admins = userRepository.findByRole(Role.HQ_ADMIN);

            notificationService.notifyPurchaseCreatedToAllAdmins(purchase, admins);
        }


    }

    // 발주 상태 변경
    @Override
    @Transactional
    public PurchaseUpdateStatusResponseDto updatePurchaseStatus(Long purchaseOrderId, OrderStatus newStatus) {
        Purchase purchase = purchaseRepository.findById(purchaseOrderId).orElseThrow(
                () -> new CustomException(PurchaseErrorCode.PURCHASE_NOT_FOUND)
        );

        OrderStatus orderStatus = purchase.getOrderStatus();

        purchase.updateOrderStatus(newStatus);

        OrderStatus curOrderStatus = purchase.getOrderStatus();

        if (curOrderStatus == OrderStatus.CONFIRMED) {
            // 발주 완료 이벤트 발행
            eventPublisher.publishEvent(new PurchaseSettlementReqEvent(purchase));
            // 입고 반영
            inventoryService.increaseInventory(purchase);
        }

        return PurchaseUpdateStatusResponseDto.builder()
                .purchaseId(purchase.getPurchaseId())
                .status(purchase.getOrderStatus().name())
                .build();
    }

    // 자동 발주 생성
    @Override
    @Transactional
    public void createAutoPurchase() {

        List<CalculatedAutoItem> autoItems = inventoryService.getAutoPurchaseItems();
        if (autoItems.isEmpty()) return;

        Map<Long, List<CalculatedAutoItem>> groupedBySupplier =
                autoItems.stream().collect(Collectors.groupingBy(CalculatedAutoItem::getSupplierId));

        Long systemUserId = 1L;
        Long warehouseId = 1L;

        for (Map.Entry<Long, List<CalculatedAutoItem>> entry : groupedBySupplier.entrySet()) {

            Long supplierId = entry.getKey();

            List<PurchaseCreateRequest.Item> items =
                    entry.getValue().stream()
                            .map(i -> new PurchaseCreateRequest.Item(i.getProductId(), i.getOrderQty()))
                            .toList();

            PurchaseCreateRequest request = new PurchaseCreateRequest(
                    supplierId,
                    systemUserId,
                    warehouseId,
                    Purchase.OrderType.AUTO,
                    OrderStatus.DRAFT_AUTO,      // 자동초안
                    LocalDate.now(),
                    items
            );

            createPurchase(request);
        }


    }
    // 자동발주 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<AutoPurchaseListResponseDto> getAutoPurchases (OrderStatus status, Integer page, Integer size){

        Pageable pageable = PageRequest.of(page, size);
        Page<AutoPurchaseListResponseDto> pageResult;

        if (status == null) {
            pageResult = purchaseRepository.findAutoOrderAllStatus(pageable);
        } else {
            pageResult = purchaseRepository.findByAutoOrderStatus(status, pageable);
        }
        System.out.println(pageResult.getContent());

        return pageResult.getContent();
    }

    // 자동발주 상세 조회
    @Override
    @Transactional(readOnly = true)
    public AutoPurchaseDetailResponseDto getAutoPurchaseDetail (Long purchaseId){

        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow(
                () -> new CustomException(PurchaseErrorCode.PURCHASE_NOT_FOUND));

        // PurchaseDetail + safetyQty 조회
        List<Object[]> results = purchaseDetailRepository.findDetailsWithSafetyQty(purchaseId);

        // DTO 변환
        List<AutoPurchaseDetailResponseDto.AutoPurchaseItemDto> items = results.stream()
                .map(r -> {
                    PurchaseDetail detail = (PurchaseDetail) r[0];
                    Integer safetyQty = (Integer) r[1];

                    return AutoPurchaseDetailResponseDto.AutoPurchaseItemDto.fromEntity(detail, safetyQty);
                })
                .toList();

        return AutoPurchaseDetailResponseDto.builder()
                .purchaseId(purchase.getPurchaseId())
                .poNo(purchase.getPoNo())
                .supplierName(purchase.getSupplier().getSupplierName())
                .requestedAt(purchase.getCreatedAt())
                .purchaseItems(items)
                .build();
    }

    @Override
    @Transactional
    public AutoPurchaseDetailResponseDto submitAutoPurchase (Long purchaseId, AutoPurchaseSubmitRequestDto request){


        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new CustomException(PurchaseErrorCode.PURCHASE_NOT_FOUND));

        List<PurchaseDetail> existingDetails = purchaseDetailRepository.findByPurchase_PurchaseId(purchaseId);

        Map<Long, Integer> requestedMap = request.items().stream()
                .collect(Collectors.toMap(AutoPurchaseSubmitRequestDto.Item::productId,
                        AutoPurchaseSubmitRequestDto.Item::orderQty));

        List<PurchaseDetailHistory> historyList = new ArrayList<>();

        // 1) UPDATE & DELETE
        for (PurchaseDetail detail : existingDetails) {
            Long productId = detail.getProduct().getProductId();
            Integer newQty = requestedMap.get(productId);

            if (newQty == null) {
                // 삭제
                historyList.add(
                        PurchaseDetailHistory.builder()
                                .purchaseId(purchaseId)
                                .purchaseOrderLineId(detail.getPurchaseOrderLineId())
                                .productId(productId)
                                .beforeQty(detail.getOrderQty())
                                .afterQty(0)
                                .changedBy(purchase.getUser().getUserId())
                                .build()
                );
                purchaseDetailRepository.delete(detail);
                continue;
            }

            Integer oldQty = detail.getOrderQty();
            if (!oldQty.equals(newQty)) {
                historyList.add(PurchaseDetailHistory.builder()
                        .purchaseId(purchaseId)
                        .purchaseOrderLineId(detail.getPurchaseOrderLineId())
                        .productId(productId)
                        .beforeQty(oldQty)
                        .afterQty(newQty)
                        .changedBy(purchase.getUser().getUserId())
                        .build()
                );
                detail.updateOrderQty(newQty);
            }

            requestedMap.remove(productId);
        }

        // 2) ADD
        for (Map.Entry<Long, Integer> entry : requestedMap.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(PurchaseErrorCode.PURCHASE_CREATION_FAILED));

            PurchaseDetail newDetail = PurchaseDetail.builder()
                    .purchase(purchase)
                    .product(product)
                    .orderQty(qty)
                    .unitPrice(product.getPrice())
                    .build();

            purchaseDetailRepository.save(newDetail);

            historyList.add(PurchaseDetailHistory.builder()
                    .purchaseId(purchaseId)
                    .purchaseOrderLineId(newDetail.getPurchaseOrderLineId())
                    .productId(productId)
                    .beforeQty(0)
                    .afterQty(qty)
                    .changedBy(purchase.getUser().getUserId())
                    .build()
            );
        }

        purchaseDetailHistoryRepository.saveAll(historyList);

        purchase.updateOrderStatus(OrderStatus.SUBMITTED);

        // 자동 발주 알림 (테스트 완)
        List<User> admins = userRepository.findByRole(Role.HQ_ADMIN);

        notificationService.notifyPurchaseCreatedToAllAdmins(purchase, admins);

        return getAutoPurchaseDetail(purchaseId);
    }
}
