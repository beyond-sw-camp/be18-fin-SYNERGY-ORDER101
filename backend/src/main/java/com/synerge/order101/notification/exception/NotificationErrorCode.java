package com.synerge.order101.notification.exception;

import com.synerge.order101.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"),
    FORBIDDEN_NOTIFICATION_DELETE(HttpStatus.FORBIDDEN, "FORBIDDEN_NOTIFICATION_DELETE", "Forbidden");





    private final HttpStatus status;
    private final String code;
    private final String message;

    NotificationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
