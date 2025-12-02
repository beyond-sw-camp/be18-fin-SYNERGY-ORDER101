package com.synerge.order101.warehouse.model.entity;


import com.synerge.order101.product.model.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "warehouse_inventory")
public class WarehouseInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder.Default
    @Column(name = "on_hand_qty", nullable = false)
    private Integer onHandQuantity = 0;

    @Builder.Default
    @Column(name = "safety_qty", nullable = false)
    private Integer safetyQuantity = 0;

    @Column(columnDefinition = "DATETIME(6)")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void decrease(int qty) {
        if (onHandQuantity < qty) {
            throw new IllegalStateException("재고 부족: 요청 수량=" + qty + ", 보유 수량=" + onHandQuantity);
        }
        this.onHandQuantity -= qty;
    }

    public void increase(int qty) {
        this.onHandQuantity += qty;
    }

    public void updateSafetyQty(int safety) {
        this.safetyQuantity = safety;
    }
}
