package com.synerge.order101.settlement.model.entity;
import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.supplier.model.entity.Supplier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "settlement")
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @ManyToOne
    @JoinColumn(name = "supplier_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "store_id",  foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Store store;

    @OneToOne
    @JoinColumn(name = "purchase_id",  foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Purchase purchase;

    @OneToOne
    @JoinColumn(name = "store_order_id",  foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private StoreOrder storeOrder;

    @Column()
    private String settlementNo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SettlementType settlementType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;

    @Column
    private String note;

    // 정산 완료 날짜
    @Column
    private LocalDateTime settledDate;

    // 정산 물품 수량
    @Column
    private Integer productsQty;

    // 정산에 필요한 금액
    @Column(nullable = false)
    private BigDecimal productsAmount;

    // 정산 요청 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.settlementNo = generateSettlementNo();
        this.settlementStatus = SettlementStatus.DRAFT;
    }

    public enum VendorType {
        STORE,
        SUPPLIER
    }

    public String generateSettlementNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "SETL-" + datePart + randomNum;
    }



    public enum SettlementStatus {
        DRAFT, //초안
        ISSUED, //발행됌
        VOID, //확정
    }


}
