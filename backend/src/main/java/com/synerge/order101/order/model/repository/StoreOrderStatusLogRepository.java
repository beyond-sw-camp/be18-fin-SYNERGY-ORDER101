package com.synerge.order101.order.model.repository;

import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.entity.StoreOrderStatusLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StoreOrderStatusLogRepository extends JpaRepository<StoreOrderStatusLog,Long> {
    @Query("""
    SELECT l.updated_at
    FROM StoreOrderStatusLog l
    WHERE l.storeOrder.storeOrderId = :orderId
      AND l.curStatus = com.synerge.order101.common.enums.OrderStatus.CONFIRMED
    ORDER BY l.updated_at ASC
    """)
    LocalDateTime findConfirmedTime(@Param("orderId") Long orderId);

}
