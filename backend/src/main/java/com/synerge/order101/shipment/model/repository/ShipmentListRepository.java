package com.synerge.order101.shipment.model.repository;

import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.shipment.model.dto.response.ShipmentResponseDto;
import com.synerge.order101.shipment.model.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ShipmentListRepository extends JpaRepository<Shipment, Long> {

    @Query(value = """
        select new com.synerge.order101.shipment.model.dto.response.ShipmentResponseDto(
            so.storeOrderId,
            so.orderNo,
            st.storeName,        
            wh.warehouseName,
            cast(
                coalesce(
                    (
                        select sum(d.orderQty)
                        from StoreOrderDetail d
                        where d.storeOrder.storeOrderId = so.storeOrderId
                    ),
                    0
                ) as bigdecimal
            ),
            sh.shipmentStatus,
            coalesce(so.orderDatetime, so.createdAt)
        )
        from Shipment sh
            join sh.store st
            join sh.storeOrder so
            join so.warehouse wh
        where (:orderNo is null or so.orderNo like concat('%', :orderNo, '%'))
          and (:storeId is null or st.storeId = :storeId)
          and (:status is null or sh.shipmentStatus = :status)
          and (:fromDt is null or coalesce(so.orderDatetime, so.createdAt) >= :fromDt)
          and (:toDt is null or coalesce(so.orderDatetime, so.createdAt) <= :toDt)
        order by coalesce(so.orderDatetime, so.createdAt) desc
        """,
            countQuery = """
        select count(sh)
        from Shipment sh
            join sh.store st
            join sh.storeOrder so
        where (:orderNo is null or so.orderNo like concat('%', :orderNo, '%'))
          and (:storeId is null or st.storeId = :storeId)
          and (:status is null or sh.shipmentStatus = :status)
          and (:fromDt is null or coalesce(so.orderDatetime, so.createdAt) >= :fromDt)
          and (:toDt is null or coalesce(so.orderDatetime, so.createdAt) <= :toDt)
        """
    )
    Page<ShipmentResponseDto> findPage(
            @Param("orderNo") String orderNo,
            @Param("storeId") Long storeId,
            @Param("status") ShipmentStatus status,
            @Param("fromDt") LocalDateTime from,
            @Param("toDt") LocalDateTime to,
            Pageable pageable
    );

}
