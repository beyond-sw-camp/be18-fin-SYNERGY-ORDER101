package com.synerge.order101.purchase.model.entity;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.query.Order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long purchaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Warehouse warehouse;

    @Column
    private String poNo;

    @Column
    private LocalDateTime poDate;

    @Column
    private LocalDateTime createdAt;

    @Column
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @OneToMany(mappedBy = "purchase")
    @Builder.Default
    private List<PurchaseDetail> purchaseDetails = new ArrayList<>();


    public enum OrderType {
        MANUAL,
        AUTO,
        SMART
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
                createdAt = now;
                poDate = now;
        }
        poNo = this.generatePoNo();
    }

    public String generatePoNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PO" + datePart + randomNum;
    }

    public void updateOrderStatus(OrderStatus newOrderStatus) {
        this.orderStatus = newOrderStatus;
    }

    public void submit(User submitUser, LocalDateTime time) {
        this.user = submitUser;
        this.poDate = time;
        this.orderStatus = OrderStatus.SUBMITTED;
    }
}
