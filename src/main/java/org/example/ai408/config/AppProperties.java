package org.example.ai408.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ai408")
public record AppProperties(
        Files files,
        Auth auth,
        Jwt jwt,
        Ai ai,
        Cos cos,
        Admin admin,
        Cors cors
) {
    public record Files(String storageDir, String templateDir) {}

    public record Auth(String fixedCode, int codeExpireSeconds) {}

    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays) {}

    public record Ai(String baseUrl, String apiKey, String model, String visionModel, String systemPrompt, boolean mockEnabled) {}

    public record Cos(
            boolean enable,
            String region,
            String secretId,
            String secretKey,
            String bucketName,
            String domain,
            String pathPrefix,
            long signExpireSeconds,
            boolean publicRead
    ) {}

    public record Admin(String seedMobile, String seedNickname) {}

    public record Cors(List<String> allowedOrigins) {}
}
