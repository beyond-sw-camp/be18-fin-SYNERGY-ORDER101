package com.synerge.order101;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Order101Application {

	public static void main(String[] args) {
        // Spring 초기화 이전에 시스템 환경 변수 직접 출력
        String dbHostCheck = System.getenv("DB_HOST");
        System.out.println(">>> 🎯 [OS Check] DB_HOST (System.getenv): " + dbHostCheck);

        // 민감 정보는 길이만 확인
        String jwtSecretCheck = System.getenv("JWT_SECRET");
        System.out.println(">>> 🎯 [OS Check] JWT_SECRET Length: " +
                (jwtSecretCheck != null ? jwtSecretCheck.length() : "null"));
		SpringApplication.run(Order101Application.class, args);
	}

}
