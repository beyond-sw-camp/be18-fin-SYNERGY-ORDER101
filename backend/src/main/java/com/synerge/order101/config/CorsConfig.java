package com.synerge.order101.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/sse/**")
                .allowedOrigins("https://order101.link")
                .allowedMethods("GET")
                .allowCredentials(false)       // 토큰을 query로 주니까 false여도 됨
                .maxAge(3600);

        // 필요하면 일반 REST용 CORS도 여기 같이 추가
    }
}
