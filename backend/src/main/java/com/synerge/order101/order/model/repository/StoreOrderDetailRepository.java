package com.synerge.order101.order.model.repository;

import com.synerge.order101.order.model.entity.StoreOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoreOrderDetailRepository extends JpaRepository<StoreOrderDetail,Long> {

    List<StoreOrderDetail> findByStoreOrder_StoreOrderId(Long storeOrderId);

    @Query("""
        SELECT SUM(d.orderQty)
        FROM StoreOrderDetail d
        WHERE d.product.productId = :productId
          AND d.storeOrder.orderStatus = 'CONFIRMED'
          AND d.storeOrder.orderDatetime >= :fromDate
        GROUP BY FUNCTION('date', d.storeOrder.orderDatetime)
        ORDER BY FUNCTION('date', d.storeOrder.orderDatetime)
    """)
    List<Integer> findDailySalesQtySince(
            @Param("productId") Long productId,
            @Param("fromDate") LocalDateTime fromDate
    );


}
