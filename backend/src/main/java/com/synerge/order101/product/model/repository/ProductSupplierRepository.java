package com.synerge.order101.product.model.repository;

import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {

    @Query(
            value = """
    select ps from ProductSupplier ps
    join fetch ps.product p
    where ps.supplier.supplierId = :supplierId
      and (
        :keyword is null or :keyword = '' or
        lower(p.productName) like lower(concat('%', :keyword, '%')) or
        lower(p.productCode) like lower(concat('%', :keyword, '%')) or
        lower(ps.supplierProductCode) like lower(concat('%', :keyword, '%'))
      )
  """,
            countQuery = """
    select count(ps) from ProductSupplier ps
    join ps.product p
    where ps.supplier.supplierId = :supplierId
      and (
        :keyword is null or :keyword = '' or
        lower(p.productName) like lower(concat('%', :keyword, '%')) or
        lower(p.productCode) like lower(concat('%', :keyword, '%')) or
        lower(ps.supplierProductCode) like lower(concat('%', :keyword, '%'))
      )
  """
    )
    Page<ProductSupplier> findBySupplierWithProductAndKeyword(
            @Param("supplierId") Long supplierId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<ProductSupplier> findByProductAndSupplier(Product product, Supplier supplier);

    Optional<ProductSupplier> findByProduct_ProductIdAndSupplier_SupplierId(Long productId, Long supplierId);

    Optional<ProductSupplier> findTop1ByProduct(Product product);

    Optional<ProductSupplier> findByProduct(Product product);

    List<ProductSupplier> findBySupplier(Supplier supplier);
}
