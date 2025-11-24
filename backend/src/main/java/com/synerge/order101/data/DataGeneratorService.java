package com.synerge.order101.data;

import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DataGeneratorService {
    // 필요한 Repository 주입
    @Autowired private ProductRepository productRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductSupplierRepository productSupplierRepository;

    // 공급사가 제공하는 상품 목록을 랜덤하게 추가합니다.
    @Transactional
    public void generateProductSupplierData(int count) {

        System.out.println("--- 🚀 테스트 데이터 생성 시작: " + count + "건 ---");

        // 1단계: 유효한 ID 목록 조회
        List<Product> validProductIds = productRepository.findAllProducts();
        List<Supplier> validSupplierIds = supplierRepository.findAllSuppliers();

        if (validProductIds.isEmpty() || validSupplierIds.isEmpty()) {
            System.out.println("⚠️ 오류: 유효한 Product ID 또는 Supplier ID가 DB에 없어 데이터 생성을 건너뜁니다.");
            return;
        }

        // 2단계: 데이터 생성 및 배치 삽입
        Random random = new Random();
        List<ProductSupplier> batchList = new ArrayList<>();

        // created_at 범위 설정 (2000-01-01 ~ 2025-11-22)
        long minDay = LocalDate.of(2000, 1, 1).toEpochDay();
        long maxDay = LocalDate.of(2025, 11, 22).toEpochDay();

        for (int i = 0; i < count; i++) {

            Product randomProduct = validProductIds.get(random.nextInt(validProductIds.size()));
            Supplier randomSupplier = validSupplierIds.get(random.nextInt(validSupplierIds.size()));
            long randomDay = minDay + random.nextInt((int) (maxDay - minDay));
            LocalDate randomDate = LocalDate.ofEpochDay(randomDay);

            ProductSupplier ps = ProductSupplier.builder()
                    .product(randomProduct)
                    .supplier(randomSupplier)
                    .leadTimeDays(random.nextInt(6)+1) // 1~6 랜덤
                    .purchasePrice(BigDecimal.valueOf(random.nextInt(1950001) + 50000L))
                    .createdAt(randomDate.atTime(random.nextInt(24), random.nextInt(60)))
                    .supplierProductCode("SP"+String.format("%03d", random.nextInt(1000)))
                    .build();
            batchList.add(ps);
        }

        productSupplierRepository.saveAll(batchList);
        System.out.println("--- ✅ 테스트 데이터 생성 완료: " + count + "건 삽입 ---");
    }


}
