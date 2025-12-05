package com.synerge.order101.order.model.entity;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoreOrderTest")
class StoreOrderTest {

    private Store store;
    private Warehouse warehouse;
    private User user;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .storeId(1L)
                .storeName("테스트 가맹점")
                .storeCode("ST001")
                .address("서울시 강남구")
                .contactNumber("02-1234-5678")
                .isActive(true)
                .build();

        warehouse = new Warehouse();

        user = User.create(
                "hong@test.com",
                "encodedPassword",
                "홍길동",
                Role.STORE_ADMIN,
                "010-1234-5678",
                null
        );
    }

    @Test
    @DisplayName("StoreOrder 생성 테스트")
    void createStoreOrder() {
        // given
        String remark = "긴급 주문";

        // when
        StoreOrder order = StoreOrder.builder()
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .orderStatus(OrderStatus.SUBMITTED)
                .shipmentStatus(ShipmentStatus.WAITING)
                .remark(remark)
                .build();

        // then
        assertThat(order).isNotNull();
        assertThat(order.getStore()).isEqualTo(store);
        assertThat(order.getWarehouse()).isEqualTo(warehouse);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(order.getShipmentStatus()).isEqualTo(ShipmentStatus.WAITING);
        assertThat(order.getRemark()).isEqualTo(remark);
    }

    @Test
    @DisplayName("주문번호 생성 테스트")
    void generateOrderNo() {
        // given
        StoreOrder order = StoreOrder.builder()
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .build();

        // when
        String orderNo = order.generateOrderNo();

        // then
        assertThat(orderNo).isNotNull();
        assertThat(orderNo).startsWith("OR");
        assertThat(orderNo).hasSize(14); // OR + 8자리 날짜 + 4자리 랜덤
    }

    @Test
    @DisplayName("주문 상태 변경 테스트")
    void updateOrderState() {
        // given
        StoreOrder order = StoreOrder.builder()
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .orderStatus(OrderStatus.SUBMITTED)
                .build();

        // when
        order.updateOrderState(OrderStatus.CONFIRMED);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("배송 상태 변경 테스트")
    void updateShipmentStatus() {
        // given
        StoreOrder order = StoreOrder.builder()
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .shipmentStatus(ShipmentStatus.WAITING)
                .build();

        // when
        order.updateShipmentStatus(ShipmentStatus.IN_TRANSIT);

        // then
        assertThat(order.getShipmentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    @DisplayName("기본값 설정 테스트")
    void defaultValues() {
        // when
        StoreOrder order = StoreOrder.builder()
                .store(store)
                .warehouse(warehouse)
                .user(user)
                .build();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(order.getShipmentStatus()).isEqualTo(ShipmentStatus.WAITING);
        assertThat(order.getStoreOrderDetails()).isEmpty();
    }

    @Test
    @DisplayName("생성자를 통한 StoreOrder 생성 테스트")
    void createStoreOrderWithConstructor() {
        // given
        String remark = "비고사항";

        // when
        StoreOrder order = new StoreOrder(store, warehouse, user, remark);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getStore()).isEqualTo(store);
        assertThat(order.getWarehouse()).isEqualTo(warehouse);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getRemark()).isEqualTo(remark);
    }
}
