// src/main/java/com/synerge/order101/data/DataGeneratorRunner.java
package com.synerge.order101.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.data.generate.enabled", havingValue = "true")
public class DataGeneratorRunner implements CommandLineRunner {

//    @Autowired
//    private DataGeneratorService dataGeneratorService;

    @Value("${app.data.generate.items:}") // ex: productSupplier,order
    private String items;

    @Value("${app.data.generate.count:100}")
    private int count;

    @Override
    public void run(String... args) throws Exception {
        Set<String> set = Arrays.stream(items.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

//        if (set.contains("productsupplier") || set.contains("productSupplier".toLowerCase()) || set.contains("productsupplier")) {
//            dataGeneratorService.generateProductSupplierData(count);
//        }
//
//        if (set.contains("order")) {
//            dataGeneratorService.generateOrderData(count);
//        }
//
//        if(set.contains("purchase")){
//            dataGeneratorService.generatePurchaseData(count);
//        }
    }
}