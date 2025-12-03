package com.synerge.order101;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Order101Application {

	public static void main(String[] args) {
		SpringApplication.run(Order101Application.class, args);
	}

}
