package com.synerge.order101.inbound.model.entity;


import com.synerge.order101.product.model.entity.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inbound_detail")
public class InboundDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbound_id", nullable = false)
    private Inbound inbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Product product;

    @Column(nullable = false)
    private Integer receivedQty;

    @Column(columnDefinition = "DATETIME(6)")
    @CreationTimestamp
    private LocalDateTime createdAt;

}
