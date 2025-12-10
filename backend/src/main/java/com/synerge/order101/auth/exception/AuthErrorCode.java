package com.synerge.order101.auth.exception;

import com.synerge.order101.common.exception.errorcode.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "잘못된 비밀번호입니다."),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS", "잘못된 인증 정보입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "계정이 비활성화 되어 있습니다."),
    TOKEN_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "TOKEN_NOT_PROVIDED", "토큰이 제공되지 않았습니다."),
    UNAUTHORIZED_TOKEN(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_TOKEN","허가되지 않은 토큰."),

    // 토큰 관련 상세 에러
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "MALFORMED_TOKEN", "손상되었거나 잘못된 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),

    // 리프레시 토큰 관련
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status,String code, String message) {
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
