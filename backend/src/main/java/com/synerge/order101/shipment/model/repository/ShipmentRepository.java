package com.synerge.order101.shipment.model.repository;

import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.shipment.model.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByShipmentStatus(ShipmentStatus status);

    int countByStore_StoreIdAndShipmentStatus(Long storeId, ShipmentStatus shipmentStatus);
    List<Shipment> findByStoreOrder(StoreOrder storeOrder);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Shipment s
       set s.shipmentStatus = :nextStatus,
           s.updatedAt      = :now
     where s.shipmentStatus = :currentStatus
       and s.createdAt      <= :threshold
  """)
    int updateFromCreatedAt(@Param("currentStatus") ShipmentStatus currentStatus,
                            @Param("nextStatus")   ShipmentStatus nextStatus,
                            @Param("threshold")    LocalDateTime threshold,
                            @Param("now")          LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Shipment s
       set s.shipmentStatus = :nextStatus,
           s.updatedAt      = :now
     where s.shipmentStatus = :currentStatus
       and s.updatedAt      <= :threshold
       and s.inTransitApplied = true
       and s.inventoryApplied = false
  """)
    int updateFromUpdatedAt(@Param("currentStatus") ShipmentStatus currentStatus,
                            @Param("nextStatus")   ShipmentStatus nextStatus,
                            @Param("threshold")    LocalDateTime threshold,
                            @Param("now")          LocalDateTime now);


    @Query("""
      select s from Shipment s
       where s.shipmentStatus = com.synerge.order101.common.enums.ShipmentStatus.IN_TRANSIT
         and s.inTransitApplied = false
    """)
    List<Shipment> findInTransitNotApplied();

    @Query("""
      select s from Shipment s
       where s.shipmentStatus = com.synerge.order101.common.enums.ShipmentStatus.DELIVERED
         and s.inventoryApplied = false
    """)
    List<Shipment> findDeliveredNotApplied();


}
