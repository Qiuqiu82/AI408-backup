package org.example.ai408.service;

import jakarta.transaction.Transactional;
import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.repository.UserRepository;
import org.example.ai408.security.JwtService;
import org.example.ai408.security.JwtType;
import org.example.ai408.util.CryptoUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AppProperties appProperties, UserRepository userRepository, JwtService jwtService) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthDtos.SendCodeResponse sendCode(String mobile) {
        requireMobile(mobile);
        return new AuthDtos.SendCodeResponse(appProperties.auth().codeExpireSeconds());
    }

    @Transactional
    public AuthDtos.AuthTokens login(String mobile, String code, String deviceId, String clientType) {
        requireMobile(mobile);
        if (!appProperties.auth().fixedCode().equals(code)) {
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }

        UserEntity user = userRepository.findByMobile(mobile)
                .orElseGet(() -> createUser(mobile));
        user.setLastLoginAt(LocalDateTime.now());
        return issueAndPersistTokens(userRepository.save(user));
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
        return issueAndPersistTokens(user);
    }

    private AuthDtos.AuthTokens issueAndPersistTokens(UserEntity user) {
        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = jwtService.issueRefreshToken(user);
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

    private UserEntity createUser(String mobile) {
        UserEntity user = new UserEntity();
        user.setId(IdGenerator.prefixed("u"));
        user.setMobile(mobile);
        user.setNickname(mobile.equals(appProperties.admin().seedMobile())
                ? appProperties.admin().seedNickname()
                : "AI408 学员");
        user.setAvatarUrl("");
        user.setRole(mobile.equals(appProperties.admin().seedMobile()) ? "admin" : "student");
        user.setWrongBookAutoRemoveEnabled(false);
        user.setWrongBookAutoRemoveThreshold(1);
        user.setLastLoginAt(LocalDateTime.now());
        return user;
    }

    private void requireMobile(String mobile) {
        if (mobile == null || !MOBILE_PATTERN.matcher(mobile).matches()) {
            throw new BusinessException(ErrorCode.MOBILE_INVALID);
        }
    }
}
