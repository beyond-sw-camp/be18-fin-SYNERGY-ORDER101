package com.synerge.order101.product.model.service;

import com.amazonaws.services.s3.AmazonS3;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.inbound.model.repository.InboundDetailRepository;
import com.synerge.order101.product.model.dto.ProductCreateReq;
import com.synerge.order101.product.model.dto.ProductCreateRes;
import com.synerge.order101.product.model.dto.ProductInventoryDetailRes;
import com.synerge.order101.product.model.dto.ProductListRes;
import com.synerge.order101.product.model.dto.ProductRes;
import com.synerge.order101.product.model.dto.ProductUpdateReq;
import com.synerge.order101.product.model.entity.CategoryLevel;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductCategory;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductCategoryRepository;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import com.synerge.order101.warehouse.model.service.InventoryServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ProductServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InboundDetailRepository inboundDetailRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private InventoryServiceImpl inventoryService;

    @Mock
    private AmazonS3 amazonS3;

    @Test
    @Order(1)
    void createProduct_withSupplier_success() {
        // Given
        ProductCategory small = mock(ProductCategory.class);
        given(productCategoryRepository.findById(12L)).willReturn(Optional.of(small));
        given(small.getCategoryLevel()).willReturn(CategoryLevel.SMALL);
        given(small.getProductCategoryId()).willReturn(12L);

        Supplier supplier = mock(Supplier.class);
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));
        // supplier.getSupplierCode() 는 내부에서 사용되지만, 값 자체는 검증 X

        ProductCreateReq request = mock(ProductCreateReq.class);
        given(request.getCategorySmallId()).willReturn(12L);
        given(request.getSupplierId()).willReturn(1L);
        given(request.getCategoryMediumId()).willReturn((Long) null);
        given(request.getCategoryLargeId()).willReturn((Long) null);

        MultipartFile imageFile = null;

        // When
        ProductCreateRes res = productService.create(request, imageFile);

        // Then
        assertThat(res).isNotNull();
        verify(productRepository, times(1)).save(any(Product.class));
        verify(inventoryService, times(1)).createInventory(any(Product.class));
        verify(productSupplierRepository, times(1)).save(any(ProductSupplier.class));
    }

    @Test
    @Order(2)
    void updateProduct_basicFields_success() {
        // Given
        Long productId = 1L;

        Product product = mock(Product.class);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        ProductCategory small = mock(ProductCategory.class);
        given(productCategoryRepository.findById(12L)).willReturn(Optional.of(small));
        given(small.getCategoryLevel()).willReturn(CategoryLevel.SMALL);
        given(small.getCategoryName()).willReturn("소분류");
        // medium/large 는 요청에서 null 이라 검증 로직 통과 → 이름 null 이어도 괜찮음
        given(small.getParent()).willReturn(null);

        ProductUpdateReq request = mock(ProductUpdateReq.class);
        given(request.getCategorySmallId()).willReturn(12L);
        given(request.getProductName()).willReturn("수정 상품명");
        given(request.getDescription()).willReturn("수정 설명");
        given(request.getStatus()).willReturn(Boolean.TRUE);
        given(request.getCategoryMediumId()).willReturn((Long) null);
        given(request.getCategoryLargeId()).willReturn((Long) null);
        // medium/large/removeImage 등은 null 로 두어 분기 타지 않게 함

        given(product.getProductName()).willReturn("수정 상품명");
        given(product.getProductCode()).willReturn("PRD-01-XXXX");
        given(product.getDescription()).willReturn("수정 설명");
        given(product.getStatus()).willReturn(Boolean.TRUE);
        // price, imageUrl 등은 굳이 검증하지 않아서 stub 생략 가능

        // When
        ProductRes res = productService.update(productId, request, null);

        // Then
        assertThat(res).isNotNull();
        assertThat(res.getProductName()).isEqualTo("수정 상품명");
        assertThat(res.getProductCode()).isEqualTo("PRD-01-XXXX");
        assertThat(res.getCategorySmallName()).isEqualTo("소분류");
        assertThat(res.getCategoryMediumName()).isNull();
        assertThat(res.getCategoryLargeName()).isNull();
    }

    @Test
    @Order(3)
    void getProductInventory_success() {
        // Given
        Long productId = 1L;
        int page = 1;
        int numOfRows = 10;
        int limit = numOfRows;
        int offset = 0; // pageIndex 0

        Product product = mock(Product.class);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(product.getProductId()).willReturn(productId);
        given(product.getProductCode()).willReturn("PRD-01-XXXX");
        given(product.getProductName()).willReturn("테스트 상품");

        given(warehouseInventoryRepository.sumOnHandAll(productId)).willReturn(100L);
        given(warehouseInventoryRepository.sumSafetyAll(productId)).willReturn(20L);

        List<Object[]> rows = List.<Object[]>of(
                new Object[]{
                        "IN-001",
                        "INBOUND",
                        10L,
                        Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 10, 0))
                }
        );
        given(inboundDetailRepository.findMovements(productId, limit, offset))
                .willReturn(rows);
        given(inboundDetailRepository.countMovements(productId))
                .willReturn(1L);

        // When
        ProductInventoryDetailRes res = productService.getProductInventory(productId, page, numOfRows);

        // Then
        assertThat(res).isNotNull();
        assertThat(res.getSummary().getProductId()).isEqualTo(productId);
        assertThat(res.getSummary().getCurrentQty()).isEqualTo(100L);
        assertThat(res.getSummary().getSafetyQty()).isEqualTo(20L);
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getMovementNo()).isEqualTo("IN-001");
    }

    @Test
    @Order(4)
    void getProducts_success() {
        // Given
        int page = 1;
        int numOfRows = 5;
        String keyword = "TV";

        ProductCategory category = mock(ProductCategory.class);
        given(category.getProductCategoryId()).willReturn(3L);
        given(category.getCategoryName()).willReturn("전자제품");
        given(category.getCategoryLevel()).willReturn(CategoryLevel.SMALL);

        Product product = mock(Product.class);
        given(product.getProductId()).willReturn(1L);
        given(product.getProductCode()).willReturn("PRD-03-XXXX");
        given(product.getProductName()).willReturn("TV");
        given(product.getStatus()).willReturn(Boolean.TRUE);
        given(product.getProductCategory()).willReturn(category);

        Page<Product> productPage = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, numOfRows),
                1
        );

        given(productRepository.findAll(
                ArgumentMatchers.<Specification<Product>>any(),
                any(Pageable.class)
        )).willReturn(productPage);

        // When
        ItemsResponseDto<ProductListRes> result =
                productService.getProducts(page, numOfRows, keyword, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        ProductListRes item = result.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getProductCode()).isEqualTo("PRD-03-XXXX");
        assertThat(item.getProductName()).isEqualTo("TV");
        assertThat(item.getCategoryId()).isEqualTo(3L);
        assertThat(item.getCategoryName()).isEqualTo("전자제품");
    }

    @Test
    @Order(5)
    void getProduct_success() {
        // Given
        Long productId = 1L;

        ProductCategory small = mock(ProductCategory.class);
        ProductCategory medium = mock(ProductCategory.class);
        ProductCategory large = mock(ProductCategory.class);

        given(large.getCategoryName()).willReturn("대분류");
        given(medium.getCategoryName()).willReturn("중분류");
        given(small.getCategoryName()).willReturn("소분류");

        given(small.getParent()).willReturn(medium);
        given(medium.getParent()).willReturn(large);

        Product product = mock(Product.class);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(product.getProductCategory()).willReturn(small);
        given(product.getProductName()).willReturn("상품명");
        given(product.getProductCode()).willReturn("PRD-01-XXXX");
        given(product.getStatus()).willReturn(Boolean.TRUE);
        given(product.getImageUrl()).willReturn("http://example.com/image.jpg");
        given(product.getDescription()).willReturn("상품 설명");
        // price는 굳이 검증하지 않아서 stub 생략 가능

        // When
        ProductRes res = productService.getProduct(productId);

        // Then
        assertThat(res).isNotNull();
        assertThat(res.getProductName()).isEqualTo("상품명");
        assertThat(res.getProductCode()).isEqualTo("PRD-01-XXXX");
        assertThat(res.getCategorySmallName()).isEqualTo("소분류");
        assertThat(res.getCategoryMediumName()).isEqualTo("중분류");
        assertThat(res.getCategoryLargeName()).isEqualTo("대분류");
        assertThat(res.getDescription()).isEqualTo("상품 설명");
        assertThat(res.getImageUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(res.getStatus()).isTrue();
    }

    @Test
    @Order(8)
    void getProduct_notFound_throwsException() {
        // Given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @Order(9)
    void deleteProduct_success() {
        // Given
        Long productId = 1L;
        Product product = mock(Product.class);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // When
        productService.deleteProduct(productId);

        // Then
        verify(productRepository, times(1)).delete(product);
    }

}
