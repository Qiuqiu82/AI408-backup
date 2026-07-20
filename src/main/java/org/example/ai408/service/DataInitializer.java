package org.example.ai408.service;

import jakarta.annotation.PostConstruct;
import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class DataInitializer {
    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DataInitializer(
            AppProperties appProperties,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostConstruct
    public void ensureFolders() throws IOException {
        Files.createDirectories(fileStorageService.storageDir());
        Files.createDirectories(fileStorageService.templateDir());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() throws IOException {
        seedAdminUser();
        seedTemplate();
    }

    private void seedAdminUser() {
        String seedEmail = appProperties.admin().seedEmail() == null ? "" : appProperties.admin().seedEmail().trim().toLowerCase();
        UserEntity user = (!seedEmail.isBlank()
                ? userRepository.findByEmail(seedEmail)
                : userRepository.findByMobile(appProperties.admin().seedMobile()))
                .orElseGet(() -> {
                    UserEntity entity = new UserEntity();
                    entity.setId(IdGenerator.prefixed("u"));
                    entity.setMobile(seedEmail.isBlank() ? appProperties.admin().seedMobile() : null);
                    entity.setEmail(seedEmail.isBlank() ? null : seedEmail);
                    return entity;
                });
        if (!seedEmail.isBlank()) {
            user.setEmail(seedEmail);
        }
        if (user.getMobile() == null && seedEmail.isBlank()) {
            user.setMobile(appProperties.admin().seedMobile());
        }
        user.setNickname(appProperties.admin().seedNickname());
        user.setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
        user.setRole("admin");
        user.setWrongBookAutoRemoveEnabled(Boolean.TRUE.equals(user.getWrongBookAutoRemoveEnabled()));
        user.setWrongBookAutoRemoveThreshold(user.getWrongBookAutoRemoveThreshold() == null ? 1 : user.getWrongBookAutoRemoveThreshold());
        user.setLastLoginAt(user.getLastLoginAt() == null ? LocalDateTime.now() : user.getLastLoginAt());
        userRepository.save(user);
    }

    private void seedTemplate() throws IOException {
        Path template = fileStorageService.templateDir().resolve("ai408-question-template.xlsx");
        TemplateGenerator.writeTemplate(template);
    }

}
