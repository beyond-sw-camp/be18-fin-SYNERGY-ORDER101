package com.synerge.order101.ai.service;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.ai.model.dto.request.SmartOrderUpdateRequestDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDashboardResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDetailResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderLineItemResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.repository.DemandForecastRepository;
import com.synerge.order101.ai.repository.SmartOrderRepository;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.synerge.order101.user.model.entity.User;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartOrderService {
    private final SmartOrderRepository smartOrderRepository;
    private final DemandForecastRepository demandForecastRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final UserRepository userRepository;

    //AI가 스마트 발주 초안 작성
    @Transactional
    public List<SmartOrderResponseDto> generateSmartOrders(LocalDate targetWeek) {

        // 같은 주차에 이미 스마트발주 존재하는 경우
        List<SmartOrder> existing = smartOrderRepository.findByTargetWeek(targetWeek);
        if (!existing.isEmpty()) {
            throw new CustomException(AiErrorCode.SMART_ORDER_ALREADY_EXISTS);
        }
        User systemUser = getSystemUser();

        // 1) 해당 주차의 수요 예측 조회
        List<DemandForecast> forecasts = demandForecastRepository.findByTargetWeek(targetWeek);
        if (forecasts.isEmpty()) {
            throw new CustomException(AiErrorCode.FORECAST_NOT_FOUND);
        }

        // 2) 상품  공급사 매핑 후 스마트 발주 생성
        List<SmartOrder> saved = forecasts.stream()
                .map(df -> {
                    var product = df.getProduct();

                    var mapping = productSupplierRepository.findByProduct(product)
                            .orElseThrow(() ->
                                    new CustomException(AiErrorCode.SUPPLIER_MAPPING_NOT_FOUND)
                            );

                    SmartOrder so = SmartOrder.builder()
                            .supplier(mapping.getSupplier())
                            .product(product)
                            .demandForecast(df)
                            .targetWeek(df.getTargetWeek())
                            .forecastQty(df.getYPred())
                            .recommendedOrderQty(df.getYPred())
                            .smartOrderStatus(OrderStatus.DRAFT_AUTO)
                            .build();

                    so.setSystemUserIfNull(systemUser);
                    return so;
                })
                .map(smartOrderRepository::save)
                .toList();

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }


    // 스마트 발주 목록 조회
    public List<SmartOrderResponseDto> getSmartOrders(
            OrderStatus status, LocalDate from, LocalDate to
    ) {
        List<SmartOrder> list;

        boolean hasStatus = (status != null);
        boolean hasRange = (from != null && to != null);

        if (hasStatus && hasRange) {
            list = smartOrderRepository
                    .findBySmartOrderStatusAndTargetWeekBetween(status, from, to);
        } else if (hasStatus) {
            list = smartOrderRepository.findBySmartOrderStatus(status);
        } else if (hasRange) {
            list = smartOrderRepository.findByTargetWeekBetween(from, to);
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
                .items(list.stream()
                        .map(this::toLineItemDto)
                        .toList())
                .build();
    }

    private SmartOrderLineItemResponseDto toLineItemDto(SmartOrder so) {
        Integer forecast = so.getForecastQty();
        Integer recommended = so.getRecommendedOrderQty();

        boolean edited = (forecast != null && recommended != null && !forecast.equals(recommended));

        return SmartOrderLineItemResponseDto.builder()
                .smartOrderId(so.getSmartOrderId())
                .productId(so.getProduct().getProductId())
                .productCode(so.getProduct().getProductCode())
                .productName(so.getProduct().getProductName())
                .forecastQty(forecast)
                .recommendedOrderQty(recommended)
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

        return SmartOrderDashboardResponseDto.builder()
                .targetWeek(targetWeek)
                .totalRecommendedQty(totalRecommended)
                .totalForecastQty(totalForecast)
                .draftCount((int) draftCount)
                .submittedCount((int) submittedCount)
                .build();
    }



    private SmartOrderResponseDto toResponse(SmartOrder so) {
        return SmartOrderResponseDto.builder()
                .id(so.getSmartOrderId())
                .supplierId(so.getSupplier().getSupplierId())
                .productId(so.getProduct().getProductId())
                .demandForecastId(so.getDemandForecast().getDemandForecastId())
                .targetWeek(so.getTargetWeek())
                .recommendedOrderQty(so.getRecommendedOrderQty())
                .forecastQty(so.getForecastQty())
                .smartOrderStatus(so.getSmartOrderStatus())
                .snapshotAt(so.getSnapshotAt())
                .updatedAt(so.getUpdatedAt())
                .build();
    }

    // SYSTEM 유저 조회
    private User getSystemUser() {
        List<User> list = userRepository.findByRole(Role.SYSTEM);

        if (list.isEmpty()) {
            throw new IllegalStateException("SYSTEM 유저를 찾을 수 없습니다.");
        }

        return list.get(0);
    }


    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null ||
                !auth.isAuthenticated() ||
                auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("로그인 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }


        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            return userRepository.findByEmail(username)
                    .orElseThrow(() ->
                            new IllegalStateException("로그인 유저를 찾을 수 없습니다. username=" + username));
        }


        throw new IllegalStateException(
                "지원하지 않는 principal 타입: " + principal.getClass().getName());
    }




}