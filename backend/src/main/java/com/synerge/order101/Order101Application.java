package com.synerge.order101;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Order101Application {

	public static void main(String[] args) {
// 1. DB_HOST와 DB_PORT 환경 변수 확인 (URL 결합을 위한 요소)
        String dbHostCheck = System.getenv("DB_HOST");
        String dbPortCheck = System.getenv("DB_PORT");

        // 2. DB URL 결합 확인
        String dbUrl = String.format("jdbc:mariadb://%s:%s/order101",
                dbHostCheck != null ? dbHostCheck : "NULL_HOST",
                dbPortCheck != null ? dbPortCheck : "NULL_PORT");
        System.out.println(">>> 🎯 [OS Check] Final DB URL: " + dbUrl);

        // 3. DB_PASSWORD 환경 변수 확인 (길이만)
        String dbPasswordCheck = System.getenv("DB_PASSWORD");
        System.out.println(">>> 🎯 [OS Check] DB_PASSWORD Length: " +
                (dbPasswordCheck != null ? dbPasswordCheck : "NULL"));

        SpringApplication.run(Order101Application.class, args);
	}

}
