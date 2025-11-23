package com.synerge.order101.product.model.repository;

import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.supplier.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {

    @Query("""
SELECT ps
FROM ProductSupplier ps
JOIN FETCH ps.product p
WHERE ps.supplier.supplierId = :supplierId
""")
    Page<ProductSupplier> findBySupplierWithProduct(
            @Param("supplierId") Long supplierId,
            Pageable pageable
    );

    Optional<ProductSupplier> findByProductAndSupplier(Product product, Supplier supplier);

    Optional<ProductSupplier> findByProduct(Product product);
}
