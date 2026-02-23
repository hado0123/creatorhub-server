package com.creatorhub.repository;

import com.creatorhub.constant.EpisodeThumbnailType;
import com.creatorhub.entity.Episode;
import com.creatorhub.repository.projection.EpisodeListProjection;
import com.creatorhub.repository.projection.EpisodeMetaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    boolean existsByCreationIdAndEpisodeNum(Long creationId, Integer episodeNum);

    @Query("""
    SELECT
        e.id AS id,
        e.episodeNum AS episodeNum,
        e.title AS title,
        e.isPublic AS isPublic,
        e.ratingAverage AS ratingAverage,
        fo.storageKey AS storageKey,
        e.createdAt AS createdAt
    FROM Episode e
    LEFT JOIN e.episodeThumbnails t
        ON t.type = :type
    LEFT JOIN t.fileObject fo
    WHERE e.creation.id = :creationId
    ORDER BY e.episodeNum ASC
""")
    List<EpisodeListProjection> findEpisodeListProjection(
            Long creationId,
            EpisodeThumbnailType type
    );

    @Query("""
        SELECT
            e.id AS episodeId,
            e.episodeNum AS episodeNum,
            e.title AS title,
            e.likeCount AS likeCount,
            e.ratingAverage AS ratingAverage,
            e.ratingCount AS ratingCount
        FROM Episode e
        WHERE e.id = :episodeId
          AND e.creation.id = :creationId
    """)
    Optional<EpisodeMetaProjection> findEpisodeMeta(
            @Param("creationId") Long creationId,
            @Param("episodeId") Long episodeId
    );

    // JPA 엔티티 방식으로 작업시 읽기 -> 계산 -> 쓰기 방식으로 진행되기 때문에 동시 요청시 likeCount값이 -1이 될 수 있음
    // 이를 방지하기 위해 UPDATE 쿼리에서 증감 연산을 수행해 DB에서 원자성을 보장하도록 구현
    // 동시에 likeCount값이 0 아래로 더 이상 감소되지 않도록 처리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Episode e
           SET e.likeCount =
               CASE
                   WHEN :delta < 0 AND COALESCE(e.likeCount, 0) <= 0
                       THEN 0
                   ELSE COALESCE(e.likeCount, 0) + :delta
               END
         WHERE e.id = :episodeId
    """)
    void updateLikeCount(@Param("episodeId") Long episodeId,
                              @Param("delta") int delta);

    // 별점 최초 등록: sum += score, count += 1, average = sum/(count + 1)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Episode e
           SET e.ratingSum = COALESCE(e.ratingSum, 0) + :score,
               e.ratingCount =COALESCE(e.ratingCount, 0) + 1,
               e.ratingAverage = CAST(COALESCE(e.ratingSum, 0) + :score AS bigdecimal)
                                 / (COALESCE(e.ratingCount, 0) + 1)
           WHERE e.id = :episodeId
    """)
    void addRating(@Param("episodeId") Long episodeId,
                  @Param("score") int score);
}