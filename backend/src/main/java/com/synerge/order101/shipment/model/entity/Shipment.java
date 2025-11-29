package com.synerge.order101.shipment.model.entity;

import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.outbound.model.entity.Outbound;
import com.synerge.order101.store.model.entity.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "shipment")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long shipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Outbound outbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private StoreOrder storeOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = true)
    private ShipmentStatus shipmentStatus;

    @Column(name="inventory_applied", nullable = false)
    private Boolean inventoryApplied = false;

    @Column(name = "in_transit_applied", nullable = false)
    private Boolean inTransitApplied = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, insertable = true)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    public void markInventoryApplied() {
        this.inventoryApplied = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void markInTransitApplied() {
        this.inTransitApplied = true;
        this.updatedAt = LocalDateTime.now();
    }


}
