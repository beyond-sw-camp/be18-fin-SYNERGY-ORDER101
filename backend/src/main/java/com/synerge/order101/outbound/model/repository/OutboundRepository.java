package com.synerge.order101.outbound.model.repository;

import com.synerge.order101.outbound.model.entity.Outbound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OutboundRepository extends JpaRepository<Outbound,Long> {
    @Query("""
        select o as outbound,
               (select count(distinct d.product.productId)
                from OutboundDetail d
                where d.outbound = o) as itemCount,
               (select coalesce(sum(d.outboundQty), 0)
                from OutboundDetail d
                where d.outbound = o) as totalQty
        from Outbound o
        join o.store s
        order by o.outboundDatetime desc, o.outboundId desc
        """
    )
    Page<Object[]> findOutboundWithCounts(Pageable pageable);

    @Query("""
        SELECT o,
               COUNT(d),
               COALESCE(SUM(d.outboundQty), 0)
        FROM Outbound o
        LEFT JOIN OutboundDetail d ON o.outboundId = d.outbound.outboundId
        WHERE (:storeId IS NULL OR o.store.storeId = :storeId)
          AND (:start IS NULL OR :end IS NULL OR o.outboundDatetime BETWEEN :start AND :end)
        GROUP BY o.outboundId
    """)
    Page<Object[]> searchOutbounds(Long storeId,
                                   LocalDateTime start,
                                   LocalDateTime end,
                                   Pageable pageable);

}
