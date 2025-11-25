package com.synerge.order101.notification.controller;

import com.sun.security.auth.UserPrincipal;
import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.notification.model.entity.Notification;
import com.synerge.order101.notification.model.repository.NotificationRepository;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ItemsResponseDto<Notification>> getNotifications(@AuthenticationPrincipal User user ,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        ItemsResponseDto<Notification> notifications = notificationService.getNotifications( user, page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponseDto<Map<String, Integer>>> unreadCount(@AuthenticationPrincipal User user) {

        int count = notificationService.getUnreadCount(user);
        Map<String, Integer> map = Map.of("count", count);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, map));
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<BaseResponseDto<Map<String, Integer>>> markAllRead(@AuthenticationPrincipal User user) {

        int updated = notificationService.readAll(user);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK,Map.of("updated", updated)));
    }

    @DeleteMapping("/{notificationId}")
    @Transactional
    public ResponseEntity<BaseResponseDto<String>> delete(@PathVariable int notificationId, @AuthenticationPrincipal User user) throws AccessDeniedException {

        notificationService.deleteNotification(notificationId, user);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, "삭제가 완료되었습니다."));
    }
}
