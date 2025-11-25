package com.synerge.order101.notification.model.entity;

import com.synerge.order101.notification.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private String title;
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    private Long userId;
    private Long storeId;

    private Long storeOrderId;
    private String orderNo;

    private Long purchaseOrderId;
    private String poNo;
    private String orderType;

    private Long supplierId;
    private Long smartOrderId;
    private String orderStatus;

    public void markRead() {
        this.readAt = LocalDateTime.now();
    }
}
