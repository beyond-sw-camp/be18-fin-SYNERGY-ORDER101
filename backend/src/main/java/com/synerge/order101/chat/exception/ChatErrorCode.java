package com.synerge.order101.chat.exception;

import com.synerge.order101.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements ErrorCode {
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized"),
    NOT_CHAT_MEMBER(HttpStatus.FORBIDDEN, "FORBIDDEN", "채팅방 멤버가 아닙니다."),
    NOT_ASSIGNED_PARTNER(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "담당된 가맹점주(HQ)가 존재하지 않습니다.");





    private final HttpStatus status;
    private final String code;
    private final String message;

    ChatErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
