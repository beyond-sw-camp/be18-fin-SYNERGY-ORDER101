package com.synerge.order101.order.model.entity;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoreOrderDetailTest")
class StoreOrderDetailTest {

    private StoreOrder storeOrder;
    private Product product;

    @BeforeEach
    void setUp() {
        Store store = Store.builder()
                .storeId(1L)
                .storeName("테스트 가맹점")
                .storeCode("ST001")
                .address("서울시 강남구")
                .contactNumber("02-1234-5678")
                .isActive(true)
                .build();

        Warehouse warehouse = new Warehouse();

        User user = User.create(
                "hong@test.com",
                "encodedPassword",
                "홍길동",
                Role.STORE_ADMIN,
                "010-1234-5678",
                null
        );

        storeOrder = StoreOrder.builder()
                .storeOrderId(1L)
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .orderStatus(OrderStatus.SUBMITTED)
                .shipmentStatus(ShipmentStatus.WAITING)
                .build();

        product = Product.builder()
                .productId(1L)
                .productName("테스트 상품")
                .productCode("TEST-001")
                .price(BigDecimal.valueOf(10000))
                .build();
    }

    @Test
    @DisplayName("StoreOrderDetail 생성 테스트 - Builder")
    void createStoreOrderDetail_WithBuilder() {
        // given
        Integer orderQty = 10;
        BigDecimal unitPrice = BigDecimal.valueOf(10000);
        BigDecimal amount = BigDecimal.valueOf(100000);

        // when
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(orderQty)
                .unitPrice(unitPrice)
                .amount(amount)
                .build();

        // then
        assertThat(orderDetail).isNotNull();
        assertThat(orderDetail.getStoreOrder()).isEqualTo(storeOrder);
        assertThat(orderDetail.getProduct()).isEqualTo(product);
        assertThat(orderDetail.getOrderQty()).isEqualTo(orderQty);
        assertThat(orderDetail.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderDetail.getAmount()).isEqualTo(amount);
    }

    @Test
    @DisplayName("StoreOrderDetail 생성 테스트 - Constructor")
    void createStoreOrderDetail_WithConstructor() {
        // given
        Integer orderQty = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(20000);
        BigDecimal amount = BigDecimal.valueOf(100000);

        // when
        StoreOrderDetail orderDetail = new StoreOrderDetail(
                storeOrder, product, orderQty, unitPrice, amount
        );

        // then
        assertThat(orderDetail).isNotNull();
        assertThat(orderDetail.getStoreOrder()).isEqualTo(storeOrder);
        assertThat(orderDetail.getProduct()).isEqualTo(product);
        assertThat(orderDetail.getOrderQty()).isEqualTo(orderQty);
        assertThat(orderDetail.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderDetail.getAmount()).isEqualTo(amount);
    }

    @Test
    @DisplayName("금액 계산 테스트")
    void calculateAmount() {
        // given
        Integer orderQty = 15;
        BigDecimal unitPrice = BigDecimal.valueOf(5000);
        BigDecimal expectedAmount = unitPrice.multiply(BigDecimal.valueOf(orderQty));

        // when
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(orderQty)
                .unitPrice(unitPrice)
                .amount(expectedAmount)
                .build();

        // then
        assertThat(orderDetail.getAmount()).isEqualTo(expectedAmount);
        assertThat(orderDetail.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(75000));
    }

    @Test
    @DisplayName("상품 정보 연결 테스트")
    void productAssociation() {
        // given & when
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(10)
                .unitPrice(product.getPrice())
                .amount(product.getPrice().multiply(BigDecimal.valueOf(10)))
                .build();

        // then
        assertThat(orderDetail.getProduct()).isNotNull();
        assertThat(orderDetail.getProduct().getProductId()).isEqualTo(1L);
        assertThat(orderDetail.getProduct().getProductName()).isEqualTo("테스트 상품");
        assertThat(orderDetail.getProduct().getProductCode()).isEqualTo("TEST-001");
    }

    @Test
    @DisplayName("주문 정보 연결 테스트")
    void orderAssociation() {
        // given & when
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(10)
                .unitPrice(BigDecimal.valueOf(10000))
                .amount(BigDecimal.valueOf(100000))
                .build();

        // then
        assertThat(orderDetail.getStoreOrder()).isNotNull();
        assertThat(orderDetail.getStoreOrder().getStoreOrderId()).isEqualTo(1L);
        assertThat(orderDetail.getStoreOrder().getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    @DisplayName("수량이 0인 경우 테스트")
    void zeroQuantity() {
        // given & when
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(0)
                .unitPrice(BigDecimal.valueOf(10000))
                .amount(BigDecimal.ZERO)
                .build();

        // then
        assertThat(orderDetail.getOrderQty()).isEqualTo(0);
        assertThat(orderDetail.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("대량 주문 테스트")
    void largeQuantityOrder() {
        // given
        Integer largeQty = 1000;
        BigDecimal unitPrice = BigDecimal.valueOf(500);
        BigDecimal expectedAmount = unitPrice.multiply(BigDecimal.valueOf(largeQty));

        // when
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .storeOrder(storeOrder)
                .product(product)
                .orderQty(largeQty)
                .unitPrice(unitPrice)
                .amount(expectedAmount)
                .build();

        // then
        assertThat(orderDetail.getOrderQty()).isEqualTo(largeQty);
        assertThat(orderDetail.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
    }
}
