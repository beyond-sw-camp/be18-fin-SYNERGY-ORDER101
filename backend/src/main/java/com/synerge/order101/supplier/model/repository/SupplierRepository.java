package com.synerge.order101.supplier.model.repository;

import com.synerge.order101.supplier.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    int countSupplierByAddress(String address);

    Page<Supplier> findBySupplierNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT s FROM Supplier s")
    List<Supplier> findAllSuppliers();
}
