package com.synerge.order101.store.exception;

import com.synerge.order101.common.exception.errorcode.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StoreErrorCode implements ErrorCode {
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND","가맹점 없음.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    StoreErrorCode(HttpStatus status,String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
