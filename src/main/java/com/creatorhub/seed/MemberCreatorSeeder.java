package com.creatorhub.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MemberCreatorSeeder {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private static final String[] GENDERS = {"NONE", "MALE", "FEMALE"};
    private static final String SEED_PASSWORD = "seed1234!";

    @Transactional
    public void seedMembersAndCreators(int totalCount) {
        // 이미 시드 회원이 있으면 스킵
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE email LIKE 'seed%@creatorhub.com'",
                Integer.class
        );
        if (existingCount != null && existingCount > 0) return;

        LocalDateTime now = LocalDateTime.now();
        Random random = new Random(42);
        String encodedPassword = passwordEncoder.encode(SEED_PASSWORD);

        // 1) member batch insert
        List<Object[]> memberParams = new ArrayList<>();
        for (int i = 1; i <= totalCount; i++) {
            String email = "seed" + i + "@creatorhub.com";
            String name = "시드작가" + i;
            LocalDate birthday = LocalDate.of(
                    1985 + random.nextInt(16),  // 1985~2000
                    1 + random.nextInt(12),
                    1 + random.nextInt(28)
            );
            String gender = GENDERS[random.nextInt(GENDERS.length)];
            memberParams.add(new Object[]{email, name, birthday, gender});
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO member (email, password, name, birthday, gender, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                memberParams,
                500,
                (ps, param) -> {
                    ps.setString(1, (String) param[0]);
                    ps.setString(2, encodedPassword);
                    ps.setString(3, (String) param[1]);
                    ps.setObject(4, param[2]);
                    ps.setString(5, (String) param[3]);
                    ps.setString(6, "CREATOR");
                    ps.setObject(7, now);
                    ps.setObject(8, now);
                }
        );

        // 2) 방금 만든 member id 조회
        List<Long> memberIds = jdbcTemplate.queryForList(
                "SELECT id FROM member WHERE email LIKE 'seed%@creatorhub.com' ORDER BY id",
                Long.class
        );

        // 3) creator batch insert
        List<Object[]> creatorParams = new ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            String creatorName = "시드작가" + (i + 1);
            creatorParams.add(new Object[]{memberIds.get(i), creatorName});
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO creator (member_id, creator_name, introduction, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                creatorParams,
                500,
                (ps, param) -> {
                    ps.setLong(1, (Long) param[0]);
                    ps.setString(2, (String) param[1]);
                    ps.setString(3, "시드 데이터 작가입니다.");
                    ps.setObject(4, now);
                    ps.setObject(5, now);
                }
        );
    }
}
