package com.synerge.order101.supplier.model.service;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.supplier.model.dto.SupplierDetailRes;
import com.synerge.order101.supplier.model.dto.SupplierListRes;

import java.util.List;

public interface SupplierService {
    ItemsResponseDto<SupplierListRes> getSuppliers(int page, int numOfRows, String address);

    SupplierDetailRes getSupplierDetail(Long supplierId, int numOfRows, int page, String keyword);
}
