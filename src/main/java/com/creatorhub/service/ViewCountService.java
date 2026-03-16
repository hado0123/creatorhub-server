package com.creatorhub.service;

import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private static final String EPISODE_KEY_PREFIX  = "VC:E:";
    private static final String CREATION_KEY_PREFIX = "VC:C:";

    private final EpisodeRepository  episodeRepository;
    private final CreationRepository creationRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 요청마다 Redis INCR만 수행 — DB lock 경합 없음
     */
    public void increment(Long episodeId, Long creationId) {
        redisTemplate.opsForValue().increment(EPISODE_KEY_PREFIX  + episodeId);
        redisTemplate.opsForValue().increment(CREATION_KEY_PREFIX + creationId);
    }

    /**
     * 10초마다 Redis에 쌓인 조회수를 DB에 batch flush
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void flush() {
        flushPattern(EPISODE_KEY_PREFIX,
                episodeRepository::incrementViewCountBy);
        flushPattern(CREATION_KEY_PREFIX,
                creationRepository::incrementTotalViewCountBy);
    }

    private void flushPattern(String prefix, BiConsumer<Long, Long> updater) {
        ScanOptions opts = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(200)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(opts)) {
            cursor.forEachRemaining(key -> {
                String raw = redisTemplate.opsForValue().getAndDelete(key);
                if (raw == null) return;
                long delta = Long.parseLong(raw);
                long id    = Long.parseLong(key.substring(prefix.length()));
                updater.accept(id, delta);
            });
        }
    }
}
