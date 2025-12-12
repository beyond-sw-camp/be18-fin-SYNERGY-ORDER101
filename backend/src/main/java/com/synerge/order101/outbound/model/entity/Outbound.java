package com.synerge.order101.outbound.model.entity;


import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbound")
public class Outbound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outboundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Store store;

    @Column(nullable = false)
    private String outboundNo;

    @Column(name = "outbound_datetime", columnDefinition = "DATETIME(6)")
    @CreationTimestamp
    private LocalDateTime outboundDatetime;

    private String createdBy;

    public static Outbound create(Warehouse warehouse, Store store, String outboundNo, String createdBy) {
        Outbound outbound = new Outbound();
        outbound.warehouse = warehouse;
        outbound.store = store;
        outbound.outboundNo = outboundNo;
        outbound.createdBy = createdBy;
        return outbound;
    }

}
