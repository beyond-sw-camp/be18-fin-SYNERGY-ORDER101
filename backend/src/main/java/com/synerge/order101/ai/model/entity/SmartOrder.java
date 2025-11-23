package com.synerge.order101.ai.model.entity;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(
        name = "smart_order",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_smart_order_forecast",
                        columnNames = {"demand_forecast_id"}
                )
        },
        indexes = {
                @Index(name = "idx_so_product_week", columnList = "product_id, target_week")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SmartOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "smart_order_id")
    private Long smartOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_forecast_id", nullable = false)
    private DemandForecast demandForecast;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "target_week", nullable = false)
    private LocalDate targetWeek;

    @Column(name = "recommended_order_qty", nullable = false)
    private int recommendedOrderQty;

    @Column(name = "forecast_qty", nullable = false)
    private int forecastQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "smart_order_status", nullable = false)
    private OrderStatus smartOrderStatus = OrderStatus.DRAFT_AUTO;

    @CreationTimestamp
    @Column(name = "snapshot_at", updatable = false)
    private LocalDateTime snapshotAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    public void updateRecommendedQty(Integer newRecommendedQty){
        if(this.smartOrderStatus != OrderStatus.DRAFT_AUTO){
            throw new CustomException(AiErrorCode.SMART_ORDER_UPDATE_FAILED);
        }
        this.recommendedOrderQty = newRecommendedQty;
    }

    public void submit(User submitter) {
        if (this.smartOrderStatus != OrderStatus.DRAFT_AUTO) {
            throw new CustomException(AiErrorCode.SMART_ORDER_SUBMIT_FAILED);
        }
        this.user = submitter;
        this.smartOrderStatus = OrderStatus.SUBMITTED;
        this.snapshotAt = LocalDateTime.now();
    }

    public void setSystemUserIfNull(User systemUser) {
        if (this.user == null) {
            this.user = systemUser;
        }
    }




}
