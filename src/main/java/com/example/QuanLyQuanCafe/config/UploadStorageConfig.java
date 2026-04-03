package com.example.QuanLyQuanCafe.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UploadStorageConfig {

    public static final String UPLOAD_STORAGE_ROOT_BEAN = "uploadStorageRoot";

    @Bean(name = UPLOAD_STORAGE_ROOT_BEAN)
    public Path uploadStorageRoot(
            @Value("${app.upload.base-path:src/main/resources/static/uploads}") String base) {
        try {
            Path root = Paths.get(base).toAbsolutePath().normalize();
            Files.createDirectories(root.resolve("avatars"));
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}