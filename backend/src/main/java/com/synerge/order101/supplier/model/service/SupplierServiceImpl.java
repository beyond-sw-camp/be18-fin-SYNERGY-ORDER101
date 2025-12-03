package com.synerge.order101.supplier.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.supplier.exception.SupplierErrorCode;
import com.synerge.order101.supplier.model.dto.SupplierDetailRes;
import com.synerge.order101.supplier.model.dto.SupplierInfoRes;
import com.synerge.order101.supplier.model.dto.SupplierListRes;
import com.synerge.order101.supplier.model.dto.SupplierProductItemRes;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto<SupplierListRes> getSuppliers(int page, int numOfRows, String keyword) {
        int pageIndex = Math.max(0, page - 1);

        Pageable pageable = PageRequest.of(pageIndex, numOfRows, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Supplier> resultPage;
        if(keyword != null && !keyword.isBlank()) {
            resultPage = supplierRepository.findBySupplierNameContainingIgnoreCase(keyword, pageable);
        }else {
            resultPage = supplierRepository.findAll(pageable);
        }

        List<SupplierListRes> items = resultPage.getContent().stream()
                .map(s -> SupplierListRes.builder()
                        .supplierId(s.getSupplierId())
                        .supplierCode(s.getSupplierCode())
                        .supplierName(s.getSupplierName())
                        .address(s.getAddress())
                        .contactName(s.getContactName())
                        .contactNumber(s.getContactNumber())
                        .createdDate(s.getCreatedAt())
                        .build())
                .toList();
        int totalCount = (int) resultPage.getTotalElements();

        return new ItemsResponseDto<>(HttpStatus.OK, items, page, totalCount);
    }

    @Override
    public SupplierDetailRes getSupplierDetail(Long supplierId, int numOfRows, int page, String keyword) {
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow(() ->
                new CustomException(SupplierErrorCode.SUPPLIER_NOT_FOUND));

        SupplierInfoRes info = SupplierInfoRes.builder()
                .supplierId(supplier.getSupplierId())
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getSupplierName())
                .address(supplier.getAddress())
                .contactName(supplier.getContactName())
                .contactNumber(supplier.getContactNumber())
                .createdAt(supplier.getCreatedAt())
                .build();

        int pageIndex = Math.max(0, page - 1);

        Pageable pageable = PageRequest.of(pageIndex, numOfRows);

        Page<ProductSupplier> psPage = productSupplierRepository.findBySupplierWithProductAndKeyword(supplierId, keyword, pageable);

        List<SupplierProductItemRes> items = psPage.getContent().stream()
                .map(ps -> SupplierProductItemRes.builder()
                        .productId(ps.getProduct().getProductId())
                        .productCode(ps.getProduct().getProductCode())
                        .supplierProductCode(ps.getSupplierProductCode())
                        .productName(ps.getProduct().getProductName())
                        .purchasePrice(ps.getPurchasePrice())           // 공급가
                        .price(ps.getProduct().getPrice())              // 판매가
                        .leadTimeDays(ps.getLeadTimeDays())
                        .build())
                .toList();

        return SupplierDetailRes.builder()
                .supplier(info)
                .products(items)
                .page(page)
                .numOfRows(numOfRows)
                .totalCount((int)psPage.getTotalElements())
                .build();

    }

}
