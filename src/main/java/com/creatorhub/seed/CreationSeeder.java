package com.creatorhub.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreationSeeder {

    private final JdbcTemplate jdbcTemplate;

    private static final String[] FORMATS = {"EPISODE", "OMNIBUS", "STORY"};
    private static final String[] GENRES = {
            "ROMANCE", "FANTASY", "ACTION", "DAILY_LIFE", "THRILLER",
            "COMEDY", "MARTIAL_ARTS", "DRAMA", "EMOTIONAL", "SPORTS"
    };
    private static final String[] PUBLISH_DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    private static final String SEED_TITLE = "시드 작품";
    private static final String SEED_PLOT = "성능 테스트를 위한 시드 데이터입니다.";

    @Transactional
    public void seedCreations(int totalCount, int publicCount) {
        Long dummyFileObjectId = ensureDummyReadyFileObject();

        // 1) 모든 creator ID 조회
        List<Long> creatorIds = jdbcTemplate.queryForList(
                "SELECT id FROM creator",
                Long.class
        );

        if (creatorIds.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        Random random = new Random(42);

        // 2) creation batch insert — totalCount개를 creator들에게 라운드 로빈 배분
        //    후반부에 공개 작품이 몰리되, 간헐적으로 섞이도록 확률 기반 배정
        //    진행도(0.0~1.0)에 따라 공개 확률이 점점 높아짐
        List<Object[]> creationParams = new ArrayList<>();
        int publicAssigned = 0;
        int privateAssigned = 0;
        int privateTotal = totalCount - publicCount;

        for (int i = 0; i < totalCount; i++) {
            Long creatorId = creatorIds.get(i % creatorIds.size());
            String format = FORMATS[random.nextInt(FORMATS.length)];
            String genre = GENRES[random.nextInt(GENRES.length)];

            double progress = (double) i / totalCount; // 0.0 → 1.0
            double publicProb = progress * progress;    // 후반으로 갈수록 급격히 증가

            boolean isPublic;
            int publicRemaining = publicCount - publicAssigned;
            int privateRemaining = privateTotal - privateAssigned;

            if (privateRemaining <= 0) {
                isPublic = true;
            } else if (publicRemaining <= 0) {
                isPublic = false;
            } else {
                isPublic = random.nextDouble() < publicProb;
            }

            if (isPublic) publicAssigned++;
            else privateAssigned++;

            String title = SEED_TITLE + (i + 1);
            creationParams.add(new Object[]{creatorId, format, genre, isPublic, title});
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO creation (" +
                        "creator_id, format, genre, title, plot, is_public, favorite_count, " +
                        "created_at, updated_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                creationParams,
                500,
                (ps, param) -> {
                    ps.setLong(1, (Long) param[0]);
                    ps.setString(2, (String) param[1]);
                    ps.setString(3, (String) param[2]);
                    ps.setString(4, (String) param[4]);
                    ps.setString(5, SEED_PLOT);
                    ps.setBoolean(6, (Boolean) param[3]);
                    ps.setInt(7, 0);
                    ps.setObject(8, now);
                    ps.setObject(9, now);
                }
        );

        // 3) 방금 만든 creation id 조회 (썸네일, 연재요일 삽입용)
        List<Long> creationIds = new ArrayList<>();
        for (List<Long> chunk : chunk(creatorIds, 1000)) {
            String inSql = chunk.stream().map(x -> "?").collect(Collectors.joining(","));
            String sql = "SELECT id FROM creation WHERE creator_id IN (" + inSql + ") ORDER BY id";
            creationIds.addAll(jdbcTemplate.queryForList(sql, Long.class, chunk.toArray()));
        }

        // 5) creation_publish_day batch insert
        //    요일별 120개가 되도록 배정 (1000 / 7 = 142~143)
        List<Object[]> publishDayParams = new ArrayList<>();
        for (int i = 0; i < creationIds.size(); i++) {
            String day = PUBLISH_DAYS[i % PUBLISH_DAYS.length];
            publishDayParams.add(new Object[]{creationIds.get(i), day});
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO creation_publish_day (creation_id, publish_day) VALUES (?, ?)",
                publishDayParams,
                500,
                (ps, param) -> {
                    ps.setLong(1, (Long) param[0]);
                    ps.setString(2, (String) param[1]);
                }
        );

        // 6) creation_thumbnail batch insert (POSTER 타입, display_order=1)
        jdbcTemplate.batchUpdate(
                "INSERT INTO creation_thumbnail (" +
                        "creation_id, file_object_id, type, display_order, " +
                        "created_at, updated_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?)",
                creationIds,
                500,
                (ps, creationId) -> {
                    ps.setLong(1, creationId);
                    ps.setLong(2, dummyFileObjectId);
                    ps.setString(3, "POSTER");
                    ps.setInt(4, 1);
                    ps.setObject(5, now);
                    ps.setObject(6, now);
                }
        );
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
