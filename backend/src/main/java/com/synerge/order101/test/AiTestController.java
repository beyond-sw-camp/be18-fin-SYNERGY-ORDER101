package com.synerge.order101.test;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequiredArgsConstructor
public class AiTestController {

    private final WebClient webClient;

    @GetMapping("/test/python-health")
    public Object testPython() {
        return webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }
}