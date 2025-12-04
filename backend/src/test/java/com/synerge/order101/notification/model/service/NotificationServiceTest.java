package com.synerge.order101.notification.model.service;

import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.entity.Notification;
import com.synerge.order101.notification.model.repository.NotificationRepository;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("NotificationServiceTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private UserRepository userRepository;

    // ---------- getNotifications ----------

    @Test
    @Order(1)
    void getNotifications_success() {
        // Given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        int page = 0;
        int size = 20;

        Notification n = mock(Notification.class);
        Page<Notification> p = new PageImpl<>(
                List.of(n),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(p);

        // When
        ItemsResponseDto<Notification> res =
                notificationService.getNotifications(user, page, size);

        // Then
        assertThat(res).isNotNull();
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getPage()).isEqualTo(page);
        assertThat(res.getTotalCount()).isEqualTo(1);

        verify(notificationRepository, times(1))
                .findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    @Order(2)
    void getNotifications_userNull_throwsAccessDenied() {
        // Given
        User user = null;

        // When & Then
        assertThatThrownBy(() ->
                notificationService.getNotifications(user, 0, 20)
        ).isInstanceOf(CustomException.class);
        // 에러코드까지 보고 싶으면 CustomException 에 getErrorCode() 있으면 꺼내서 검증해도 좋음.
    }

    @Test
    @Order(3)
    void getUnreadCount_success() {
        // Given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(notificationRepository.countByUserIdAndReadAtIsNull(1L)).willReturn(3);

        // When
        int unread = notificationService.getUnreadCount(user);

        // Then
        assertThat(unread).isEqualTo(3);
        verify(notificationRepository, times(1))
                .countByUserIdAndReadAtIsNull(1L);
    }

    @Test
    @Order(4)
    void getUnreadCount_userNull_throwsAccessDenied() {
        // When & Then
        assertThatThrownBy(() ->
                notificationService.getUnreadCount(null)
        ).isInstanceOf(CustomException.class);
    }

    @Test
    @Order(5)
    void readAll_success() {
        // Given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        given(notificationRepository.markAllReadByUserId(eq(1L), any(LocalDateTime.class)))
                .willReturn(5);

        // When
        int updatedCount = notificationService.readAll(user);

        // Then
        assertThat(updatedCount).isEqualTo(5);
        verify(notificationRepository, times(1))
                .markAllReadByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @Order(6)
    void readAll_userNull_throwsAccessDenied() {
        // When & Then
        assertThatThrownBy(() ->
                notificationService.readAll(null)
        ).isInstanceOf(CustomException.class);
    }

    @Test
    @Order(7)
    void deleteNotification_success() {
        // Given
        int notificationId = 10;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        Notification notification = mock(Notification.class);
        given(notificationRepository.findById(notificationId))
                .willReturn(Optional.of(notification));
        given(notification.getUserId()).willReturn(1L);

        // When
        notificationService.deleteNotification(notificationId, user);

        // Then
        verify(notificationRepository, times(1)).delete(notification);
    }

    @Test
    @Order(8)
    void deleteNotification_notFound_throwsException() {
        // Given
        int notificationId = 999;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        given(notificationRepository.findById(notificationId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                notificationService.deleteNotification(notificationId, user)
        ).isInstanceOf(CustomException.class);

        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    @Order(9)
    void deleteNotification_otherUsersNotification_throwsForbidden() {
        // Given
        int notificationId = 10;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        Notification notification = mock(Notification.class);
        given(notificationRepository.findById(notificationId))
                .willReturn(Optional.of(notification));
        // 다른 유저의 알림
        given(notification.getUserId()).willReturn(2L);

        // When & Then
        assertThatThrownBy(() ->
                notificationService.deleteNotification(notificationId, user)
        ).isInstanceOf(CustomException.class);

        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    @Order(10)
    void deleteNotification_userNull_throwsAccessDenied() {
        assertThatThrownBy(() ->
                notificationService.deleteNotification(1, null)
        ).isInstanceOf(CustomException.class);
    }

    @Test
    @Order(11)
    void deleteAllNotification_success() {
        // Given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);

        Notification n1 = mock(Notification.class);
        Notification n2 = mock(Notification.class);
        List<Notification> list = List.of(n1, n2);

        given(notificationRepository.findAllByUserId(1L))
                .willReturn(list);

        // When
        notificationService.deleteAllNotification(user);

        // Then
        verify(notificationRepository, times(1))
                .findAllByUserId(1L);
        verify(notificationRepository, times(1))
                .deleteAll(list);
    }

    @Test
    @Order(12)
    void deleteAllNotification_userNull_throwsAccessDenied() {
        assertThatThrownBy(() ->
                notificationService.deleteAllNotification(null)
        ).isInstanceOf(CustomException.class);
    }
}
