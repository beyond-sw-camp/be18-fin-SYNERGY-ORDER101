// src/main/java/com/synerge/order101/data/DataGeneratorRunner.java
package com.synerge.order101.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.data.generate.enabled", havingValue = "true")
public class DataGeneratorRunner implements CommandLineRunner {

    @Autowired
    private DataGeneratorService dataGeneratorService;

    @Value("${app.data.generate.count:100}")
    private int count;

    @Override
    public void run(String... args) throws Exception {
        dataGeneratorService.generateProductSupplierData(count);
    }
}