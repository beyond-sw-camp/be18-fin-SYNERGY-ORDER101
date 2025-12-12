package com.synerge.order101.chat.controller;

import com.synerge.order101.chat.model.dto.ChatDto;
import com.synerge.order101.chat.model.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/{chatRoomId}")
    public void sendMessage(@DestinationVariable int chatRoomId, ChatDto message){

        System.out.println(message.getMessage());
        chatService.saveMessage(chatRoomId, message);
        messageTemplate.convertAndSend("/sub/chat/"+chatRoomId, message);
    }
}


