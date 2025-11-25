package com.synerge.order101.notification.model;

public enum NotificationType {
    // 본사 직원
    STORE_ORDER_APPROVAL_REQUEST,
    AUTO_PURCHASE_BATCH_SUMMARY,
    AUTO_PURCHASE_CREATED,
    AUTO_SMART_ORDER_CREATED,

    // 본사 관리자
    PURCHASE_APPROVAL_REQUEST,

    // 가맹점주
    STORE_ORDER_APPROVED,
    STORE_ORDER_REJECTED
}
