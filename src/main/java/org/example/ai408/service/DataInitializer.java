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
        if (!appProperties.admin().seedEnabled()) {
            return;
        }
        String seedEmail = appProperties.admin().seedEmail() == null ? "" : appProperties.admin().seedEmail().trim().toLowerCase();
        if ((!seedEmail.isBlank()
                ? userRepository.findByEmail(seedEmail)
                : userRepository.findByMobile(appProperties.admin().seedMobile())).isPresent()) {
            return;
        }
        UserEntity user = new UserEntity();
        user.setId(IdGenerator.prefixed("u"));
        user.setMobile(seedEmail.isBlank() ? appProperties.admin().seedMobile() : null);
        user.setEmail(seedEmail.isBlank() ? null : seedEmail);
        user.setNickname(appProperties.admin().seedNickname());
        user.setAvatarUrl("");
        user.setRole("admin");
        user.setWrongBookAutoRemoveEnabled(false);
        user.setWrongBookAutoRemoveThreshold(1);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void seedTemplate() throws IOException {
        Path template = fileStorageService.templateDir().resolve("ai408-question-template.xlsx");
        TemplateGenerator.writeTemplate(template);
    }

}
