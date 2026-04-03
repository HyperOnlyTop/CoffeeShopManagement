package com.example.QuanLyQuanCafe.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadStorageRoot;

    public WebConfig(@Qualifier(UploadStorageConfig.UPLOAD_STORAGE_ROOT_BEAN) Path uploadStorageRoot) {
        this.uploadStorageRoot = uploadStorageRoot;
    }

    @Override
    @SuppressWarnings("null")
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadStorageRoot.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}