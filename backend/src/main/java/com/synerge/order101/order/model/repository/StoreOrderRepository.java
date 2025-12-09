package com.synerge.order101.order.model.repository;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.order.model.dto.StoreOrderSummaryResponseDto;
import com.synerge.order101.order.model.entity.StoreOrder;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long>,StoreOrderRepositoryCustom {


    // 1. 승인 대기 발주
    int countByStore_StoreIdAndOrderStatus(Long storeId, OrderStatus orderStatus);

    // 4. 이번 달 반려 발주
    @Query("""
        select count(o)
        from StoreOrder o
        where o.store.storeId = :storeId
          and o.orderStatus = com.synerge.order101.common.enums.OrderStatus.REJECTED
          and o.updatedAt between :from and :to
    """)
    int countCanceledThisMonth(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
