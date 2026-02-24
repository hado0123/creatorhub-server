package com.creatorhub.security.utils;

import com.creatorhub.dto.auth.RefreshTokenPayload;
import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.constant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JWTUtil {
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessTokenExpMinutes;
    private final long refreshTokenExpDays;

    public JWTUtil(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.access-token-exp-min}") long accessTokenExpMinutes,
            @Value("${jwt.refresh-token-exp-days}") long refreshTokenExpDays
    ) {
        try {
            this.accessSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
            this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("JWTUtil - JWT secret key 초기화 실패", e);
            throw new IllegalStateException("JWT secret key 초기화 실패", e);
        }

        this.accessTokenExpMinutes = accessTokenExpMinutes;
        this.refreshTokenExpDays = refreshTokenExpDays;
    }

    /**
     * Access 토큰 생성
     */
    public String createAccessToken(TokenPayload payload) {
        return createToken(payload.toClaims(), accessTokenExpMinutes, accessSecretKey);
    }

    /**
     * Refresh 토큰 생성
     */
    public String createRefreshToken(RefreshTokenPayload payload) {
        long refreshMinutes = refreshTokenExpDays * 24 * 60;
        return createToken(payload.toClaims(), refreshMinutes, refreshSecretKey);
    }

    /**
     * 공통 토큰 생성
     */
    private String createToken(Map<String, Object> claims, long expireMinutes, SecretKey key) {

        ZonedDateTime now = ZonedDateTime.now();

        return Jwts.builder().header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(now.plusMinutes(expireMinutes).toInstant()))
                .claims(claims)
                .signWith(key)
                .compact();
    }

    /**
     * 공통 Claims 파싱 (서명/만료 검증 포함)
     */
    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Access 토큰 검증
     */
    public TokenPayload validateToken(String token) {

        Claims claims = parseClaims(token, accessSecretKey);

        Long id = claims.get("id", Long.class);
        String name = claims.get("name", String.class);
        String email = claims.get("email", String.class);
        String roleStr = claims.get("role", String.class);

        return new TokenPayload(
                id,
                name,
                email,
                Role.valueOf(roleStr)
        );
    }

    /**
     * Refresh 토큰 검증
     */
    public RefreshTokenPayload validateRefreshToken(String token) {
        Claims claims = parseClaims(token, refreshSecretKey);
        Long id = claims.get("id", Long.class);
        
        Boolean keepLogin = claims.get("keepLogin", Boolean.class);
        boolean keepLoginValue = keepLogin != null && keepLogin;

        return new RefreshTokenPayload(id, keepLoginValue);
    }
}
