package com.synerge.order101.warehouse.model.repository;


import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory,Long> {

    @Query("""
        SELECT wi
        FROM WarehouseInventory wi
        JOIN FETCH wi.product p
        JOIN FETCH p.productCategory c
    """)
    List<WarehouseInventory> findAllWithProduct();

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

}
