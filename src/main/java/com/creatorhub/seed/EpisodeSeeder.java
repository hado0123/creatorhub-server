package com.creatorhub.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeSeeder {

    private final JdbcTemplate jdbcTemplate;

    public void seedEpisodes() {
        Long dummyFileObjectId = ensureDummyReadyFileObject();

        // 1) 아직 회차가 없는 작품 ID 조회
        List<Long> creationIds = jdbcTemplate.queryForList(
                "SELECT c.id FROM creation c " +
                        "LEFT JOIN episode e ON e.creation_id = c.id " +
                        "WHERE e.id IS NULL",
                Long.class
        );

        if (creationIds.isEmpty()) {
            log.info("[EpisodeSeeder] 시드할 작품이 없습니다.");
            return;
        }

        log.info("[EpisodeSeeder] 시작 — 대상 작품 {}개", creationIds.size());

        Random random = new Random(42);
        int totalEpisodes = 0;
        int processedCreations = 0;

        // 2) 작품 100개 단위로 묶어서 처리
        List<List<Long>> creationChunks = chunk(creationIds, 100);

        for (List<Long> creationChunk : creationChunks) {
            LocalDateTime now = LocalDateTime.now();
            List<Object[]> episodeParams = new ArrayList<>();

            for (Long creationId : creationChunk) {
                int episodeCount = generateEpisodeCount(random);
                for (int ep = 1; ep <= episodeCount; ep++) {
                    int likeCount = random.nextInt(5001);
                    int viewCount = 100 + random.nextInt(999901);
                    int ratingCount = random.nextInt(7001);
                    int ratingSum = ratingCount == 0 ? 0 : ratingCount * 2 + random.nextInt(ratingCount * 3 + 1);
                    BigDecimal ratingAverage = ratingCount == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(ratingSum).divide(BigDecimal.valueOf(ratingCount), 3, RoundingMode.HALF_UP);

                    episodeParams.add(new Object[]{
                            creationId, ep, ep + "화",
                            likeCount, viewCount, ratingSum, ratingCount, ratingAverage
                    });
                }
            }

            // episode insert
            jdbcTemplate.batchUpdate(
                    "INSERT INTO episode (" +
                            "creation_id, episode_num, title, creator_note, is_public, is_comment_enabled, " +
                            "like_count, view_count, rating_sum, rating_count, rating_average, " +
                            "created_at, updated_at" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    episodeParams,
                    500,
                    (ps, param) -> {
                        ps.setLong(1, (Long) param[0]);
                        ps.setInt(2, (Integer) param[1]);
                        ps.setString(3, (String) param[2]);
                        ps.setString(4, "seed");
                        ps.setBoolean(5, true);
                        ps.setBoolean(6, true);
                        ps.setInt(7, (Integer) param[3]);
                        ps.setInt(8, (Integer) param[4]);
                        ps.setInt(9, (Integer) param[5]);
                        ps.setInt(10, (Integer) param[6]);
                        ps.setBigDecimal(11, (BigDecimal) param[7]);
                        ps.setObject(12, now);
                        ps.setObject(13, now);
                    }
            );

            // episode id 조회 → thumbnail, manuscript insert
            String inSql = creationChunk.stream().map(x -> "?").collect(Collectors.joining(","));
            List<Long> episodeIds = jdbcTemplate.queryForList(
                    "SELECT id FROM episode WHERE creation_id IN (" + inSql + ") ORDER BY id",
                    Long.class,
                    creationChunk.toArray()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO episode_thumbnail (episode_id, file_object_id, type, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    episodeIds,
                    500,
                    (ps, episodeId) -> {
                        ps.setLong(1, episodeId);
                        ps.setLong(2, dummyFileObjectId);
                        ps.setString(3, "EPISODE");
                        ps.setObject(4, now);
                        ps.setObject(5, now);
                    }
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO manuscript_image (episode_id, display_order, file_object_id, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    episodeIds,
                    500,
                    (ps, episodeId) -> {
                        ps.setLong(1, episodeId);
                        ps.setInt(2, 1);
                        ps.setLong(3, dummyFileObjectId);
                        ps.setObject(4, now);
                        ps.setObject(5, now);
                    }
            );

            processedCreations += creationChunk.size();
            totalEpisodes += episodeParams.size();
            log.info("[EpisodeSeeder] 진행 {}/{} 작품 완료 (누적 회차 {}개)",
                    processedCreations, creationIds.size(), totalEpisodes);
        }

        log.info("[EpisodeSeeder] 완료 — 총 {}개 작품, {}개 회차 생성", processedCreations, totalEpisodes);
    }

    /**
     * 20~100화가 가장 많고, 1~19화 또는 101~300화는 드물게 나오는 분포.
     * 70% 확률로 20~100화, 15% 확률로 1~19화, 15% 확률로 101~300화.
     */
    private int generateEpisodeCount(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.15) {
            return 1 + random.nextInt(19);          // 1~19
        } else if (roll < 0.85) {
            return 20 + random.nextInt(81);         // 20~100
        } else {
            return 101 + random.nextInt(200);       // 101~300
        }
    }

    private Long ensureDummyReadyFileObject() {
        List<Long> found = jdbcTemplate.queryForList(
                "SELECT id FROM file_object WHERE storage_key = ? LIMIT 1",
                Long.class,
                "seed/dummy.jpg"
        );
        if (!found.isEmpty()) return found.get(0);

        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO file_object (storage_key, original_filename, status, content_type, size_bytes, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, "seed/dummy.jpg");
            ps.setString(2, "dummy.jpg");
            ps.setString(3, "READY");
            ps.setString(4, "image/jpeg");
            ps.setLong(5, 0L);
            return ps;
        }, kh);

        Number key = kh.getKey();
        if (key == null) throw new IllegalStateException("Failed to create dummy file_object");
        return key.longValue();
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            out.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return out;
    }
}
