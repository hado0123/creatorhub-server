package com.creatorhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-exp-days}")
    private long refreshTokenExpDays;

    private String key(Long id) {
        return "RT:" + id;
    }

    /**
     * Refresh 토큰 저장 또는 갱신
     */
    public void saveRefreshToken(Long id, String refreshToken) {
        String key = key(id);
        log.debug("Refresh token key: {}", key);

        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                refreshTokenExpDays, // 7일 뒤 삭제
                TimeUnit.DAYS
        );
        log.debug("Refresh Token Redis 저장 완료 - memberId: {}", id);
    }

    /**
     * Redis에 저장된 Refresh 토큰 조회
     */
    public String getRefreshToken(Long id) {
        return redisTemplate.opsForValue().get(key(id));
    }

    /**
     * 로그아웃/강제 만료 시 삭제
     */
    public void deleteRefreshToken(Long id) {
        redisTemplate.delete(key(id));
        log.debug("Refresh Token Redis 삭제 완료 - memberId: {}", id);
    }
}