package com.synerge.order101.notification.model.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotificationSseService {

    private static final long TIMEOUT = 60L * 60 * 1000; // 1h
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Object>> eventCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, AtomicLong> seqMap = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        CopyOnWriteArrayList<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.clear();               // 기존 연결 제거
        list.add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        if(lastEventId != null && !lastEventId.isBlank()) {
            Map<String, Object> cached = eventCache.getOrDefault(userId, Collections.emptyMap());

            cached.entrySet().stream()
                    .filter(e -> e.getKey().compareTo(lastEventId) > 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .id(e.getKey())
                                    .name("notification")
                                    .data(e.getValue()));
                        } catch (IOException ignored) {

                        }
                    });
        }

        // 첫 이벤트 (연결 확인)
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    public void send(String userId, Object payload) {
        String evenId = createEventId(userId);

        eventCache.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(evenId, payload);

        CopyOnWriteArrayList<SseEmitter> list = emitters.getOrDefault(userId, new CopyOnWriteArrayList<>());

        List<SseEmitter> dead = new ArrayList<>();

        for(SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(evenId)
                        .name("notification")
                        .data(payload));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }

        if (!dead.isEmpty()) {
            list.removeAll(dead);
        }

        Map<String, Object> cache = eventCache.get(userId);
        if (cache != null && cache.size() > 500) {
            cache.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .limit(cache.size() - 400)
                    .forEach(e -> cache.remove(e.getKey()));
        }
    }

    private String createEventId(String userId) {
        long now = System.currentTimeMillis();
        long seq = seqMap.computeIfAbsent(userId, k -> new AtomicLong()).incrementAndGet();
        return now + "-" + seq;
    }

    private void removeEmitter(String loginId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(loginId);
        if(list != null) {
            list.remove(emitter);
        }
    }
}
