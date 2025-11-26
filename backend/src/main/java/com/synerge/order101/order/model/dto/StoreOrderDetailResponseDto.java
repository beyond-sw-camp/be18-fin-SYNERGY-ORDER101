package com.synerge.order101.order.model.dto;

import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.product.model.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class StoreOrderDetailResponseDto {

    // 주문 id
    private Long storeOrderId;

    // 주문 번호
    private String storeOrderNo;

    // 요청자
    private String requesterName;

    // 상점이름
    private String storeName;

    // 생성 시간
    private LocalDateTime orderDate;

    // 상태
    private String status;

    // 주문 아이템 목록
    private List<OrderItemDto> orderItems;

    public static StoreOrderDetailResponseDto fromEntity(StoreOrder storeOrder) {

        // storeOrder 객체 안의 storeOrderDetails 리스트를 꺼내서 orderItemDto 리스트로 변환
        List<OrderItemDto> itemDtoList = storeOrder.getStoreOrderDetails()
                .stream()
                .map(OrderItemDto::fromEntity)// 각 detail 항목을 OrderItemDto로 변환
                .collect(Collectors.toList());

        return StoreOrderDetailResponseDto.builder()
                .requesterName(storeOrder.getUser().getName())
                .storeOrderNo(storeOrder.getOrderNo())
                .storeOrderId(storeOrder.getStoreOrderId())
                .storeName(storeOrder.getStore()==null?null:storeOrder.getStore().getStoreName())
                .status(storeOrder.getOrderStatus()==null?null:storeOrder.getOrderStatus().name())
                .orderDate(storeOrder.getOrderDatetime())
                .orderItems(itemDtoList)
                .build();
    }

    @Getter
    @Builder
    public static class OrderItemDto {

        private Long productId;
        private String productName;
        private Integer orderQty;
        private BigDecimal unitPrice;
        private BigDecimal amount;

        public static OrderItemDto fromEntity(StoreOrderDetail detail) {
            return OrderItemDto.builder()
                    .productId(detail.getProduct()==null?null:detail.getProduct().getProductId())
                    .productName(detail.getProduct()==null?null:detail.getProduct().getProductName())
                    .orderQty(detail.getOrderQty().intValue())
                    .unitPrice(detail.getUnitPrice())
                    .amount(detail.getAmount())
                    .build();
        }

    }


}
