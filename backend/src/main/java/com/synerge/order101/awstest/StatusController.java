package com.synerge.order101.awstest;// 예시: StatusController.java 파일 추가
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/status")
    public String checkStatus() {
        return "UP"; // 단순히 문자열만 반환
    }
}