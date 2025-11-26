package com.synerge.order101.warehouse.model.repository;


import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.warehouse.model.dto.response.InventoryResponseDto;
import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory,Long> {

    // 전체 리스트 조회
    @Query("""
        select wi
        from WarehouseInventory wi
        join fetch wi.product p
    """)
    List<WarehouseInventory> findAllWithProduct();

    // 페이징 조회
    @Query(value = """
        select wi
        from WarehouseInventory wi
        join fetch wi.product p
        """,
                countQuery = """
        select count(wi)
        from WarehouseInventory wi
    """)
    Page<WarehouseInventory> findAllWithProduct(Pageable pageable);

    Optional<WarehouseInventory> findByProduct_ProductId(Long productId);

    @Query("""
        SELECT wi
        FROM WarehouseInventory wi
        JOIN FETCH wi.product p
        JOIN FETCH p.productSupplier ps
        JOIN FETCH ps.supplier s
    """)
    List<WarehouseInventory> findAllWithProductAndSupplier();

    @Query("select coalesce(sum(w.onHandQuantity), 0) from WarehouseInventory w " +
            "where w.product.productId = :pid")
    long sumOnHandAll(@Param("pid") Long productId);

    @Query("select coalesce(sum(w.safetyQuantity), 0) from WarehouseInventory w " +
            "where w.product.productId = :pid")
    long sumSafetyAll(@Param("pid") Long productId);


    Optional<WarehouseInventory> findByProduct(Product product);

    @Query("""
    SELECT new com.synerge.order101.warehouse.model.dto.response.InventoryResponseDto(
        i.inventoryId, p.productCode, pc.categoryName, p.productName,
        i.onHandQuantity, i.safetyQuantity
    )
    FROM WarehouseInventory i
    JOIN i.product p
    JOIN p.productCategory pc
    WHERE (:largeCategoryId IS NULL OR pc.categoryLevel = 'LARGE' AND pc.productCategoryId = :largeCategoryId)
      AND (:mediumCategoryId IS NULL OR pc.categoryLevel = 'MEDIUM' AND pc.productCategoryId = :mediumCategoryId)
      AND (:smallCategoryId IS NULL OR pc.categoryLevel = 'SMALL' AND pc.productCategoryId = :smallCategoryId)
""")
    Page<InventoryResponseDto> searchInventory(Long largeCategoryId,
                                               Long mediumCategoryId,
                                               Long smallCategoryId,
                                               Pageable pageable
    );
}
