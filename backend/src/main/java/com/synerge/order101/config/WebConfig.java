package com.synerge.order101.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-root:uploads}")  // UPLOAD_ROOT랑 같은 값
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // saveProductImage에서 쓰는 실제 저장 경로와 100% 동일해야 함
        String uploadPath = Paths.get(System.getProperty("user.dir"),
                uploadRoot,
                "product-images").toUri().toString();

        registry.addResourceHandler("/product-images/**")
                .addResourceLocations(uploadPath); // file:///....../uploads/product-images/
    }
}
