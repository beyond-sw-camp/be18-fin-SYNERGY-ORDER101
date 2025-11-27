package com.synerge.order101.order.model.entity;

import com.synerge.order101.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "store_order_status_log")
public class StoreOrderStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_order_status_log_id")
    private Long storeOrderStatusLogId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "store_order_id")
    private StoreOrder storeOrder;

    @Column(name = "updated_at")
    private LocalDateTime updated_at;

    @Column(name = "prev_order_status")
    @Enumerated(EnumType.STRING)
    private OrderStatus prevStatus;

    @Column(name = "cur_order_status")
    @Enumerated(EnumType.STRING)
    private OrderStatus curStatus;

}


