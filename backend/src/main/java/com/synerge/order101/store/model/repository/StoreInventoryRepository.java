package com.synerge.order101.store.model.repository;

import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.entity.StoreInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory,Long> {
    Optional<StoreInventory> findByStoreAndProduct(Store store, Product product);

    Page<StoreInventory> findByStore(Store store, Pageable pageable);
}
