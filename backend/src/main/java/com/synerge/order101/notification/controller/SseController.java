package com.synerge.order101.notification.controller;

import com.synerge.order101.auth.jwt.JwtTokenProvider;
import com.synerge.order101.auth.model.service.CustomUserDetails;
import com.synerge.order101.notification.model.service.NotificationSseService;
import com.synerge.order101.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.attribute.UserPrincipal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sse")
public class SseController {
    private final NotificationSseService sseService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) String token,
                                @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        Authentication jwtAuth = jwtTokenProvider.createAuthentication(token);
        Object principal = jwtAuth.getPrincipal();
        Long userId;

        if (principal instanceof User user) {
            userId = user.getUserId();
        } else if (principal instanceof CustomUserDetails cud) {
            userId = cud.getUser().getUserId();
        } else {
            throw new IllegalStateException(
                    "Unknown principal type: " + principal.getClass().getName());
        }

        return sseService.subscribe(String.valueOf(userId), lastEventId);
    }
}
