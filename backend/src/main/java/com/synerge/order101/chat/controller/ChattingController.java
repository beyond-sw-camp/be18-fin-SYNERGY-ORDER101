package com.synerge.order101.chat.controller;

import com.synerge.order101.chat.model.dto.ChatDto;
import com.synerge.order101.chat.model.service.ChatService;
import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/chatrooms")
@RequiredArgsConstructor
public class ChattingController {
    private final ChatService chatService;

    // 개인 채팅방 개설 또는 기존 roomId return
    @PostMapping("/private/create")
    public ResponseEntity<BaseResponseDto<Long>> createOrGetPrivateRoom(@RequestParam String otherNickname, @AuthenticationPrincipal User user) {
        long roomId = chatService.getOrCreatePrivateRoom(otherNickname, user);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, roomId));
    }

    // 채팅방 들어갈때 db에 쌓여있는 메세지들 불러오기
    @GetMapping("/history/{roomId}")
    public ResponseEntity<BaseResponseDto<ChatDto>> getChatHistory(@PathVariable long roomId, @AuthenticationPrincipal User user) {
        List<ChatDto> chatDtos = chatService.getChatHistory(roomId, user);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, chatDtos));
    }

    // 채팅메시지 읽음처리
    @PostMapping("/{roomId}/read")
    public ResponseEntity<BaseResponseDto<String>> messageRead(@PathVariable long roomId, @AuthenticationPrincipal User user) {
        chatService.messageRead(roomId, user);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, "읽음 처리에 성공하였습니다."));
    }


    @GetMapping("/{roomId}/exists")
    public ResponseEntity<BaseResponseDto<String>> existsRoom(@PathVariable long roomId, @AuthenticationPrincipal User user) {
        chatService.assertAccessible(roomId, user);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, "채팅방이 존재합니다."));
    }
}
