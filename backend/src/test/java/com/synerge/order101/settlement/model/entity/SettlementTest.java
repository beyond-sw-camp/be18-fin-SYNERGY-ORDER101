package com.synerge.order101.settlement.model.entity;

import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.supplier.model.entity.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementTest")
class SettlementTest {

    @Test
    @DisplayName("Settlement 생성 - 공급사 정산(AP)")
    void createSettlement_SupplierAP_Success() {
        // given
        Supplier supplier = Supplier.builder()
                .supplierId(1L)
                .supplierName("테스트공급사")
                .build();

        Purchase purchase = Purchase.builder()
                .purchaseId(100L)
                .build();

        BigDecimal amount = new BigDecimal("500000");
        Integer qty = 100;

        // when
        Settlement settlement = Settlement.builder()
                .supplier(supplier)
                .purchase(purchase)
                .settlementType(SettlementType.AP)
                .productsAmount(amount)
                .productsQty(qty)
                .build();

        // then
        assertThat(settlement).isNotNull();
        assertThat(settlement.getSupplier()).isEqualTo(supplier);
        assertThat(settlement.getPurchase()).isEqualTo(purchase);
        assertThat(settlement.getSettlementType()).isEqualTo(SettlementType.AP);
        assertThat(settlement.getProductsAmount()).isEqualTo(amount);
        assertThat(settlement.getProductsQty()).isEqualTo(qty);
    }

    @Test
    @DisplayName("Settlement 생성 - 가맹점 정산(AR)")
    void createSettlement_StoreAR_Success() {
        // given
        Store store = Store.builder()
                .storeId(1L)
                .storeName("테스트매장")
                .build();

        StoreOrder storeOrder = StoreOrder.builder()
                .storeOrderId(200L)
                .build();

        BigDecimal amount = new BigDecimal("300000");
        Integer qty = 50;

        // when
        Settlement settlement = Settlement.builder()
                .store(store)
                .storeOrder(storeOrder)
                .settlementType(SettlementType.AR)
                .productsAmount(amount)
                .productsQty(qty)
                .build();

        // then
        assertThat(settlement).isNotNull();
        assertThat(settlement.getStore()).isEqualTo(store);
        assertThat(settlement.getStoreOrder()).isEqualTo(storeOrder);
        assertThat(settlement.getSettlementType()).isEqualTo(SettlementType.AR);
        assertThat(settlement.getProductsAmount()).isEqualTo(amount);
        assertThat(settlement.getProductsQty()).isEqualTo(qty);
    }

    @Test
    @DisplayName("Settlement 생성 - Builder 패턴 검증")
    void createSettlement_WithBuilder_Success() {
        // given & when
        Settlement settlement = Settlement.builder()
                .settlementId(1L)
                .settlementNo("SETL-20231201-1234")
                .settlementType(SettlementType.AP)
                .settlementStatus(Settlement.SettlementStatus.DRAFT)
                .productsAmount(new BigDecimal("1000000"))
                .productsQty(200)
                .note("테스트 정산")
                .createdAt(LocalDateTime.now())
                .build();

        // then
        assertThat(settlement.getSettlementId()).isEqualTo(1L);
        assertThat(settlement.getSettlementNo()).isEqualTo("SETL-20231201-1234");
        assertThat(settlement.getSettlementType()).isEqualTo(SettlementType.AP);
        assertThat(settlement.getSettlementStatus()).isEqualTo(Settlement.SettlementStatus.DRAFT);
        assertThat(settlement.getProductsAmount()).isEqualTo(new BigDecimal("1000000"));
        assertThat(settlement.getProductsQty()).isEqualTo(200);
        assertThat(settlement.getNote()).isEqualTo("테스트 정산");
        assertThat(settlement.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Settlement 정산번호 생성 테스트")
    void generateSettlementNo_Success() {
        // given
        Settlement settlement = Settlement.builder()
                .settlementType(SettlementType.AP)
                .productsAmount(new BigDecimal("100000"))
                .build();

        // when
        String settlementNo = settlement.generateSettlementNo();

        // then
        assertThat(settlementNo).isNotNull();
        assertThat(settlementNo).startsWith("SETL-");
        assertThat(settlementNo).hasSize(17); // SETL-yyyyMMdd#### = 17자
    }

    @Test
    @DisplayName("Settlement SettlementStatus Enum 검증")
    void settlementStatus_EnumValues_Success() {
        // when & then
        assertThat(Settlement.SettlementStatus.DRAFT).isNotNull();
        assertThat(Settlement.SettlementStatus.ISSUED).isNotNull();
        assertThat(Settlement.SettlementStatus.VOID).isNotNull();
        assertThat(Settlement.SettlementStatus.values()).hasSize(3);
    }

    @Test
    @DisplayName("Settlement VendorType Enum 검증")
    void vendorType_EnumValues_Success() {
        // when & then
        assertThat(Settlement.VendorType.STORE).isNotNull();
        assertThat(Settlement.VendorType.SUPPLIER).isNotNull();
        assertThat(Settlement.VendorType.values()).hasSize(2);
    }

    @Test
    @DisplayName("Settlement 최소 필수 필드만으로 생성")
    void createSettlement_MinimalFields_Success() {
        // given & when
        Settlement settlement = Settlement.builder()
                .settlementType(SettlementType.AP)
                .productsAmount(new BigDecimal("50000"))
                .build();

        // then
        assertThat(settlement).isNotNull();
        assertThat(settlement.getSettlementType()).isEqualTo(SettlementType.AP);
        assertThat(settlement.getProductsAmount()).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("Settlement 금액과 수량 검증")
    void settlement_AmountAndQuantity_Validation() {
        // given
        BigDecimal expectedAmount = new BigDecimal("1234567.89");
        Integer expectedQty = 999;

        // when
        Settlement settlement = Settlement.builder()
                .settlementType(SettlementType.AR)
                .productsAmount(expectedAmount)
                .productsQty(expectedQty)
                .build();

        // then
        assertThat(settlement.getProductsAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(settlement.getProductsQty()).isEqualTo(expectedQty);
    }

    @Test
    @DisplayName("Settlement 정산 완료 날짜 설정")
    void settlement_SettledDate_Success() {
        // given
        LocalDateTime settledDate = LocalDateTime.now();

        // when
        Settlement settlement = Settlement.builder()
                .settlementType(SettlementType.AP)
                .productsAmount(new BigDecimal("100000"))
                .settledDate(settledDate)
                .build();

        // then
        assertThat(settlement.getSettledDate()).isEqualTo(settledDate);
    }

    @Test
    @DisplayName("Settlement note 필드 설정")
    void settlement_Note_Success() {
        // given
        String note = "12월 정산 건입니다.";

        // when
        Settlement settlement = Settlement.builder()
                .settlementType(SettlementType.AR)
                .productsAmount(new BigDecimal("200000"))
                .note(note)
                .build();

        // then
        assertThat(settlement.getNote()).isEqualTo(note);
    }
}
