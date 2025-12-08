package com.synerge.order101.ai.model.repository;

import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.common.enums.OrderStatus;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SmartOrderRepository extends JpaRepository<SmartOrder,Long> {
    long countBySmartOrderStatus(OrderStatus status);
    List<SmartOrder> findBySmartOrderStatus(OrderStatus status);
    List<SmartOrder> findAllByOrderByTargetWeekDesc();
    List<SmartOrder> findByTargetWeek(LocalDate targetWeek);
    List<SmartOrder> findByTargetWeekBetween(LocalDate from, LocalDate to);
    List<SmartOrder> findBySmartOrderStatusAndTargetWeekBetween(OrderStatus status, LocalDate from, LocalDate to);
    List<SmartOrder> findBySupplier_SupplierIdAndTargetWeek(Long supplierId, LocalDate targetWeek);
    Optional<SmartOrder> findTopByDemandForecast_DemandForecastId(Long demandForecastId);
    //업데이트라 풀패키지명 작성
    @Modifying
    @Query("UPDATE SmartOrder so " +
            "SET so.smartOrderStatus = com.synerge.order101.common.enums.OrderStatus.CANCELLED " +
            "WHERE so.smartOrderStatus = com.synerge.order101.common.enums.OrderStatus.DRAFT_AUTO")
    int cancelAllAutoDrafts();


}

