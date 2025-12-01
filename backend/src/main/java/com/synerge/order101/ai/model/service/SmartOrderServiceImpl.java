package com.synerge.order101.ai.model.service;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.ai.model.dto.request.SmartOrderUpdateRequestDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDashboardResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDetailResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderLineItemResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.repository.SmartOrderRepository;
import org.springframework.cache.annotation.Cacheable;
import com.synerge.order101.common.cache.CachedPerf;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import com.synerge.order101.supplier.exception.SupplierErrorCode;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartOrderServiceImpl implements SmartOrderService{
    private final SmartOrderRepository smartOrderRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final UserRepository userRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final PurchaseDetailRepository purchaseDetailRepository;
    private final NotificationService notificationService;
    private static final DateTimeFormatter PO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    @Transactional
    public int cancelPreviousAutoDrafts() {
        return smartOrderRepository.cancelAllAutoDrafts();
    }

    //AI가 스마트 발주 초안 작성
    @Transactional
    public List<SmartOrderResponseDto> generateSmartOrders(LocalDate targetWeek) {

        // 같은 주차에 이미 스마트발주 존재하는 경우
        List<SmartOrder> existing = smartOrderRepository.findByTargetWeek(targetWeek);
        if (!existing.isEmpty()) {
            throw new CustomException(AiErrorCode.SMART_ORDER_ALREADY_EXISTS);
        }
        User systemUser = getSystemUser();

        // 해당 주차의 수요 예측 가져오기
        List<DemandForecast> forecasts = demandForecastRepository.findByTargetWeek(targetWeek);
        if (forecasts.isEmpty()) {
            throw new CustomException(AiErrorCode.FORECAST_NOT_FOUND);
        }

        // 스마트 발주 로직
        List<SmartOrder> saved = forecasts.stream()
                .map(df -> {
                    var product = df.getProduct();

                    //  상품 -> 공급사
                    var mapping = productSupplierRepository.findByProduct(product)
                            .orElseThrow(() ->
                                    new CustomException(AiErrorCode.SUPPLIER_MAPPING_NOT_FOUND)
                            );

                    // 1주일 기준 예측 수요
                    int weeklyForecast = df.getYPred() != null ? df.getYPred() : 0;

                    // 리드타임 기반 추가 수요
                    Integer leadTimeDays = mapping.getLeadTimeDays();
                    int leadTimeDemand = 0;
                    if (leadTimeDays != null && leadTimeDays > 0) {
                        leadTimeDemand = (int) Math.round(weeklyForecast * (leadTimeDays / 7.0));
                    }

                    // 창고 재고 + 안전재고
                    var invOpt = warehouseInventoryRepository.findByProduct(product);
                    int onHand = 0;
                    int safety = 0;
                    if (invOpt.isPresent()) {
                        var inv = invOpt.get();
                        onHand = inv.getOnHandQuantity() != null ? inv.getOnHandQuantity() : 0;
                        safety = inv.getSafetyQuantity() != null ? inv.getSafetyQuantity() : 0;
                    }

                    // 본사 입고 예정 수량  - purchase_detail
                    long inTransit = purchaseDetailRepository
                            .sumOpenOrderQtyByProduct(product);

                    // 필요 재고 계산
                    int targetStock = weeklyForecast + leadTimeDemand + safety;

                    //  최종 발주량 = 목표재고 - 현재고 - 입고예정
                    long rawOrder = (long) targetStock - onHand - inTransit;
                    int recommendedOrderQty = (rawOrder > 0) ? (int) rawOrder : 0;

                    if (recommendedOrderQty <= 0) {
                        return null;
                    }


                    SmartOrder so = SmartOrder.builder()
                            .supplier(mapping.getSupplier())
                            .product(product)
                            .demandForecast(df)
                            .targetWeek(df.getTargetWeek())
                            .forecastQty(weeklyForecast)
                            .recommendedOrderQty(recommendedOrderQty)
                            .smartOrderStatus(OrderStatus.DRAFT_AUTO)
                            .poNumber(generatePoNumber(df.getTargetWeek(), mapping.getSupplier()))
                            .build();

                    so.setSystemUserIfNull(systemUser);

                    return so;
                })
                .filter(Objects::nonNull)
                .map(smartOrderRepository::save)
                .toList();

        List<User> hqList = userRepository.findByRole(Role.HQ);

        // smartorder -> supplier 공급사별 매핑
        Map<Long, List<SmartOrder>> bySupplier = new HashMap<>();

        for (SmartOrder smartOrder : saved) {
            Product product = smartOrder.getProduct();

            ProductSupplier ps = productSupplierRepository.findTop1ByProduct(product).orElseThrow(() ->
                    new CustomException(SupplierErrorCode.SUPPLIER_NOT_FOUND));

            Supplier supplier = ps.getSupplier();

            bySupplier.computeIfAbsent(supplier.getSupplierId(), k -> new ArrayList<>()).add(smartOrder);
        }

        // 공급사별로 알림 1개씩 전송
        for (Map.Entry<Long, List<SmartOrder>> entry : bySupplier.entrySet()) {
            List<SmartOrder> smartOrders = entry.getValue();
            Supplier supplier = productSupplierRepository.findTop1ByProduct(smartOrders.get(0).getProduct())
                    .get().getSupplier();

            notificationService.notifySmartOrderCreatedToHq(hqList, supplier, smartOrders);
        }

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    // 스마트 발주 목록 조회
//    @Cacheable(value = "smartOrders",
//            key = "{#status?.name(), #from, #to}")
//    @CachedPerf("SmartOrderList")
    public List<SmartOrderResponseDto> getSmartOrders(
            OrderStatus status, LocalDate from, LocalDate to
    ) {
        List<SmartOrder> list;

        boolean hasStatus = (status != null);
        boolean hasRange = (from != null && to != null);

        // 날짜 범위의 끝(to)을 포함하기 위해 to에 하루를 더합니다.
        LocalDate endDateInclusive = (hasRange) ? to.plusDays(1) : null;

        if (hasStatus && hasRange) {
            list = smartOrderRepository
                    .findBySmartOrderStatusAndTargetWeekBetween(status, from, endDateInclusive);
        } else if (hasStatus) {
            list = smartOrderRepository.findBySmartOrderStatus(status);
        } else if (hasRange) {
            list = smartOrderRepository.findByTargetWeekBetween(from, endDateInclusive);
        } else {
            list = smartOrderRepository.findAllByOrderByTargetWeekDesc();
        }

        return list.stream().map(this::toResponse).toList();
    }


    // 스마트 발주 상세 조회
    public SmartOrderResponseDto getSmartOrder(Long id) {
        SmartOrder entity = smartOrderRepository.findById(id)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));
        return toResponse(entity);
    }


    public SmartOrderDetailResponseDto getSmartOrderDetail(Long supplierId, LocalDate targetWeek) {
        List<SmartOrder> list =
                smartOrderRepository.findBySupplier_SupplierIdAndTargetWeek(supplierId, targetWeek);

        if (list.isEmpty()) {
            throw new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND);
        }

        SmartOrder any = list.get(0);

        String requesterName =
                (any.getUser() != null) ? any.getUser().getName() : "SYSTEM";

        return SmartOrderDetailResponseDto.builder()
                .supplierId(supplierId)
                .supplierName(any.getSupplier().getSupplierName())
                .targetWeek(targetWeek)
                .requesterName(requesterName)
                .status(any.getSmartOrderStatus())
                .items(list.stream()
                        .map(this::toLineItemDto)
                        .toList())
                .build();
    }

    private SmartOrderLineItemResponseDto toLineItemDto(SmartOrder so) {
        Integer forecast = so.getForecastQty();
        Integer recommended = so.getRecommendedOrderQty();

        boolean edited = (forecast != null && recommended != null && !forecast.equals(recommended));

        var product = so.getProduct();
        BigDecimal price = Optional.ofNullable(product.getPrice()).orElse(BigDecimal.ZERO);
        int qty = (recommended != null) ? recommended : 0;
        BigDecimal lineAmount = price.multiply(BigDecimal.valueOf(qty));

        return SmartOrderLineItemResponseDto.builder()
                .smartOrderId(so.getSmartOrderId())
                .productId(so.getProduct().getProductId())
                .productCode(so.getProduct().getProductCode())
                .productName(so.getProduct().getProductName())
                .forecastQty(forecast)
                .recommendedOrderQty(recommended)
                .unitPrice(price)
                .lineAmount(lineAmount)
                .manualEdited(edited)
                .build();
    }

    @Transactional
    public SmartOrderResponseDto submitSmartOrder(Long smartOrderId, SmartOrderUpdateRequestDto request) {
        SmartOrder entity = smartOrderRepository.findById(smartOrderId)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));

        if (request.getRecommendedOrderQty() != null) {
            entity.updateRecommendedQty(request.getRecommendedOrderQty());
        }


        User currentUser = getCurrentUser();
        entity.submit(currentUser);

        List<User> admins = userRepository.findByRole(Role.HQ_ADMIN);

        if (!admins.isEmpty()){
            notificationService.notifySmartOrderApprovalToAdmins(entity, admins);
        }

        return toResponse(entity);
    }

    @Override
    @Transactional
    public SmartOrderResponseDto confirmSmartOrder(Long smartOrderId) {
        SmartOrder entity = smartOrderRepository.findById(smartOrderId)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));

        entity.confirm();
        return toResponse(entity);
    }

    @Override
    @Transactional
    public SmartOrderResponseDto rejectSmartOrder(Long smartOrderId) {
        SmartOrder entity = smartOrderRepository.findById(smartOrderId)
                .orElseThrow(() -> new CustomException(AiErrorCode.SMART_ORDER_NOT_FOUND));

        entity.reject();
        return toResponse(entity);
    }


    // 대시보드 상단 요약 카드
    public SmartOrderDashboardResponseDto getSmartOrderSummary(LocalDate targetWeek) {
        List<SmartOrder> list = smartOrderRepository.findByTargetWeek(targetWeek);

        long totalRecommended = list.stream()
                .mapToLong(so -> (long) so.getRecommendedOrderQty())
                .sum();

        long totalForecast = list.stream()
                .mapToLong(so -> (long) so.getForecastQty())
                .sum();

        long draftCount = list.stream()
                .filter(so -> so.getSmartOrderStatus() == OrderStatus.DRAFT_AUTO)
                .count();

        long submittedCount = list.stream()
                .filter(so -> so.getSmartOrderStatus() == OrderStatus.SUBMITTED)
                .count();

        BigDecimal totalRecommendedAmount = list.stream()
                .map(so -> {
                    BigDecimal price = Optional.ofNullable(so.getProduct().getPrice())
                            .orElse(BigDecimal.ZERO);
                    int qty = Optional.ofNullable(so.getRecommendedOrderQty()).orElse(0);
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SmartOrderDashboardResponseDto.builder()
                .targetWeek(targetWeek)
                .totalRecommendedQty(totalRecommended)
                .totalForecastQty(totalForecast)
                .draftCount((int) draftCount)
                .submittedCount((int) submittedCount)
                .totalRecommendedAmount(totalRecommendedAmount)
                .build();
    }

    private String generatePoNumber(LocalDate targetWeek, Supplier supplier) {
        String datePart = targetWeek.format(PO_DATE_FORMAT);
        String supplierPart = String.format("%04d", supplier.getSupplierId());
        return "PO" + datePart + supplierPart;
    }



    private SmartOrderResponseDto toResponse(SmartOrder so) {
        // 단가
        BigDecimal price = Optional.ofNullable(so.getProduct().getPrice())
                .orElse(BigDecimal.ZERO);

        // 수량
        int qty = Optional.ofNullable(so.getRecommendedOrderQty())
                .orElse(0);

        // 라인 금액 = 단가 × 수량
        BigDecimal lineAmount = price.multiply(BigDecimal.valueOf(qty));


        return SmartOrderResponseDto.builder()
                .id(so.getSmartOrderId())
                .supplierId(so.getSupplier().getSupplierId())
                .supplierName(so.getSupplier().getSupplierName())
                .productId(so.getProduct().getProductId())
                .productCode(so.getProduct().getProductCode())
                .productName(so.getProduct().getProductName())
                .demandForecastId(so.getDemandForecast().getDemandForecastId())
                .targetWeek(so.getTargetWeek())
                .recommendedOrderQty(so.getRecommendedOrderQty())
                .forecastQty(so.getForecastQty())
                .smartOrderStatus(so.getSmartOrderStatus())
                .snapshotAt(so.getSnapshotAt())
                .updatedAt(so.getUpdatedAt())
                .unitPrice(price)
                .lineAmount(lineAmount)
                .poNumber(so.getPoNumber())
                .build();
    }

    // SYSTEM 유저 조회
    private User getSystemUser() {
        List<User> list = userRepository.findByRole(Role.SYSTEM);

        if (list.isEmpty()) {
            throw new CustomException(AiErrorCode.SYSTEM_USER_NOT_FOUND);
        }

        return list.get(0);
    }


    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null ||
                !auth.isAuthenticated() ||
                auth instanceof AnonymousAuthenticationToken) {
            throw new CustomException(AiErrorCode.AUTHENTICATION_REQUIRED);
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }


        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            return userRepository.findByEmail(username)
                    .orElseThrow(() ->
                            new CustomException(AiErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        }


        throw new CustomException(AiErrorCode.UNSUPPORTED_PRINCIPAL_TYPE);
    }

}
