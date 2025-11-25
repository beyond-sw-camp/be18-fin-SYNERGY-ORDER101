package com.synerge.order101.notification.controller;

import com.synerge.order101.auth.jwt.JwtTokenProvider;
import com.synerge.order101.notification.model.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sse")
public class SseController {
    private final NotificationSseService sseService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) String token,
                                @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        String userId = null;

        Authentication jwtAuth = jwtTokenProvider.createAuthentication(token);
        userId = jwtAuth.getName();

        return sseService.subscribe(userId, lastEventId);
    }
}
