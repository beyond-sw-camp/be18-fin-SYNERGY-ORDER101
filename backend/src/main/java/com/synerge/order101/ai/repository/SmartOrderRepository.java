package com.synerge.order101.ai.repository;

import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.common.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SmartOrderRepository extends JpaRepository<SmartOrder,Long> {
    List<SmartOrder> findBySmartOrderStatus(OrderStatus status);
    List<SmartOrder> findAllByOrderByTargetWeekDesc();
    List<SmartOrder> findByTargetWeek(LocalDate targetWeek);
    List<SmartOrder> findByTargetWeekBetween(LocalDate from, LocalDate to);
    List<SmartOrder> findBySmartOrderStatusAndTargetWeekBetween(OrderStatus status, LocalDate from, LocalDate to);
    List<SmartOrder> findBySupplier_SupplierIdAndTargetWeek(Long supplierId, LocalDate targetWeek);
}

