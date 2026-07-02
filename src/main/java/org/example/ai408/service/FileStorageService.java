package org.example.ai408.service;

import org.example.ai408.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private final AppProperties appProperties;
    private Path storageDir;
    private Path templateDir;

    public FileStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        storageDir = Path.of(appProperties.files().storageDir()).toAbsolutePath().normalize();
        templateDir = Path.of(appProperties.files().templateDir()).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
        Files.createDirectories(templateDir);
    }

    public Path storageDir() {
        return storageDir;
    }

    public Path templateDir() {
        return templateDir;
    }

    public Path resolveStorage(String relative) {
        return storageDir.resolve(relative).normalize();
    }

    public Path resolveTemplate(String relative) {
        return templateDir.resolve(relative).normalize();
    }

    public String storeUpload(MultipartFile file, String folder, String fileName) {
        try {
            Path dir = storageDir.resolve(folder).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/files/" + storageDir.relativize(target).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new IllegalStateException("store file failed", e);
        }
    }

    public String storeBytes(byte[] bytes, String folder, String fileName) {
        try {
            Path dir = storageDir.resolve(folder).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            Files.write(target, bytes);
            return "/files/" + storageDir.relativize(target).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new IllegalStateException("store file failed", e);
        }
    }

    public String storePath(Path file) {
        return "/files/" + storageDir.relativize(file).toString().replace("\\", "/");
    }
}
