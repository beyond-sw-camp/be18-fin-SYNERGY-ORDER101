package com.synerge.order101.purchase.model.repository;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.dto.AutoPurchaseListResponseDto;
import com.synerge.order101.purchase.model.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, QuerydslPredicateExecutor<Purchase> , PurchaseRepositoryCustom{
    long countByOrderStatus(OrderStatus status);
    /**
     * PO 번호 또는 Supplier의 이름 중 하나라도 키워드를 포함하는 Purchase 목록을 조회합니다.
     * 대소문자 구분 없이 (IgnoreCase), 페이징 처리 (Pageable)를 적용합니다.
     *
     * @param poKeyword           PO 번호 검색 키워드
     * @param supplierNameKeyword 공급업체 이름 검색 키워드
     * @param pageable            페이징 정보
     * @return Page<Purchase>
     */
    Page<Purchase> findByPoNoContainingIgnoreCaseOrSupplier_SupplierNameContainingIgnoreCaseOrUser_nameContainingIgnoreCase(
            String poKeyword,
            String supplierNameKeyword,
            String userNameKeyword,
            Pageable pageable);

    @Query("""
        SELECT new com.synerge.order101.purchase.model.dto.AutoPurchaseListResponseDto(
            p.purchaseId,
            p.poNo,
            s.supplierName,
            CAST(COUNT(pd) AS int),
            CAST(COALESCE(SUM(pd.orderQty * pd.unitPrice), 0) AS int),
            p.createdAt,
            p.orderStatus
        )
        FROM Purchase p
        JOIN p.supplier s
        JOIN PurchaseDetail pd ON pd.purchase.purchaseId = p.purchaseId
        WHERE p.orderType = com.synerge.order101.purchase.model.entity.Purchase.OrderType.AUTO
        GROUP BY p.purchaseId, p.poNo, s.supplierName, p.createdAt, p.orderStatus
    """)
    Page<AutoPurchaseListResponseDto> getAutoPurchases(Pageable pageable);

    @Query("""
        SELECT new com.synerge.order101.purchase.model.dto.AutoPurchaseListResponseDto(
            p.purchaseId,
            p.poNo,
            s.supplierName,
            CAST(COUNT(pd) AS int),
            CAST(COALESCE(SUM(pd.orderQty * pd.unitPrice), 0) AS int),
            p.createdAt,
            p.orderStatus
        )
        FROM Purchase p
        JOIN p.supplier s
        JOIN PurchaseDetail pd ON pd.purchase.purchaseId = p.purchaseId
        WHERE p.orderType = com.synerge.order101.purchase.model.entity.Purchase.OrderType.AUTO
          AND (:supplierId IS NULL OR s.supplierId = :supplierId)
          AND (:startDate IS NULL OR p.createdAt >= :startDate)
          AND (:endDate IS NULL OR p.createdAt <= :endDate)
        GROUP BY p.purchaseId, p.poNo, s.supplierName, p.createdAt, p.orderStatus
    """)
    Page<AutoPurchaseListResponseDto> searchAutoPurchases(
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDateTime start,
            @Param("endDate") LocalDateTime end,
            Pageable pageable
    );
}

