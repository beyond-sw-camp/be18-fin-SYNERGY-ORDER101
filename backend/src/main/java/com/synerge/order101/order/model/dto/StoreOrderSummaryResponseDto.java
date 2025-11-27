package com.synerge.order101.order.model.dto;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@AllArgsConstructor
@Builder
public class StoreOrderSummaryResponseDto {

    private Long storeOrderId;

    private String orderNo;

    private String storeName;

    // 품목 수
    private Integer itemCount;

    // 금액
    private BigDecimal totalAmount;

    // 총 수량
    private Integer totalQTY;

    private LocalDateTime orderDate;

    private OrderStatus orderStatus;

    public static StoreOrderSummaryResponseDto fromEntity(StoreOrder storeOrder) {
        return StoreOrderSummaryResponseDto.builder()
                .storeOrderId(storeOrder.getStoreOrderId())
                .orderNo(storeOrder.getOrderNo())
                .storeName(storeOrder.getStore().getStoreName())
                .totalAmount(storeOrder.getStoreOrderDetails().stream()
                        .map(detail -> Optional.ofNullable(detail.getAmount()).orElse(BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .itemCount(storeOrder.getStoreOrderDetails().size())
                .totalQTY(storeOrder.getStoreOrderDetails().stream()
                        .mapToInt(detail -> Optional.ofNullable(detail.getOrderQty()).orElse(0))
                        .sum())
                .orderDate(storeOrder.getCreatedAt())
                .orderStatus(storeOrder.getOrderStatus())
                .build();

    }
}
