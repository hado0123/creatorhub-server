-- ================================================================
-- CreatorHub Database DDL
-- ================================================================

-- 1. member 테이블
CREATE TABLE member (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        email VARCHAR(100) NOT NULL,
                        password VARCHAR(60) NOT NULL,
                        name VARCHAR(50) NOT NULL,
                        birthday DATE NOT NULL,
                        gender VARCHAR(30) NOT NULL,
                        role VARCHAR(30) NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        deleted_at DATETIME(6),
                        PRIMARY KEY (id),
                        CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. file_object 테이블
CREATE TABLE file_object (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             storage_key VARCHAR(255) NOT NULL,
                             original_filename VARCHAR(255),
                             status VARCHAR(30) NOT NULL,
                             content_type VARCHAR(100) NOT NULL,
                             size_bytes BIGINT NOT NULL,
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             deleted_at DATETIME(6),
                             PRIMARY KEY (id),
                             CONSTRAINT uk_file_object_storage_key UNIQUE (storage_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. creator 테이블
CREATE TABLE creator (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         member_id BIGINT NOT NULL,
                         file_object_id BIGINT,
                         creator_name VARCHAR(150) NOT NULL,
                         introduction VARCHAR(300),
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         deleted_at DATETIME(6),
                         PRIMARY KEY (id),
                         CONSTRAINT fk_creator_member FOREIGN KEY (member_id) REFERENCES member(id),
                         CONSTRAINT fk_creator_file_object FOREIGN KEY (file_object_id) REFERENCES file_object(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. creation 테이블
CREATE TABLE creation (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          creator_id BIGINT NOT NULL,
                          format VARCHAR(30) NOT NULL,
                          genre VARCHAR(30) NOT NULL,
                          title VARCHAR(30) NOT NULL,
                          plot VARCHAR(400) NOT NULL,
                          is_public BOOLEAN NOT NULL,
                          favorite_count INT,
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          deleted_at DATETIME(6),
                          created_by VARCHAR(100),
                          updated_by VARCHAR(100),
                          PRIMARY KEY (id),
                          CONSTRAINT fk_creation_creator FOREIGN KEY (creator_id) REFERENCES creator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. creation_publish_day 테이블 (ElementCollection)
CREATE TABLE creation_publish_day (
                                      creation_id BIGINT NOT NULL,
                                      publish_day VARCHAR(30) NOT NULL,
                                      CONSTRAINT fk_creation_publish_day_creation FOREIGN KEY (creation_id) REFERENCES creation(id),
                                      CONSTRAINT uk_creation_publish_day UNIQUE (creation_id, publish_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. creation_thumbnail 테이블
CREATE TABLE creation_thumbnail (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    creation_id BIGINT NOT NULL,
                                    file_object_id BIGINT NOT NULL,
                                    type VARCHAR(30) NOT NULL,
                                    display_order INT NOT NULL,
                                    source_image_id BIGINT,
                                    created_at DATETIME(6) NOT NULL,
                                    updated_at DATETIME(6) NOT NULL,
                                    deleted_at DATETIME(6),
                                    PRIMARY KEY (id),
                                    CONSTRAINT fk_creation_thumbnail_creation FOREIGN KEY (creation_id) REFERENCES creation(id),
                                    CONSTRAINT fk_creation_thumbnail_file_object FOREIGN KEY (file_object_id) REFERENCES file_object(id),
                                    CONSTRAINT fk_creation_thumbnail_source_image FOREIGN KEY (source_image_id) REFERENCES creation_thumbnail(id),
                                    CONSTRAINT uk_creation_thumbnail_creation_type_order UNIQUE (creation_id, type, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. hashtag 테이블
CREATE TABLE hashtag (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         title VARCHAR(30) NOT NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT uk_hashtag_title UNIQUE (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. creation_hashtag 테이블
CREATE TABLE creation_hashtag (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  creation_id BIGINT NOT NULL,
                                  hashtag_id BIGINT NOT NULL,
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_creation_hashtag_creation FOREIGN KEY (creation_id) REFERENCES creation(id),
                                  CONSTRAINT fk_creation_hashtag_hashtag FOREIGN KEY (hashtag_id) REFERENCES hashtag(id),
                                  CONSTRAINT uk_creation_hashtag_creation_id_hashtag_id UNIQUE (creation_id, hashtag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- creation_hashtag 인덱스
CREATE INDEX idx_creation_hashtag_hashtag_creation ON creation_hashtag(hashtag_id, creation_id);

-- 9. creation_favorite 테이블
CREATE TABLE creation_favorite (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   member_id BIGINT NOT NULL,
                                   creation_id BIGINT NOT NULL,
                                   created_at DATETIME(6) NOT NULL,
                                   updated_at DATETIME(6) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_creation_favorite_member FOREIGN KEY (member_id) REFERENCES member(id),
                                   CONSTRAINT fk_creation_favorite_creation FOREIGN KEY (creation_id) REFERENCES creation(id),
                                   CONSTRAINT uk_creation_favorite_member_creation UNIQUE (member_id, creation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- creation_favorite 인덱스
CREATE INDEX idx_creation_favorite_member_created_at ON creation_favorite(member_id, created_at);

-- 10. episode 테이블
CREATE TABLE episode (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         creation_id BIGINT NOT NULL,
                         episode_num INT NOT NULL,
                         title VARCHAR(35) NOT NULL,
                         creator_note VARCHAR(100) NOT NULL,
                         is_comment_enabled BOOLEAN NOT NULL,
                         is_public BOOLEAN NOT NULL,
                         like_count INT,
                         view_count INT,
                         rating_sum INT,
                         rating_count INT,
                         rating_average DECIMAL(4, 3),
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         deleted_at DATETIME(6),
                         created_by VARCHAR(100),
                         updated_by VARCHAR(100),
                         PRIMARY KEY (id),
                         CONSTRAINT fk_episode_creation FOREIGN KEY (creation_id) REFERENCES creation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- episode 인덱스
CREATE INDEX idx_episode_creation_episode_num ON episode(creation_id, episode_num);

-- 11. episode_thumbnail 테이블
CREATE TABLE episode_thumbnail (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   file_object_id BIGINT NOT NULL,
                                   episode_id BIGINT NOT NULL,
                                   type VARCHAR(30) NOT NULL,
                                   created_at DATETIME(6) NOT NULL,
                                   updated_at DATETIME(6) NOT NULL,
                                   deleted_at DATETIME(6),
                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_episode_thumbnail_file_object FOREIGN KEY (file_object_id) REFERENCES file_object(id),
                                   CONSTRAINT fk_episode_thumbnail_episode FOREIGN KEY (episode_id) REFERENCES episode(id),
                                   CONSTRAINT uk_episode_thumbnail_episode_type UNIQUE (episode_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- episode_thumbnail 인덱스
CREATE INDEX idx_episode_thumbnail_file_object_id ON episode_thumbnail(file_object_id);

-- 12. manuscript_image 테이블
CREATE TABLE manuscript_image (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  file_object_id BIGINT NOT NULL,
                                  episode_id BIGINT NOT NULL,
                                  display_order INT NOT NULL,
                                  created_at DATETIME(6) NOT NULL,
                                  updated_at DATETIME(6) NOT NULL,
                                  deleted_at DATETIME(6),
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_manuscript_image_file_object FOREIGN KEY (file_object_id) REFERENCES file_object(id),
                                  CONSTRAINT fk_manuscript_image_episode FOREIGN KEY (episode_id) REFERENCES episode(id),
                                  CONSTRAINT uk_manuscript_image_episode_display_order UNIQUE (episode_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- manuscript_image 인덱스
CREATE INDEX idx_manuscript_image_file_object_id ON manuscript_image(file_object_id);

-- 13. episode_like 테이블
CREATE TABLE episode_like (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              member_id BIGINT NOT NULL,
                              episode_id BIGINT NOT NULL,
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6) NOT NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_episode_like_member FOREIGN KEY (member_id) REFERENCES member(id),
                              CONSTRAINT fk_episode_like_episode FOREIGN KEY (episode_id) REFERENCES episode(id),
                              CONSTRAINT uk_episode_like_member_episode UNIQUE (member_id, episode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- episode_like 인덱스
CREATE INDEX idx_episode_like_member_created_at ON episode_like(member_id, created_at);

-- 14. episode_rating 테이블
CREATE TABLE episode_rating (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                member_id BIGINT NOT NULL,
                                episode_id BIGINT NOT NULL,
                                score INT NOT NULL,
                                created_at DATETIME(6) NOT NULL,
                                updated_at DATETIME(6) NOT NULL,
                                deleted_at DATETIME(6),
                                PRIMARY KEY (id),
                                CONSTRAINT fk_episode_rating_member FOREIGN KEY (member_id) REFERENCES member(id),
                                CONSTRAINT fk_episode_rating_episode FOREIGN KEY (episode_id) REFERENCES episode(id),
                                CONSTRAINT uk_episode_rating_member_episode UNIQUE (member_id, episode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 테이블 생성 순서
-- ================================================================
-- 1. member (독립 테이블)
-- 2. file_object (독립 테이블)
-- 3. creator (member, file_object 참조)
-- 4. creation (creator 참조)
-- 5. creation_publish_day (creation 참조)
-- 6. creation_thumbnail (creation, file_object 참조, self FK)
-- 7. hashtag (독립 테이블)
-- 8. creation_hashtag (creation, hashtag 참조)
-- 9. creation_favorite (member, creation 참조)
-- 10. episode (creation 참조)
-- 11. episode_thumbnail (episode, file_object 참조)
-- 12. manuscript_image (episode, file_object 참조)
-- 13. episode_like (member, episode 참조)
-- 14. episode_rating (member, episode 참조)