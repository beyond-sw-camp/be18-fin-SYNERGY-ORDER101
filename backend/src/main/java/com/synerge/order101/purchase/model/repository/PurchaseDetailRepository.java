package com.synerge.order101.purchase.model.repository;

import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseDetailRepository extends JpaRepository<PurchaseDetail, Long> {
    List<PurchaseDetail> findByPurchase_PurchaseId(Long purchaseId);

    @Query("""
        SELECT pd, wi.safetyQuantity
        FROM PurchaseDetail pd
        LEFT JOIN WarehouseInventory wi ON wi.product.productId = pd.product.productId
        WHERE pd.purchase.purchaseId = :purchaseId
        ORDER BY pd.purchaseOrderLineId ASC
    """)
    List<Object[]> findDetailsWithSafetyQty(Long purchaseId);

    @Query("""
        select coalesce(sum(pd.orderQty), 0)
        from PurchaseDetail pd
        join pd.purchase p
        where pd.product = :product
          and p.orderStatus in ('SUBMITTED')
    """)
    Long sumOpenOrderQtyByProduct(@Param("product") Product product);
}
