package com.synerge.order101.purchase.model.entity;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PurchaseDetailTest")
class PurchaseDetailTest {

    private Purchase purchase;
    private Product product;

    @BeforeEach
    void setUp() {
        Supplier supplier = new Supplier(
                1L,
                "SUP001",
                "테스트 공급사",
                "홍길동",
                "02-1234-5678",
                "서울시 강남구",
                null,
                null
        );

        User user = User.create(
                "manager@test.com",
                "encodedPassword",
                "관리자",
                Role.HQ_ADMIN,
                "010-1234-5678",
                null
        );

        Warehouse warehouse = new Warehouse();

        purchase = Purchase.builder()
                .purchaseId(1L)
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        product = Product.builder()
                .productId(1L)
                .productName("테스트 상품")
                .productCode("PROD001")
                .price(BigDecimal.valueOf(10000))
                .build();
    }

    @Test
    @DisplayName("PurchaseDetail 생성 테스트")
    void createPurchaseDetail() {
        // given
        Integer orderQty = 100;
        BigDecimal unitPrice = BigDecimal.valueOf(8000);
        LocalDate deadline = LocalDate.now().plusDays(7);

        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(orderQty)
                .unitPrice(unitPrice)
                .deadline(deadline)
                .build();

        // then
        assertThat(detail).isNotNull();
        assertThat(detail.getPurchase()).isEqualTo(purchase);
        assertThat(detail.getProduct()).isEqualTo(product);
        assertThat(detail.getOrderQty()).isEqualTo(orderQty);
        assertThat(detail.getUnitPrice()).isEqualByComparingTo(unitPrice);
        assertThat(detail.getDeadline()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("주문 수량 변경 테스트")
    void updateOrderQty() {
        // given
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.valueOf(8000))
                .deadline(LocalDate.now().plusDays(7))
                .build();

        // when
        detail.updateOrderQty(150);

        // then
        assertThat(detail.getOrderQty()).isEqualTo(150);
    }

    @Test
    @DisplayName("금액 계산 테스트")
    void calculateAmount() {
        // given
        Integer orderQty = 50;
        BigDecimal unitPrice = BigDecimal.valueOf(5000);
        BigDecimal expectedAmount = unitPrice.multiply(BigDecimal.valueOf(orderQty));

        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(orderQty)
                .unitPrice(unitPrice)
                .deadline(LocalDate.now().plusDays(7))
                .build();

        // then
        BigDecimal calculatedAmount = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getOrderQty()));
        assertThat(calculatedAmount).isEqualByComparingTo(expectedAmount);
        assertThat(calculatedAmount).isEqualByComparingTo(BigDecimal.valueOf(250000));
    }

    @Test
    @DisplayName("납기일 설정 테스트")
    void setDeadline() {
        // given
        LocalDate deadline = LocalDate.now().plusDays(14);

        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.valueOf(8000))
                .deadline(deadline)
                .build();

        // then
        assertThat(detail.getDeadline()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("상품 정보 연결 테스트")
    void productAssociation() {
        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.valueOf(8000))
                .deadline(LocalDate.now().plusDays(7))
                .build();

        // then
        assertThat(detail.getProduct()).isNotNull();
        assertThat(detail.getProduct().getProductId()).isEqualTo(1L);
        assertThat(detail.getProduct().getProductName()).isEqualTo("테스트 상품");
        assertThat(detail.getProduct().getProductCode()).isEqualTo("PROD001");
    }

    @Test
    @DisplayName("발주 정보 연결 테스트")
    void purchaseAssociation() {
        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.valueOf(8000))
                .deadline(LocalDate.now().plusDays(7))
                .build();

        // then
        assertThat(detail.getPurchase()).isNotNull();
        assertThat(detail.getPurchase().getPurchaseId()).isEqualTo(1L);
        assertThat(detail.getPurchase().getOrderType()).isEqualTo(Purchase.OrderType.MANUAL);
    }

    @Test
    @DisplayName("대량 주문 테스트")
    void largeQuantityOrder() {
        // given
        Integer largeQty = 10000;
        BigDecimal unitPrice = BigDecimal.valueOf(500);

        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(largeQty)
                .unitPrice(unitPrice)
                .deadline(LocalDate.now().plusDays(30))
                .build();

        // then
        assertThat(detail.getOrderQty()).isEqualTo(largeQty);
        BigDecimal totalAmount = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getOrderQty()));
        assertThat(totalAmount).isEqualByComparingTo(BigDecimal.valueOf(5000000));
    }

    @Test
    @DisplayName("수량 여러 번 변경 테스트")
    void multipleQuantityChanges() {
        // given
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.valueOf(8000))
                .deadline(LocalDate.now().plusDays(7))
                .build();

        // when & then
        detail.updateOrderQty(150);
        assertThat(detail.getOrderQty()).isEqualTo(150);

        detail.updateOrderQty(200);
        assertThat(detail.getOrderQty()).isEqualTo(200);

        detail.updateOrderQty(80);
        assertThat(detail.getOrderQty()).isEqualTo(80);
    }

    @Test
    @DisplayName("단가가 0인 경우 테스트")
    void zeroPriceTest() {
        // when
        PurchaseDetail detail = PurchaseDetail.builder()
                .purchase(purchase)
                .product(product)
                .orderQty(100)
                .unitPrice(BigDecimal.ZERO)
                .deadline(LocalDate.now().plusDays(7))
                .build();

        // then
        assertThat(detail.getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
