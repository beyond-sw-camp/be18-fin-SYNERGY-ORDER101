package com.synerge.order101.purchase.model.repository;

import com.synerge.order101.purchase.model.entity.PurchaseDetailHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.nio.channels.FileChannel;
import java.util.Optional;

@Repository
public interface PurchaseDetailHistoryRepository extends JpaRepository<PurchaseDetailHistory, Long> {
    Optional<PurchaseDetailHistory> findTopByPurchaseOrderLineIdOrderByUpdatedAtAsc(Long purchaseOrderLineId);
}
