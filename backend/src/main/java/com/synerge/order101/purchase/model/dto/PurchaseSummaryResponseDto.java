package com.synerge.order101.purchase.model.dto;


import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.user.model.entity.User;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSummaryResponseDto {

    private Long purchaseId;

    private String supplierName;

    private String requesterName;

    private String poNo;

    private Integer totalQty;

    private BigDecimal totalAmount;

    private OrderStatus status;

    private LocalDateTime requestedAt;

    public static PurchaseSummaryResponseDto fromEntity(Purchase purchase) {
        // 1. 초기 총액을 BigDecimal(0)으로 설정
        BigDecimal totalAmount = purchase.getPurchaseDetails().stream()
                .map(pd -> {
                    // 2. 각 상세 항목별 금액 계산: (수량 * 단가)
                    // 수량(Integer)을 BigDecimal로 변환하여 단가(BigDecimal)와 곱합니다.
                    BigDecimal itemQty = new BigDecimal(pd.getOrderQty());
                    BigDecimal itemPrice = pd.getUnitPrice(); // getUnitPrice가 BigDecimal이라고 가정

                    // 항목별 금액 = 수량 * 단가 (BigDecimal 연산)
                    return itemQty.multiply(itemPrice);
                })
                // 3. 모든 항목별 금액을 누적 합산 (reduce)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User test22 = purchase.getUser();
        String test = purchase.getUser().getName();

        return  PurchaseSummaryResponseDto.builder()
                .purchaseId(purchase.getPurchaseId())
                .supplierName(purchase.getSupplier().getSupplierName())
                .requesterName(purchase.getUser().getName())
                .poNo(purchase.getPoNo())
                .totalQty(purchase.getPurchaseDetails().stream().mapToInt(PurchaseDetail::getOrderQty).sum())
                .totalAmount(totalAmount)
                .status(purchase.getOrderStatus())
                .requestedAt(purchase.getCreatedAt())
                .build();
    }
}
