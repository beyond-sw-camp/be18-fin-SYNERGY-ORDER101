package com.synerge.order101.product.model.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.inbound.model.repository.InboundDetailRepository;
import com.synerge.order101.inbound.model.repository.InboundRepository;
import com.synerge.order101.product.exception.ProductErrorCode;
import com.synerge.order101.product.model.dto.InventoryMovementRes;
import com.synerge.order101.product.model.dto.InventorySummaryRes;
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
import com.synerge.order101.supplier.exception.SupplierErrorCode;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import com.synerge.order101.warehouse.model.service.InventoryServiceImpl;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final InboundDetailRepository inboundDetailRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final InventoryServiceImpl inventoryService;

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final String UPLOAD_ROOT = "uploads";

    @Override
    public ProductCreateRes create(ProductCreateReq request, MultipartFile imageFile) {

        ProductCategory small = productCategoryRepository.findById(request.getCategorySmallId()).orElseThrow(() ->
                new CustomException(ProductErrorCode.INVALID_SMALL_CATEGORY));

        if(small.getCategoryLevel() != CategoryLevel.SMALL) {
            throw new CustomException(ProductErrorCode.MUST_BE_SMALL);
        }

        ProductCategory medium = small.getParent();
        ProductCategory large = (medium != null) ? medium.getParent() : null;

        if(request.getCategoryMediumId() != null && (medium == null || !medium.getProductCategoryId().equals(request.getCategoryMediumId()))) {
            throw new CustomException(ProductErrorCode.SMALL_MEDIUM);
        }
        if(request.getCategoryLargeId() != null && (large == null || !large.getProductCategoryId().equals(request.getCategoryLargeId()))) {
            throw new CustomException(ProductErrorCode.LARGE_MEDIUM);
        }

        String productCode = generateProductCode(small);

        String imageUrl = request.getImageUrl();

        if(imageFile != null && !imageFile.isEmpty()) {
            imageUrl = saveProductImage(imageFile);
        }

        Product product = Product.builder()
                .productCode(productCode)
                .productName(request.getProductName())
                .status(Boolean.TRUE.equals(request.getStatus()))
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(imageUrl)
                .productCategory(small)
                .build();

        productRepository.save(product);

        Supplier supplier = null;
        if(request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow(() ->
                    new CustomException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
        }

        if(supplier != null) {

            String supplierProductCode = product.getProductCode() + "_" + supplier.getSupplierCode();

            int leadTimeDays = java.util.concurrent.ThreadLocalRandom.current().nextInt(3, 8);

            ProductSupplier ps = ProductSupplier.builder()
                    .product(product)
                    .supplier(supplier)
                    .supplierProductCode(supplierProductCode)
                    .purchasePrice(request.getDeliveryPrice())
                    .leadTimeDays(leadTimeDays)
                    .build();
            productSupplierRepository.save(ps);
        }

        inventoryService.createInventory(product);

        return new  ProductCreateRes(product.getProductId());
    }

    @Override
    public ProductRes update(Long productId, ProductUpdateReq request, MultipartFile imageFile) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

        ProductCategory small = productCategoryRepository.findById(request.getCategorySmallId()).orElseThrow(() ->
                new CustomException(ProductErrorCode.INVALID_SMALL_CATEGORY));

        if(small.getCategoryLevel() != CategoryLevel.SMALL) {
            throw new CustomException(ProductErrorCode.MUST_BE_SMALL);
        }

        ProductCategory medium = small.getParent();
        ProductCategory large = (medium != null) ? medium.getParent() : null;

        if(request.getCategoryMediumId() != null && (medium == null || !medium.getProductCategoryId().equals(request.getCategoryMediumId()))) {
            throw new CustomException(ProductErrorCode.SMALL_MEDIUM);
        }
        if(request.getCategoryLargeId() != null && (large == null || !large.getProductCategoryId().equals(request.getCategoryLargeId()))) {
            throw new CustomException(ProductErrorCode.LARGE_MEDIUM);
        }

        String newImageUrl = product.getImageUrl();

        if(imageFile != null && !imageFile.isEmpty()) {
            if(newImageUrl != null) {
                deleteProductImage(newImageUrl);
            }
            newImageUrl = saveProductImage(imageFile);
        } else if (Boolean.TRUE.equals(request.getRemoveImage())) {
            if(newImageUrl != null) {
                deleteProductImage(newImageUrl);
            }
            newImageUrl = null;
        }

        product.update(
                request.getProductName(),
                request.getDescription(),
                request.getPrice(),
                newImageUrl,
                request.getStatus(),
                small
        );

        return ProductRes.builder()
                .productName(product.getProductName())
                .productCode(product.getProductCode())
                .categorySmallName(small.getCategoryName())
                .categoryMediumName(medium != null ? medium.getCategoryName() : null)
                .categoryLargeName(large !=  null ? large.getCategoryName() : null)
                .price(product.getPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public ProductInventoryDetailRes getProductInventory(Long productId, int page, int numOfRows) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));
        long currentQty = warehouseInventoryRepository.sumOnHandAll(productId);
        long safetyQty = warehouseInventoryRepository.sumSafetyAll(productId);

        InventorySummaryRes summary = InventorySummaryRes.builder()
                .productId(product.getProductId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .currentQty(currentQty)
                .safetyQty(safetyQty)
                .build();

        int pageIndex = Math.max(0, page - 1);
        int limit = numOfRows;
        int offset = pageIndex * numOfRows;

        List<Object[]> rows = inboundDetailRepository.findMovements(productId, limit, offset);
        long total =  inboundDetailRepository.countMovements(productId);

        List<InventoryMovementRes> items = rows.stream()
                .map(r -> InventoryMovementRes.builder()
                        .movementNo((String) r[0])
                        .type((String) r[1])
                        .qty(((Number) r[2]).longValue())
                        .occurredAt(((Timestamp) r[3]).toLocalDateTime())
                        .build())
                .toList();
        return ProductInventoryDetailRes.builder()
                .summary(summary)
                .items(items)
                .page(page)
                .numOfRows(numOfRows)
                .totalCount((int) total)
                .build();
    }

    @Override
    public ItemsResponseDto<ProductListRes> getProducts(int page, int numOfRows, String keyword,
                                                        Long largeCategoryId,
                                                        Long mediumCategoryId, Long smallCategoryId) {
        // 1. 페이지 인덱스 보정: 클라이언트의 1-base 페이지 번호를 0-base 인덱스로 변환 (page가 1 미만이면 0으로 보정)
        int pageIndex = Math.max(0, page - 1);

        // 2. numOfRows (페이지 크기) 유효성 검증 및 보정 (수정된 부분)
        // Spring Data의 PageRequest는 페이지 크기가 1 이상이어야 하므로, 1 미만인 경우 안전한 값(10)으로 보정합니다.
        int safeNumOfRows = numOfRows;
        if (safeNumOfRows < 1) {
            safeNumOfRows = 10;
        }

        // 3. Pageable 객체 생성: 보정된 페이지 크기(safeNumOfRows)를 사용
        Pageable pageable = PageRequest.of(pageIndex, safeNumOfRows, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 4. 리포지토리 쿼리 실행
        Page<Product> productPage = productRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // category join
            Join<Product, ProductCategory> small = root.join("productCategory");
            Join<ProductCategory, ProductCategory> medium = small.join("parent", JoinType.LEFT);
            Join<ProductCategory, ProductCategory> large = medium.join("parent", JoinType.LEFT);

            if (smallCategoryId != null) {
                predicates.add(cb.equal(small.get("productCategoryId"), smallCategoryId));
            }
            if (mediumCategoryId != null) {
                predicates.add(cb.equal(medium.get("productCategoryId"), mediumCategoryId));
            }
            if (largeCategoryId != null) {
                predicates.add(cb.equal(large.get("productCategoryId"), largeCategoryId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        // 5. 결과 DTO 매핑
        List<ProductListRes> items = productPage.getContent().stream()
                .map(s -> ProductListRes.builder()
                        .productId(s.getProductId())
                        .productCode(s.getProductCode())
                        .categoryId(s.getProductCategory().getProductCategoryId())
                        .categoryName(s.getProductCategory().getCategoryName())
                        .categoryLevel(String.valueOf(s.getProductCategory().getCategoryLevel()))
                        .productName(s.getProductName())
                        .price(s.getPrice())
                        .status(s.getStatus())
                        .build())
                .toList();

        int totalCount = (int) productPage.getTotalElements();

        // 6. 응답 DTO 반환
        return new ItemsResponseDto<>(HttpStatus.OK, items, page, totalCount);
    }

    @Override
    public ProductRes getProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

        ProductSupplier ps = productSupplierRepository.findFirstByProductOrderByCreatedAtAsc(product).orElse(null);

        BigDecimal purchasePrice = (ps != null) ? ps.getPurchasePrice() : null;

        return ProductRes.builder()
                .productName(product.getProductName())
                .productCode(product.getProductCode())
                .categoryLargeName(product.getProductCategory().getParent().getParent().getCategoryName())
                .categoryMediumName(product.getProductCategory().getParent().getCategoryName())
                .categorySmallName(product.getProductCategory().getCategoryName())
                .status(product.getStatus())
                .price(product.getPrice())
                .purchasePrice(purchasePrice)
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .build();
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {

        Product product = productRepository.findById(productId).orElseThrow(() ->
                new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));
        // 나중에 유저 권한 검증 구현
        // cascade, orphan
        productRepository.delete(product);
    }

    private String saveProductImage(MultipartFile imageFile) {
        if(imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        try {
            String originalName = imageFile.getOriginalFilename();
            String ext = StringUtils.getFilenameExtension(originalName);
            String fileName = "product-" + UUID.randomUUID() + (ext != null ? "." + ext : "");

            String key = "product-images/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageFile.getSize());
            metadata.setContentType(imageFile.getContentType());

            try (InputStream inputStream = imageFile.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        bucket,
                        key,
                        inputStream,
                        metadata
                );

                amazonS3.putObject(putObjectRequest);
            }

            return amazonS3.getUrl(bucket, key).toString();
        } catch (IOException e) {
            throw new CustomException(ProductErrorCode.IMAGE_UPLOAD_FAIL);
        }
    }

    private void deleteProductImage(String imageUrl) {
        if(imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {

            if(imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                if(!imageUrl.contains(bucket)) {
                    return;
                }

                URI uri = URI.create(imageUrl);
                String path = uri.getPath();
                if(path == null || path.isBlank()) {
                    return;
                }

                String key = path.startsWith("/") ? path.substring(1) : path;

                if(key.startsWith(bucket + "/")) {
                    key = key.substring(bucket.length() + 1);
                }

                amazonS3.deleteObject(bucket, key);
                return;
            }
            String fileName = Paths.get(imageUrl).getFileName().toString();
            Path filePath = Paths.get(System.getProperty("user.dir"),
                    UPLOAD_ROOT,
                    "product-images",
                    fileName);

            if(Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException | java.nio.file.InvalidPathException e) {
            throw new CustomException(ProductErrorCode.IMAGE_UPLOAD_FAIL);
        } catch (com.amazonaws.services.s3.model.AmazonS3Exception e) {
            // S3에 없으면(404) 그냥 넘어가도 되고, 지금은 일단 예외로 올려도 됨
            throw new CustomException(ProductErrorCode.IMAGE_UPLOAD_FAIL);
        }
    }

    private String generateProductCode(ProductCategory small) {
        String smallIdPart = String.format("%02d", small.getProductCategoryId());

        String uuidPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 4)
                .toUpperCase();

        return "PRD-" + smallIdPart + "-" + small.getCategoryName() + "-" + uuidPart;
    }



}
