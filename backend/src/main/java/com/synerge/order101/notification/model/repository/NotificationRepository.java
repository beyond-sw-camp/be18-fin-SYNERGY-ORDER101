package com.synerge.order101.notification.model.repository;

import com.synerge.order101.notification.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(int userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);

    int countByUserIdAndReadAtIsNull(long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :uid and n.readAt is null")
    int markAllReadByUserId(@Param("uid") Long uid, @Param("now") LocalDateTime now);
}
