package com.synerge.order101.config.websocket;

import com.synerge.order101.chat.model.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompHandler implements ChannelInterceptor {
    @Value("${jwt.secret}")
    private String secretKey;

    private final ChatService chatService;

    private SecretKey key;

    @PostConstruct
    void init() {
        // 서명에 쓸 SecretKey 생성
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);


        if(StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("connect 요청시 토큰 유효성 검증");
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            String token = bearerToken.substring(7);
            System.out.println(token);
            // 토큰 검증
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            System.out.println("토큰 검증 완료");
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            System.out.println("subscribe 검증");

            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            if (bearerToken == null) {
                bearerToken = accessor.getFirstNativeHeader("authorization"); // 혹시 소문자로 올 수도 있음
            }
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw new AuthenticationServiceException("Authorization 헤더가 없습니다.");
            }

            String token = bearerToken.substring(7);

            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            Object userIdClaim = claims.get("userId");
            long userId;
            if (userIdClaim instanceof Integer i) {
                userId = i.longValue();
            } else if (userIdClaim instanceof Long l) {
                userId = l;
            } else if (userIdClaim instanceof String s) {
                userId = Long.parseLong(s);
            } else {
                throw new AuthenticationServiceException("userId 클레임이 올바르지 않습니다.");
            }

            String roomIdStr = accessor.getDestination().split("/")[3];
            long roomId = Long.parseLong(roomIdStr);
            log.info("userId:{} roomId:{}", userId, roomId);

            if (!chatService.isRoomParticipant(userId, roomId)) {
                throw new AuthenticationServiceException("해당 room에 권한이 없습니다.");
            }
        }
        return message;
    }
}
