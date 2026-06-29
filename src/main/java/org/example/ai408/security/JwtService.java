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
        return buildToken(user, JwtType.ACCESS, appProperties.jwt().accessTokenMinutes(), ChronoUnit.MINUTES);
    }

    public String issueRefreshToken(UserEntity user) {
        return buildToken(user, JwtType.REFRESH, appProperties.jwt().refreshTokenDays(), ChronoUnit.DAYS);
    }

    private String buildToken(UserEntity user, JwtType type, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claims(Map.of(
                        "typ", type.name(),
                        "mobile", user.getMobile(),
                        "nickname", user.getNickname(),
                        "role", user.getRole()
                ))
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
