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
          and o.orderStatus = com.synerge.order101.common.enums.OrderStatus.CANCELLED
          and o.updatedAt between :from and :to
    """)
    int countCanceledThisMonth(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );




//    String BASE_QUERY = """
//        select new com.synerge.order101.order.model.dto.StoreOrderSummaryResponseDto(
//                so.storeOrderId,
//                so.orderNo,
//                s.storeName,
//                size(so.storeOrderDetails),
//                cast(
//                (
//                    select coalesce(sum(detail.orderQty),0)
//                    from StoreOrderDetail detail
//                    where detail.storeOrder = so
//                ) as integer),
//                so.createdAt,
//                so.orderStatus)
//            from StoreOrder so
//            left join so.store s
//            """;
//
//    @Query(value = BASE_QUERY + " WHERE so.orderStatus = :status",
//            countQuery = "SELECT COUNT(so.storeOrderId) FROM StoreOrder so WHERE so.orderStatus = :status")
//    Page<StoreOrderSummaryResponseDto> findByOrderStatus(OrderStatus status, Pageable pageable);
//
//    @Query(value = BASE_QUERY,
//            countQuery = "SELECT COUNT(so.storeOrderId) FROM StoreOrder so")
//    Page<StoreOrderSummaryResponseDto> findOrderAllStatus(Pageable pageable);
}
