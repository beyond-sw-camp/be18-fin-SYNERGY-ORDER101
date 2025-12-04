package com.synerge.order101.purchase.model.entity;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PurchaseTest")
class PurchaseTest {

    private Supplier supplier;
    private User user;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        supplier = new Supplier(
                1L,
                "SUP001",
                "테스트 공급사",
                "홍길동",
                "02-1234-5678",
                "서울시 강남구",
                null,
                null
        );

        user = User.create(
                "manager@test.com",
                "encodedPassword",
                "관리자",
                Role.HQ_ADMIN,
                "010-1234-5678",
                null
        );

        warehouse = new Warehouse();
    }

    @Test
    @DisplayName("Purchase 생성 테스트")
    void createPurchase() {
        // when
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        // then
        assertThat(purchase).isNotNull();
        assertThat(purchase.getSupplier()).isEqualTo(supplier);
        assertThat(purchase.getUser()).isEqualTo(user);
        assertThat(purchase.getWarehouse()).isEqualTo(warehouse);
        assertThat(purchase.getOrderStatus()).isEqualTo(OrderStatus.DRAFT_AUTO);
        assertThat(purchase.getOrderType()).isEqualTo(Purchase.OrderType.MANUAL);
    }

    @Test
    @DisplayName("발주번호 생성 테스트")
    void generatePoNo() {
        // given
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        // when
        String poNo = purchase.generatePoNo();

        // then
        assertThat(poNo).isNotNull();
        assertThat(poNo).startsWith("PO");
        assertThat(poNo).hasSize(14); // PO + 8자리 날짜 + 4자리 랜덤
    }

    @Test
    @DisplayName("주문 상태 변경 테스트")
    void updateOrderStatus() {
        // given
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        // when
        purchase.updateOrderStatus(OrderStatus.SUBMITTED);

        // then
        assertThat(purchase.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    @DisplayName("발주 제출 테스트")
    void submit() {
        // given
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        User submitUser = User.create(
                "submitter@test.com",
                "encodedPassword",
                "제출자",
                Role.HQ,
                "010-9876-5432",
                null
        );
        LocalDateTime submitTime = LocalDateTime.now();

        // when
        purchase.submit(submitUser, submitTime);

        // then
        assertThat(purchase.getUser()).isEqualTo(submitUser);
        assertThat(purchase.getPoDate()).isEqualTo(submitTime);
        assertThat(purchase.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    @DisplayName("OrderType AUTO 생성 테스트")
    void createAutoPurchase() {
        // when
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.AUTO)
                .build();

        // then
        assertThat(purchase.getOrderType()).isEqualTo(Purchase.OrderType.AUTO);
    }

    @Test
    @DisplayName("OrderType SMART 생성 테스트")
    void createSmartPurchase() {
        // when
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.SMART)
                .build();

        // then
        assertThat(purchase.getOrderType()).isEqualTo(Purchase.OrderType.SMART);
    }

    @Test
    @DisplayName("PurchaseDetails 초기화 테스트")
    void purchaseDetailsInitialization() {
        // when
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        // then
        assertThat(purchase.getPurchaseDetails()).isNotNull();
        assertThat(purchase.getPurchaseDetails()).isEmpty();
    }

    @Test
    @DisplayName("다단계 상태 변경 테스트")
    void multipleStatusChanges() {
        // given
        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .user(user)
                .warehouse(warehouse)
                .orderStatus(OrderStatus.DRAFT_AUTO)
                .orderType(Purchase.OrderType.MANUAL)
                .build();

        // when & then
        purchase.updateOrderStatus(OrderStatus.SUBMITTED);
        assertThat(purchase.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);

        purchase.updateOrderStatus(OrderStatus.CONFIRMED);
        assertThat(purchase.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

        purchase.updateOrderStatus(OrderStatus.REJECTED);
        assertThat(purchase.getOrderStatus()).isEqualTo(OrderStatus.REJECTED);
    }
}
