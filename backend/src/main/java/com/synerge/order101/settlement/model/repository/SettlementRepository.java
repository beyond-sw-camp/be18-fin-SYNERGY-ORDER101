package com.synerge.order101.settlement.model.repository;

import com.synerge.order101.settlement.model.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> , SettlementRepositoryCustom{
    boolean existsByPurchase_PurchaseId(Long purchasePurchaseId);
    boolean existsByStoreOrder_StoreOrderId(Long storeOrderId);
    java.util.List<Settlement> findBySettlementStatus(Settlement.SettlementStatus settlementStatus);
    java.util.Optional<Settlement> findBySettlementNo(String settlementNo);

}
