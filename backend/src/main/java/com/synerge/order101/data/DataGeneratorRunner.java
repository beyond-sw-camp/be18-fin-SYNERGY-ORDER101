package com.synerge.order101.data;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data.generate.enabled", havingValue = "true")
public class DataGeneratorRunner implements CommandLineRunner {

    private final TradeDataGenerator tradeDataGenerator;

    @Value("${app.data.generate.items:trade}")
    private String items;

    @Value("${app.data.generate.count:100}")
    private int count;

    @Override
    public void run(String... args) throws Exception {
        Set<String> targets = Arrays.stream(items.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        System.out.println("[DataGenerator] 데이터 생성을 시작합니다. (Target: " + items + ")");

        // 1. 발주 (HQ -> Supplier) 데이터 생성
        if (targets.contains("trade") || targets.contains("purchase")) {
            tradeDataGenerator.generatePurchases(count);
        }

        // 2. 주문 (store -> HQ) 데이터 생성
        if (targets.contains("trade") || targets.contains("order")) {
            tradeDataGenerator.generateStoreOrders(count);
        }

        System.out.println("[DataGenerator] 모든 데이터 생성이 완료되었습니다.");
    }
}