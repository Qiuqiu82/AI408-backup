package org.example.ai408.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.ai408.config.AppProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CosStorageService {
    private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp", "gif");
    private static final DateTimeFormatter KEY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AppProperties appProperties;
    private COSClient cosClient;

    public CosStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        if (!isConfigured()) {
            return;
        }
        COSCredentials credentials = new BasicCOSCredentials(
                appProperties.cos().secretId(),
                appProperties.cos().secretKey()
        );
        ClientConfig clientConfig = new ClientConfig(new Region(appProperties.cos().region()));
        this.cosClient = new COSClient(credentials, clientConfig);
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    public String uploadQuestionImage(Path localFile, String subjectCode, String questionCode) {
        if (!isConfigured() || cosClient == null) {
            throw new IllegalStateException("COS 未配置完成，无法上传题目图片。");
        }
        if (localFile == null || !localFile.isAbsolute() || !Files.exists(localFile) || !Files.isRegularFile(localFile)) {
            throw new IllegalArgumentException("题目图片文件不存在。");
        }

        String extension = fileExtension(localFile.getFileName().toString());
        if (!SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 png/jpg/jpeg/webp/gif 图片。");
        }

        String key = buildObjectKey(subjectCode, questionCode, extension);
        PutObjectRequest request = new PutObjectRequest(appProperties.cos().bucketName(), key, localFile.toFile());
        cosClient.putObject(request);
        return buildPublicUrl(key);
    }

    private boolean isConfigured() {
        return appProperties.cos() != null
                && appProperties.cos().enable()
                && hasText(appProperties.cos().secretId())
                && hasText(appProperties.cos().secretKey())
                && hasText(appProperties.cos().region())
                && hasText(appProperties.cos().bucketName())
                && hasText(appProperties.cos().domain());
    }

    private String buildObjectKey(String subjectCode, String questionCode, String extension) {
        String prefix = hasText(appProperties.cos().pathPrefix()) ? trimSlashes(appProperties.cos().pathPrefix()) : "questions";
        String safeSubject = sanitizeSegment(subjectCode);
        String safeQuestionCode = sanitizeSegment(questionCode);
        String fileName = KEY_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + UUID.randomUUID() + "." + extension;
        return prefix + "/" + safeSubject + "/" + safeQuestionCode + "/" + fileName;
    }

    private String buildPublicUrl(String key) {
        String domain = appProperties.cos().domain();
        String normalizedDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        return normalizedDomain + "/" + key;
    }

    private String sanitizeSegment(String value) {
        if (!hasText(value)) {
            return "unknown";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String trimSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String fileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
