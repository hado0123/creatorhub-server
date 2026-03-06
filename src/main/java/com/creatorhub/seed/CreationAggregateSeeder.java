package com.creatorhub.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreationAggregateSeeder {

    private final JdbcTemplate jdbcTemplate;

    public void updateAggregates() {
        log.info("[CreationAggregateSeeder] 시작 — creation 집계 컬럼 업데이트");

        int updated = jdbcTemplate.update("""
                UPDATE creation c
                   SET c.total_view_count = (SELECT COALESCE(SUM(e.view_count), 0) FROM episode e WHERE e.creation_id = c.id),
                       c.total_like_count = (SELECT COALESCE(SUM(e.like_count), 0) FROM episode e WHERE e.creation_id = c.id),
                       c.total_rating_sum = (SELECT COALESCE(SUM(e.rating_sum), 0) FROM episode e WHERE e.creation_id = c.id),
                       c.total_rating_count = (SELECT COALESCE(SUM(e.rating_count), 0) FROM episode e WHERE e.creation_id = c.id),
                       c.total_rating_average = CASE
                           WHEN (SELECT COALESCE(SUM(e.rating_count), 0) FROM episode e WHERE e.creation_id = c.id) = 0 THEN 0
                           ELSE (SELECT COALESCE(SUM(e.rating_sum), 0) FROM episode e WHERE e.creation_id = c.id)
                                / (SELECT COALESCE(SUM(e.rating_count), 0) FROM episode e WHERE e.creation_id = c.id)
                       END
                """);

        log.info("[CreationAggregateSeeder] 완료 — {}개 creation 업데이트", updated);
    }
}
