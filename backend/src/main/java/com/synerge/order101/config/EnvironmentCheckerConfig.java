package com.synerge.order101.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class EnvironmentCheckerConfig {

    @Bean
    public ApplicationRunner environmentChecker(Environment env) {
        return args -> {
            log.info("--- ⚙️ Environment Variable Check Start ------------------");

            // 1. DB 호스트 확인 (Secret 아님)
            log.info("DB_HOST: {}", env.getProperty("spring.datasource.url"));

            // 2. 레디스 호스트 확인 (Secret 아님)
            log.info("REDIS_HOST: {}", env.getProperty("spring.data.redis.host"));

            // 3. JWT Secret 존재 여부 확인 (민감 정보는 출력하지 않음)
            boolean jwtSecretPresent = env.containsProperty("jwt.secret");
            log.info("JWT_SECRET 존재 여부: {}", jwtSecretPresent);

            log.info("--- ⚙️ Environment Variable Check End --------------------");
        };
    }
}