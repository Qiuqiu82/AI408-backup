package org.example.ai408.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.UserEntity;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.key = Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UserEntity user) {
        return issueAccessToken(user, "password");
    }

    public String issueAccessToken(UserEntity user, String authMethod) {
        return buildToken(user, JwtType.ACCESS, appProperties.jwt().accessTokenMinutes(), ChronoUnit.MINUTES, authMethod);
    }

    public String issueRefreshToken(UserEntity user) {
        return issueRefreshToken(user, "password");
    }

    public String issueRefreshToken(UserEntity user, String authMethod) {
        return buildToken(user, JwtType.REFRESH, appProperties.jwt().refreshTokenDays(), ChronoUnit.DAYS, authMethod);
    }

    private String buildToken(UserEntity user, JwtType type, long amount, ChronoUnit unit, String authMethod) {
        Instant now = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("typ", type.name());
        claims.put("authMethod", authMethod == null || authMethod.isBlank() ? "password" : authMethod);
        claims.put("mobile", user.getMobile());
        claims.put("email", user.getEmail());
        claims.put("nickname", user.getNickname());
        claims.put("role", user.getRole());
        return Jwts.builder()
                .subject(user.getId())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(amount, unit)))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token, JwtType expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String type = claims.get("typ", String.class);
        if (!expectedType.name().equals(type)) {
            throw new IllegalArgumentException("token type mismatch");
        }
        return claims;
    }
}
