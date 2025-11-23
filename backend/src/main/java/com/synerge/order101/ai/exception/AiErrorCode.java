package com.synerge.order101.ai.exception;

import com.synerge.order101.common.exception.errorcode.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AiErrorCode implements ErrorCode {
    FORECAST_NOT_FOUND(HttpStatus.NOT_FOUND,"FORECAST_NOT_FOUND", "수요 예측 정보를 찾을 수 없습니다."),
    SMART_ORDER_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMART_ORDER_UPDATE_FAILED", "DRAFT 상태에서만 수정 가능합니다."),
    SMART_ORDER_SUBMIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMART_ORDER_SUBMIT_FAILED", "DRAFT 상태에서만 상신 가능합니다."),
    SMART_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "SMART_ORDER_NOT_FOUND", "스마트 발주가 존재하지 않습니다"),
    AI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AI_SERVER_ERROR", "AI 서버 호출 중 오류가 발생했습니다."),
    SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND", "공급사를 찾을 수 없습니다."),
    SUPPLIER_MAPPING_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPLIER_MAPPING_NOT_FOUND", "공급사 매핑을 찾을 수 없습니다."),
    SMART_ORDER_ALREADY_EXISTS(HttpStatus.CONFLICT,"SMART_ORDER_ALREADY_EXISTS","해당 주차에 이미 스마트 발주가 존재합니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;


    AiErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }


    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
