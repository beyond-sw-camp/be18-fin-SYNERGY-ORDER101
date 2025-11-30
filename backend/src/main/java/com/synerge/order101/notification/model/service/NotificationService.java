package com.synerge.order101.notification.model.service;


import com.sun.security.auth.UserPrincipal;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.common.exception.errorcode.CommonErrorCode;
import com.synerge.order101.notification.exception.NotificationErrorCode;
import com.synerge.order101.notification.model.NotificationType;
import com.synerge.order101.notification.model.entity.Notification;
import com.synerge.order101.notification.model.repository.NotificationRepository;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;
    private final UserRepository userRepository;

    public ItemsResponseDto<Notification> getNotifications(User user, int page, int size) {
        System.out.println(user);
        if(user == null) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));

        long userId = user.getUserId();
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> p = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new ItemsResponseDto<>(
                HttpStatus.OK,
                p.getContent(),
                p.getNumber(),
                (int) p.getTotalElements()
        );
    }

    public int getUnreadCount(User user) {
        if(user == null) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }
        long userId = user.getUserId();

        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public int readAll(User user) {
        if(user == null) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }
        long userId = user.getUserId();
        return notificationRepository.markAllReadByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(int notificationId, User user) {
        if(user == null) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }
        long userId = user.getUserId();
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() ->
                new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if(userId!=notification.getUserId()){
            throw new CustomException(NotificationErrorCode.FORBIDDEN_NOTIFICATION_DELETE);

        }
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllNotification(User user) {
        if(user == null) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }
        long userId = user.getUserId();
        List<Notification> notifications = notificationRepository.findAllByUserId(userId);

        notificationRepository.deleteAll(notifications);
    }

    // 발주 컨펌 시 어드민들에게 승인 요청 알림(테스트 안함)
    @Transactional
    public void notifyPurchaseCreatedToAdmin(User admin, Purchase purchase) {
        String orderType;
        if(String.valueOf(purchase.getOrderType()).equals("MANUAL")) {
            orderType = "일반";
        } else if (String.valueOf(purchase.getOrderType()).equals("AUTO")) {
            orderType = "자동";
        } else {
            orderType = "스마트";
        }
        Notification noification = Notification.builder()
                .userId(admin.getUserId())
                .purchaseOrderId(purchase.getPurchaseId())
                .poNo(purchase.getPoNo())
                .orderType(String.valueOf(purchase.getOrderType()))
                .supplierId(purchase.getSupplier().getSupplierId())
                .title("발주 승인 요청!")
                .body("승인 요청이 필요한 " + orderType + "발주서가 있습니다.")
                .type(NotificationType.PURCHASE_APPROVAL_REQUEST)
                .build();

        notificationRepository.save(noification);

        notificationSseService.send(String.valueOf(admin.getUserId()), noification);

    }

    @Transactional
    public void notifyPurchaseCreatedToAllAdmins(Purchase purchase, List<User> admins) {
        for (User admin : admins) {
            notifyPurchaseCreatedToAdmin(admin, purchase);
        }
    }

    // 가맹점 주문 생성 시 알림(테스트 X)
    @Transactional
    public void notifyOrderCreatedToHQ(List<User> hqList, StoreOrder storeOrder) {

        String storeName = storeOrder.getStore().getStoreName();
        Long storeId = storeOrder.getStore().getStoreId();
        Long orderId = storeOrder.getStoreOrderId();

        String title = "가맹점 주문 승인 요청!";
        String body = String.format(
                "승인 요청이 필요한 %s 가맹점 주문이 있습니다. (%s)\n- 주문ID: %d\n- 가맹점ID: %d",
                storeName, storeName, orderId, storeId
        );

        for (User hq : hqList) {
            Notification notification = Notification.builder()
                    .userId(hq.getUserId())
                    .storeId(storeId)
                    .storeOrderId(orderId)
                    .title(title)
                    .body(body)
                    .type(NotificationType.STORE_ORDER_APPROVAL_REQUEST)
                    .orderStatus(storeOrder.getOrderStatus().name())
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            notificationSseService.send(String.valueOf(hq.getUserId()), notification);
        }
    }

    // 가맹점 승인/반려 알림 (테스트 X)
    @Transactional
    public void notifyStoreOrderResult(StoreOrder order) {

        User storeOwner = order.getUser();

        Long userId = storeOwner.getUserId();
        Long orderId = order.getStoreOrderId();
        String orderNo = order.getOrderNo();
        String statusName = order.getOrderStatus().name();

        boolean approved = order.getOrderStatus() == OrderStatus.CONFIRMED;

        String title = "주문 승인 결과";
        String body = String.format(
                "%s 주문건이 %s되었습니다.\n- 주문ID: %d\n- 주문번호: %s\n- 주문상태: %s",
                orderNo,
                approved ? "승인" : "반려",
                orderId,
                orderNo,
                statusName
        );

        Notification notification = Notification.builder()
                .userId(userId)
                .storeId(order.getStore().getStoreId())
                .storeOrderId(orderId)
                .orderNo(orderNo)
                .orderStatus(statusName)
                .title(title)
                .body(body)
                .type(approved ? NotificationType.STORE_ORDER_APPROVED : NotificationType.STORE_ORDER_REJECTED)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        notificationSseService.send(String.valueOf(userId), notification);
    }

    @Transactional
    public void notifyAutoPurchaseCreatedToHqStaff(Purchase purchase, List<User> hqList) {

        Long purchaseId = purchase.getPurchaseId();
        String purchaseNo = purchase.getPoNo();
        String orderType = purchase.getOrderType().name();
        Long supplierId = purchase.getSupplier().getSupplierId();
        String supplierName = purchase.getSupplier().getSupplierName();
        String supplierCode = purchase.getSupplier().getSupplierCode();

        String title = "자동 생성된 자동발주";
        String body = String.format(
                "%s 공급사에 대한 자동발주가 생성되었습니다.\n" +
                        "- 발주번호: %s\n" +
                        "- 발주유형: %s\n" +
                        "- 공급사번호: %s",
                supplierName, purchaseNo, orderType, supplierCode
        );

        for (User hq : hqList) {
            Notification notification = Notification.builder()
                    .userId(hq.getUserId())
                    .supplierId(supplierId)
                    .purchaseOrderId(purchaseId)
                    .poNo(purchase.getPoNo())
                    .orderType(orderType)
                    .title(title)
                    .body(body)
                    .type(NotificationType.AUTO_PURCHASE_CREATED)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            notificationSseService.send(String.valueOf(hq.getUserId()), notification);
        }
    }

    @Transactional
    public void notifySmartOrderCreatedToHq(List<User> hqList, Supplier supplier, List<SmartOrder> smartOrderList) {
        String title = "자동 생성된 스마트 발주";
        String body = String.format(
                "%s 공급사에 대한 스마트발주가 생성되었습니다.\n- 스마트발주 건수: %d\n",
                supplier.getSupplierName(),
                smartOrderList.size()
        );

        for (User hq : hqList) {
            Notification notification = Notification.builder()
                    .userId(hq.getUserId())
                    .supplierId(supplier.getSupplierId())
                    .smartOrderId(smartOrderList.get(0).getSmartOrderId())
                    .title(title)
                    .body(body)
                    .type(NotificationType.AUTO_SMART_ORDER_CREATED)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            notificationSseService.send(String.valueOf(hq.getUserId()), notification);
        }
    }

    @Transactional
    public void notifySmartOrderApprovalToAdmins(SmartOrder smartOrder, List<User> admins) {
        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .userId(admin.getUserId())
                    .smartOrderId(smartOrder.getSmartOrderId())
                    .title("발주 승인 요청!")
                    .body(
                            "승인 요청이 필요한 스마트발주서가 있습니다.\n"
                    )
                    .type(NotificationType.PURCHASE_APPROVAL_REQUEST)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            notificationSseService.send(String.valueOf(admin.getUserId()), notification);
        }
    }


}
