package com.synerge.order101.store.model.repository;

import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.entity.StoreInventory;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory,Long> {


    // 2. 입고 예정 수량 합계
    @Query("""
        select coalesce(sum(si.inTransitQty), 0)
        from StoreInventory si
        where si.store.storeId = :storeId
    """)
    int sumInTransitQty(@Param("storeId") Long storeId);

    Optional<StoreInventory> findByStoreAndProduct(Store store, Product product);

    Page<StoreInventory> findByStore(Store store, Pageable pageable);
}
