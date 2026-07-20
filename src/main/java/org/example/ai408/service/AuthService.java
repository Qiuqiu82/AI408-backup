package org.example.ai408.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.LoginCodeEntity;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.repository.LoginCodeRepository;
import org.example.ai408.repository.UserRepository;
import org.example.ai408.security.JwtService;
import org.example.ai408.security.JwtType;
import org.example.ai408.util.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final String LOGIN_SCENE = "login";
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthRateLimitService authRateLimitService;

    public AuthService(
            AppProperties appProperties,
            UserRepository userRepository,
            LoginCodeRepository loginCodeRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuthRateLimitService authRateLimitService
    ) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authRateLimitService = authRateLimitService;
    }

    @Transactional
    public AuthDtos.SendCodeResponse sendCode(String emailOrMobile, String scene, String requestIp) {
        String normalizedScene = normalizeScene(scene);
        String email = normalizeEmail(emailOrMobile);
        requireEmail(email);
        requireInvited(email);

        LocalDateTime now = LocalDateTime.now();
        authRateLimitService.checkSendCodeLimit(email, normalizedScene, requestIp, now);
        loginCodeRepository.findTopByEmailAndSceneAndUsedFalseOrderByCreatedAtDesc(email, normalizedScene)
                .ifPresent(existing -> {
                    if (existing.getLastSentAt() != null
                            && Duration.between(existing.getLastSentAt(), now).getSeconds() < SEND_INTERVAL_SECONDS) {
                        throw new BusinessException(ErrorCode.CODE_RATE_LIMIT);
                    }
                });

        String code = generateCode();
        LoginCodeEntity entity = new LoginCodeEntity();
        entity.setId(IdGenerator.prefixed("lc"));
        entity.setEmail(email);
        entity.setCodeHash(hashCode(email, code));
        entity.setScene(normalizedScene);
        entity.setExpiresAt(now.plusSeconds(resolveCodeExpireSeconds()));
        entity.setLastSentAt(now);
        entity.setAttemptCount(0);
        entity.setUsed(false);
        entity.setRequestIp(authRateLimitService.normalizeIp(requestIp));
        loginCodeRepository.save(entity);

        sendLoginCodeEmail(email, code);
        return new AuthDtos.SendCodeResponse(resolveCodeExpireSeconds());
    }

    @Transactional
    public AuthDtos.AuthTokens login(String emailOrMobile, String code, String deviceId, String clientType) {
        String email = normalizeEmail(emailOrMobile);
        requireEmail(email);
        requireInvited(email);

        LoginCodeEntity loginCode = loginCodeRepository.findTopByEmailAndSceneAndUsedFalseOrderByCreatedAtDesc(email, LOGIN_SCENE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_INVALID));
        LocalDateTime now = LocalDateTime.now();
        if (loginCode.getExpiresAt() == null || loginCode.getExpiresAt().isBefore(now)) {
            loginCode.setUsed(true);
            loginCodeRepository.save(loginCode);
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        if (loginCode.getAttemptCount() != null && loginCode.getAttemptCount() >= MAX_ATTEMPTS) {
            loginCode.setUsed(true);
            loginCodeRepository.save(loginCode);
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        if (code == null || !hashCode(email, code.trim()).equals(loginCode.getCodeHash())) {
            loginCode.setAttemptCount(valueOrZero(loginCode.getAttemptCount()) + 1);
            if (loginCode.getAttemptCount() >= MAX_ATTEMPTS) {
                loginCode.setUsed(true);
            }
            loginCodeRepository.save(loginCode);
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }

        loginCode.setUsed(true);
        loginCode.setUsedAt(now);
        loginCode.setDeviceId(deviceId);
        loginCode.setClientType(clientType);
        loginCodeRepository.save(loginCode);

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email));
        user.setLastLoginAt(now);
        return issueAndPersistTokens(userRepository.save(user), "code");
    }

    @Transactional
    public AuthDtos.AuthTokens passwordLogin(String emailValue, String password, String deviceId, String clientType, String requestIp) {
        String email = normalizeEmail(emailValue);
        requireEmail(email);
        requireInvited(email);
        LocalDateTime now = LocalDateTime.now();
        authRateLimitService.checkPasswordLoginLimit(email, requestIp, now);

        UserEntity user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null || isBlank(user.getPasswordHash()) || password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            authRateLimitService.recordPasswordLoginFailure(email, requestIp, now);
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "邮箱或密码错误");
        }

        authRateLimitService.clearPasswordLoginFailures(email, requestIp);
        user.setLastLoginAt(now);
        return issueAndPersistTokens(userRepository.save(user), "password");
    }

    @Transactional
    public AuthDtos.AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        var claims = jwtService.parseToken(refreshToken, JwtType.REFRESH);
        UserEntity user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateRefreshToken(user, refreshToken);
        return issueAndPersistTokens(user, claims.get("authMethod", String.class));
    }

    private AuthDtos.AuthTokens issueAndPersistTokens(UserEntity user, String authMethod) {
        String normalizedAuthMethod = isBlank(authMethod) ? "password" : authMethod;
        String accessToken = jwtService.issueAccessToken(user, normalizedAuthMethod);
        String refreshToken = jwtService.issueRefreshToken(user, normalizedAuthMethod);
        user.setRefreshTokenHash(CryptoUtils.sha256(refreshToken));
        user.setRefreshTokenExpiresAt(LocalDateTime.ofInstant(
                Instant.now().plus(appProperties.jwt().refreshTokenDays(), ChronoUnit.DAYS),
                ZoneId.systemDefault()
        ));
        UserEntity saved = userRepository.save(user);
        return new AuthDtos.AuthTokens(
                accessToken,
                refreshToken,
                appProperties.jwt().accessTokenMinutes() * 60,
                Support.toUserDto(saved)
        );
    }

    private void validateRefreshToken(UserEntity user, String refreshToken) {
        if (user.getRefreshTokenHash() == null || !user.getRefreshTokenHash().equals(CryptoUtils.sha256(refreshToken))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (user.getRefreshTokenExpiresAt() == null || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
    }

    private UserEntity createUser(String email) {
        UserEntity user = new UserEntity();
        user.setId(IdGenerator.prefixed("u"));
        user.setEmail(email);
        user.setMobile(null);
        user.setNickname(defaultNickname(email));
        user.setAvatarUrl("");
        user.setRole(isAdminEmail(email) ? "admin" : "student");
        user.setWrongBookAutoRemoveEnabled(false);
        user.setWrongBookAutoRemoveThreshold(1);
        user.setLastLoginAt(LocalDateTime.now());
        return user;
    }

    private void sendLoginCodeEmail(String email, String code) {
        if (appProperties.mail().mockEnabled()) {
            log.warn("AI408 login code mock enabled, email={}, code={}", email, code);
            return;
        }
        if (isBlank(appProperties.mail().host())
                || isBlank(appProperties.mail().username())
                || isBlank(appProperties.mail().password())
                || isBlank(appProperties.mail().from())) {
            throw new BusinessException(ErrorCode.MAIL_CONFIG_INVALID);
        }

        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(appProperties.mail().host());
            sender.setPort(appProperties.mail().port());
            sender.setUsername(appProperties.mail().username());
            sender.setPassword(appProperties.mail().password());
            sender.setDefaultEncoding("UTF-8");
            Properties properties = sender.getJavaMailProperties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");
            if (appProperties.mail().ssl()) {
                properties.put("mail.smtp.ssl.enable", "true");
            } else {
                properties.put("mail.smtp.starttls.enable", "true");
            }

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(appProperties.mail().from());
            helper.setTo(email);
            helper.setSubject("AI408 登录验证码");
            helper.setText("""
                    你的 AI408 登录验证码是：%s

                    验证码 5 分钟内有效，请勿转发给他人。
                    如果不是你本人操作，可以忽略这封邮件。
                    """.formatted(code), false);
            sender.send(message);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "AI408 login code email send failed, host={}, port={}, username={}, from={}, ssl={}",
                    appProperties.mail().host(),
                    appProperties.mail().port(),
                    appProperties.mail().username(),
                    appProperties.mail().from(),
                    appProperties.mail().ssl(),
                    exception
            );
            throw new BusinessException(ErrorCode.MAIL_CONFIG_INVALID, "邮件发送失败，请检查 SMTP 配置");
        }
    }

    private void requireInvited(String email) {
        if (appProperties.invite() == null || !appProperties.invite().only()) {
            return;
        }
        if (isAdminEmail(email)) {
            return;
        }
        List<String> allowedEmails = appProperties.invite().allowedEmails() == null ? List.of() : appProperties.invite().allowedEmails();
        boolean allowed = allowedEmails.stream()
                .map(this::normalizeEmail)
                .anyMatch(email::equals);
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVITE_REQUIRED);
        }
    }

    private boolean isAdminEmail(String email) {
        return !isBlank(appProperties.admin().seedEmail()) && normalizeEmail(appProperties.admin().seedEmail()).equals(email);
    }

    private String defaultNickname(String email) {
        if (isAdminEmail(email)) {
            return appProperties.admin().seedNickname();
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "AI408 学员";
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void requireEmail(String email) {
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.EMAIL_INVALID);
        }
    }

    private String hashCode(String email, String code) {
        return CryptoUtils.sha256(email + ":" + code);
    }

    private String generateCode() {
        String fixedCode = appProperties.auth() == null ? "" : appProperties.auth().fixedCode();
        boolean mockMail = appProperties.mail() != null && appProperties.mail().mockEnabled();
        if (mockMail && fixedCode != null && !fixedCode.isBlank()) {
            return fixedCode.trim();
        }
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private int resolveCodeExpireSeconds() {
        if (appProperties.auth() == null || appProperties.auth().codeExpireSeconds() <= 0) {
            return 300;
        }
        return appProperties.auth().codeExpireSeconds();
    }

    private String normalizeScene(String scene) {
        String normalized = scene == null || scene.isBlank() ? LOGIN_SCENE : scene.trim().toLowerCase(Locale.ROOT);
        if (!LOGIN_SCENE.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "暂不支持该验证码场景");
        }
        return normalized;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
