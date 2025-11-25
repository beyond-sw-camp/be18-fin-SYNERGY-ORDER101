package com.synerge.order101.purchase.model.repository;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.dto.PurchaseSummaryResponseDto;
import com.synerge.order101.purchase.model.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseRepositoryCustom {
    Page<Purchase> findByDynamicSearch(
            String keyword,
            OrderStatus status,
            Pageable pageable
    );
}
