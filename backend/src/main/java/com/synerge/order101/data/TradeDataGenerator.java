package com.synerge.order101.data;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.repository.StoreOrderDetailRepository;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductRepository;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.PurchaseDetail;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import com.synerge.order101.purchase.model.repository.PurchaseRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.supplier.model.repository.SupplierRepository;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TradeDataGenerator {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseDetailRepository purchaseDetailRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final StoreOrderDetailRepository storeOrderDetailRepository;

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    private final Random random = new Random();

    /**
     * 1. 발주 데이터 생성 (본사 -> 공급사)
     * - 창고(1개), 공급사(14개) 중 랜덤 선택
     * - 담당자(User)는 '본사 직원(HQ)' 중에서만 선택
     * - 상품 및 가격은 공급사별 계약 정보(ProductSupplier)에서 조회
     */
    @Transactional
    public void generatePurchases(int count) {
        List<Supplier> suppliers = supplierRepository.findAll();
        Warehouse warehouse = warehouseRepository.findById(1L).orElseThrow();

        List<User> managers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.HQ)
                .toList();

        // 데이터 유효성 검사
        if (suppliers.isEmpty()) {
            System.out.println("[Skip] Cannot generate Purchases. Missing Supplier data.");
            return;
        }

        if (managers.isEmpty()) {
            System.out.println("[Skip] Cannot generate Purchases. No HQ Managers found.");
            return;
        }

        List<PurchaseDetail> detailList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Supplier supplier = suppliers.get(random.nextInt(suppliers.size()));

            // 해당 공급사가 취급하는 상품 목록(ProductSupplier) 조회
            List<ProductSupplier> supplierItems = productSupplierRepository.findBySupplier(supplier);

            User manager = managers.get(random.nextInt(managers.size()));

            LocalDateTime pastDate = LocalDateTime.now().minusDays(random.nextInt(30)); // 최근 30일

            Purchase purchase = Purchase.builder()
                    .supplier(supplier)
                    .warehouse(warehouse)
                    .user(manager)
                    .orderType(Purchase.OrderType.MANUAL)
                    .orderStatus(OrderStatus.SUBMITTED)
                    .poDate(pastDate)
                    .createdAt(pastDate)
                    .build();

            purchase = purchaseRepository.save(purchase);

            // 발주 상세(PurchaseDetail) 생성 (1건당 1~5개 품목)
            int itemCount = random.nextInt(Math.min(5, supplierItems.size())) + 1;

            for (int j = 0; j < itemCount; j++) {
                ProductSupplier ps = supplierItems.get(random.nextInt(supplierItems.size()));
                Product product = ps.getProduct();

                // (만약 null이면 Product의 기본 가격 사용)
                BigDecimal unitPrice = ps.getPurchasePrice() != null ? ps.getPurchasePrice() : product.getPrice();

                detailList.add(PurchaseDetail.builder()
                        .purchase(purchase)
                        .product(product)
                        .orderQty(random.nextInt(50) + 10) // 10 ~ 50개
                        .unitPrice(unitPrice)
                        .deadline(pastDate.toLocalDate().plusDays(ps.getLeadTimeDays() != null ? ps.getLeadTimeDays() : 3)) // 리드타임 반영
                        .createdAt(pastDate)
                        .build());
            }
        }

        purchaseDetailRepository.saveAll(detailList);
    }

    /**
     * 2. 주문 데이터 생성 (가맹점 -> 본사)
     * - 가맹점(1개), 창고(1개) 매핑하여 생성
     * - 주문자(User)는 '점주(STORE_ADMIN)' 중에서만 선택
     */
    @Transactional
    public void generateStoreOrders(int count) {
        Store store = storeRepository.findById(1L).orElseThrow();
        Warehouse warehouse = warehouseRepository.findById(1L).orElseThrow();
        List<Product> products = productRepository.findAll();

        // 점주 필터링 (STORE_ADMIN 권한을 가진 유저만 조회)
        List<User> storeOwners = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STORE_ADMIN)
                .toList();

        if (products.isEmpty()) {
            System.out.println("[Skip] Cannot generate StoreOrders. Missing base data.");
            return;
        }

        if (storeOwners.isEmpty()) {
            System.out.println("[Skip] Cannot generate StoreOrders. No Store Owners found.");
            return;
        }

        // 점주 1명 고정 (리스트의 첫 번째 유저 사용)
        User fixedStoreOwner = storeOwners.get(0);

        List<StoreOrderDetail> detailList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            LocalDateTime orderDate = LocalDateTime.now().minusDays(random.nextInt(30));

            StoreOrder order = StoreOrder.builder()
                    .store(store)
                    .warehouse(warehouse)
                    .user(fixedStoreOwner)
                    .orderDatetime(orderDate)
                    .createdAt(orderDate)
                    .orderStatus(OrderStatus.SUBMITTED)
//                    .orderStatus(OrderStatus.CONFIRMED)
                    .shipmentStatus(ShipmentStatus.WAITING)
                    .build();

            order = storeOrderRepository.save(order);

            // 주문 상세(StoreOrderDetail) 생성 (1건당 1~5개 품목)
            int itemCount = random.nextInt(5) + 1;
            for (int j = 0; j < itemCount; j++) {
                Product product = products.get(random.nextInt(products.size()));
                int qty = random.nextInt(20) + 1; // 1~20개
                BigDecimal price = product.getPrice();
                BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));

                detailList.add(StoreOrderDetail.builder()
                        .storeOrder(order)
                        .product(product)
                        .orderQty(qty)
                        .unitPrice(price)
                        .amount(amount)
                        .createdAt(orderDate)
                        .build());
            }
        }

        storeOrderDetailRepository.saveAll(detailList);
    }
}