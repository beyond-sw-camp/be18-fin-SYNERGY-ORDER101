package com.synerge.order101.supplier.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.supplier.model.dto.SupplierDetailRes;
import com.synerge.order101.supplier.model.dto.SupplierInfoRes;
import com.synerge.order101.supplier.model.dto.SupplierListRes;
import com.synerge.order101.supplier.model.dto.SupplierProductItemRes;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@DisplayName("SupplierServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class SupplierServiceImplTest {

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Test
    @Order(1)
    void getSuppliers_withoutKeyword_usesFindAll() {
        // Given
        int page = 1;
        int numOfRows = 10;
        String keyword = null;

        Supplier supplier = mock(Supplier.class);
        given(supplier.getSupplierId()).willReturn(1L);
        given(supplier.getSupplierCode()).willReturn("SUP-001");
        given(supplier.getSupplierName()).willReturn("테스트 공급사");
        given(supplier.getAddress()).willReturn("서울시 어딘가");
        given(supplier.getContactName()).willReturn("홍길동");
        given(supplier.getContactNumber()).willReturn("010-1111-2222");
        given(supplier.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 10, 0));

        Page<Supplier> resultPage = new PageImpl<>(
                List.of(supplier),
                PageRequest.of(0, numOfRows, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        given(supplierRepository.findAll(any(Pageable.class))).willReturn(resultPage);

        // When
        ItemsResponseDto<SupplierListRes> res =
                supplierService.getSuppliers(page, numOfRows, keyword);

        // Then
        assertThat(res).isNotNull();
        assertThat(res.getItems()).hasSize(1);
        SupplierListRes item = res.getItems().get(0);
        assertThat(item.getSupplierId()).isEqualTo(1L);
        assertThat(item.getSupplierCode()).isEqualTo("SUP-001");
        assertThat(item.getSupplierName()).isEqualTo("테스트 공급사");
        assertThat(item.getAddress()).isEqualTo("서울시 어딘가");
        assertThat(item.getContactName()).isEqualTo("홍길동");
        assertThat(item.getContactNumber()).isEqualTo("010-1111-2222");

        verify(supplierRepository, times(1)).findAll(any(Pageable.class));
        verify(supplierRepository, never())
                .findBySupplierNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @Order(2)
    void getSuppliers_withKeyword_usesFindByNameContainingIgnoreCase() {
        // Given
        int page = 1;
        int numOfRows = 10;
        String keyword = "식품";

        Supplier supplier = mock(Supplier.class);
        given(supplier.getSupplierId()).willReturn(2L);
        given(supplier.getSupplierCode()).willReturn("SUP-002");
        given(supplier.getSupplierName()).willReturn("가전제품 공급사");
        given(supplier.getAddress()).willReturn("부산시 어딘가");
        given(supplier.getContactName()).willReturn("김철수");
        given(supplier.getContactNumber()).willReturn("010-3333-4444");
        given(supplier.getCreatedAt()).willReturn(LocalDateTime.of(2025, 2, 1, 9, 0));

        Page<Supplier> resultPage = new PageImpl<>(
                List.of(supplier),
                PageRequest.of(0, numOfRows, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        given(supplierRepository.findBySupplierNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .willReturn(resultPage);

        // When
        ItemsResponseDto<SupplierListRes> res =
                supplierService.getSuppliers(page, numOfRows, keyword);

        // Then
        assertThat(res).isNotNull();
        assertThat(res.getItems()).hasSize(1);
        SupplierListRes item = res.getItems().get(0);
        assertThat(item.getSupplierId()).isEqualTo(2L);
        assertThat(item.getSupplierCode()).isEqualTo("SUP-002");
        assertThat(item.getSupplierName()).isEqualTo("가전제품 공급사");

        verify(supplierRepository, times(1))
                .findBySupplierNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
        verify(supplierRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @Order(3)
    void getSupplierDetail_success() {
        // Given
        Long supplierId = 1L;
        int numOfRows = 10;
        int page = 1;
        String keyword = "TV";

        Supplier supplier = mock(Supplier.class);
        given(supplierRepository.findById(supplierId)).willReturn(Optional.of(supplier));
        given(supplier.getSupplierId()).willReturn(supplierId);
        given(supplier.getSupplierCode()).willReturn("SUP-001");
        given(supplier.getSupplierName()).willReturn("테스트 공급사");
        given(supplier.getAddress()).willReturn("서울 어딘가");
        given(supplier.getContactName()).willReturn("홍길동");
        given(supplier.getContactNumber()).willReturn("010-1111-2222");
        given(supplier.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 10, 0));

        ProductSupplier ps = mock(ProductSupplier.class);
        Product product = mock(Product.class);
        given(ps.getProduct()).willReturn(product);

        given(product.getProductId()).willReturn(100L);
        given(product.getProductCode()).willReturn("PRD-100");
        given(product.getProductName()).willReturn("TV");

        given(ps.getSupplierProductCode()).willReturn("SUP-001-PRD-100");
        given(product.getPrice()).willReturn(BigDecimal.valueOf(1500));
        given(ps.getLeadTimeDays()).willReturn(3);

        Page<ProductSupplier> psPage = new PageImpl<>(
                List.of(ps),
                PageRequest.of(0, numOfRows),
                1
        );

        given(productSupplierRepository.findBySupplierWithProductAndKeyword(
                eq(supplierId), eq(keyword), any(Pageable.class)
        )).willReturn(psPage);

        // When
        SupplierDetailRes res =
                supplierService.getSupplierDetail(supplierId, numOfRows, page, keyword);

        // Then
        assertThat(res).isNotNull();

        SupplierInfoRes info = res.getSupplier();
        assertThat(info.getSupplierId()).isEqualTo(supplierId);
        assertThat(info.getSupplierCode()).isEqualTo("SUP-001");
        assertThat(info.getSupplierName()).isEqualTo("테스트 공급사");
        assertThat(info.getAddress()).isEqualTo("서울 어딘가");
        assertThat(info.getContactName()).isEqualTo("홍길동");
        assertThat(info.getContactNumber()).isEqualTo("010-1111-2222");

        assertThat(res.getProducts()).hasSize(1);
        SupplierProductItemRes item = res.getProducts().get(0);
        assertThat(item.getProductId()).isEqualTo(100L);
        assertThat(item.getProductCode()).isEqualTo("PRD-100");
        assertThat(item.getProductName()).isEqualTo("TV");
        assertThat(item.getSupplierProductCode()).isEqualTo("SUP-001-PRD-100");
        assertThat(item.getPrice()).isEqualTo(BigDecimal.valueOf(1500));
        assertThat(item.getLeadTimeDays()).isEqualTo(3);

        assertThat(res.getPage()).isEqualTo(page);
        assertThat(res.getNumOfRows()).isEqualTo(numOfRows);
        assertThat(res.getTotalCount()).isEqualTo(1);
    }

    @Test
    @Order(4)
    void getSupplierDetail_notFound_throwsException() {
        // Given
        Long supplierId = 999L;
        given(supplierRepository.findById(supplierId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                supplierService.getSupplierDetail(supplierId, 10, 1, null)
        ).isInstanceOf(CustomException.class);

        verify(productSupplierRepository, never())
                .findBySupplierWithProductAndKeyword(anyLong(), anyString(), any(Pageable.class));
    }
}
