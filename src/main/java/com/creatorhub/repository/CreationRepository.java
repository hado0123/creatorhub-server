package com.creatorhub.repository;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.constant.PublishDay;
import com.creatorhub.entity.Creation;
import com.creatorhub.repository.projection.CreationBaseProjection;
import com.creatorhub.repository.projection.CreationListProjection;
import com.creatorhub.repository.projection.CreationSeekRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreationRepository extends JpaRepository<Creation, Long> {

    @Query("""
        SELECT
            c.id AS creationId,
            c.title AS title,
            fo.storageKey AS storageKey
        FROM Creation c
        LEFT JOIN c.creationThumbnails ct
            ON ct.type = :type AND ct.displayOrder = 0
        LEFT JOIN ct.fileObject fo
        WHERE c.creator.id = :creatorId
        ORDER BY c.id DESC
    """)
    List<CreationListProjection> findAllByCreatorIdWithThumbnails(
            @Param("creatorId") Long creatorId,
            @Param("type") CreationThumbnailType type
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Creation c
           SET c.favoriteCount =
               CASE
                   WHEN :delta < 0 AND COALESCE(c.favoriteCount, 0) <= 0
                       THEN 0
                   ELSE COALESCE(c.favoriteCount, 0) + :delta
               END
         WHERE c.id = :creationId
    """)
    void updateFavoriteCount(@Param("creationId") Long creationId, @Param("delta") int delta);

    @Query("""
        select c
        from Creation c
        where c.id in :ids
    """)
    List<Creation> findByIdIn(@Param("ids") List<Long> ids);

    @Query("""
        SELECT c.creator.member.id
        FROM Creation c
        WHERE c.id = :creationId
    """)
    Optional<Long> findOwnerMemberIdByCreationId(@Param("creationId") Long creationId);

    @Query("""
        SELECT
            c.id AS creationId,
            c.title AS title,
            c.plot AS plot,
            cr.creatorName AS creatorName,
            fo.storageKey AS storageKey
        FROM Creation c
        JOIN c.creator cr
        LEFT JOIN c.creationThumbnails ct
            ON ct.type = :posterType AND ct.displayOrder = 0
        LEFT JOIN ct.fileObject fo
        WHERE c.id = :creationId
    """)
    Optional<CreationBaseProjection> findCreationDetailBase(
            @Param("creationId") Long creationId,
            @Param("posterType") CreationThumbnailType posterType
    );

    // 조회수 집계: episode view_count 합산으로 갱신
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Creation c
           SET c.totalViewCount = (
               SELECT COALESCE(SUM(e.viewCount), 0)
               FROM Episode e
               WHERE e.creation = c
           )
         WHERE c.id = :creationId
    """)
    void updateTotalViewCount(@Param("creationId") Long creationId);

    // 좋아요 집계: delta 증감
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Creation c
           SET c.totalLikeCount =
               CASE
                   WHEN :delta < 0 AND COALESCE(c.totalLikeCount, 0) <= 0
                       THEN 0
                   ELSE COALESCE(c.totalLikeCount, 0) + :delta
               END
         WHERE c.id = :creationId
    """)
    void updateTotalLikeCount(@Param("creationId") Long creationId, @Param("delta") int delta);

    // 별점 집계: score 추가, count +1, average 재계산
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Creation c
           SET c.totalRatingSum = COALESCE(c.totalRatingSum, 0) + :score,
               c.totalRatingCount = COALESCE(c.totalRatingCount, 0) + 1,
               c.totalRatingAverage = CAST(COALESCE(c.totalRatingSum, 0) + :score AS bigdecimal)
                                      / (COALESCE(c.totalRatingCount, 0) + 1)
         WHERE c.id = :creationId
    """)
    void addTotalRating(@Param("creationId") Long creationId, @Param("score") int score);

    // 조회순
    @Query("""
        SELECT
            c.id AS id,
            COALESCE(c.totalViewCount, 0) AS longValue,
            NULL AS doubleValue,
            NULL AS tie
        FROM Creation c
        WHERE c.isPublic = true
          AND :day MEMBER OF c.publishDays
          AND (
              :cursorValue IS NULL
              OR COALESCE(c.totalViewCount, 0) < :cursorValue
              OR (COALESCE(c.totalViewCount, 0) = :cursorValue AND c.id < :cursorId)
          )
        ORDER BY COALESCE(c.totalViewCount, 0) DESC, c.id DESC
    """)
    List<CreationSeekRow> findByDayOrderByViewsSeek(
            @Param("day") PublishDay day,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 인기순(좋아요)
    @Query("""
        SELECT
            c.id AS id,
            COALESCE(c.totalLikeCount, 0) AS longValue,
            NULL AS doubleValue,
            NULL AS tie
        FROM Creation c
        WHERE c.isPublic = true
          AND :day MEMBER OF c.publishDays
          AND (
              :cursorValue IS NULL
              OR COALESCE(c.totalLikeCount, 0) < :cursorValue
              OR (COALESCE(c.totalLikeCount, 0) = :cursorValue AND c.id < :cursorId)
          )
        ORDER BY COALESCE(c.totalLikeCount, 0) DESC, c.id DESC
    """)
    List<CreationSeekRow> findByDayOrderByLikesSeek(
            @Param("day") PublishDay day,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 별점순
    // totalRatingAverage 사전 계산된 컬럼 사용
    // ratingCount 합이 0인 경우 0 처리
    @Query("""
        SELECT
            c.id AS id,
            NULL AS longValue,
            COALESCE(CAST(c.totalRatingAverage AS double), 0.0) AS doubleValue,
            COALESCE(CAST(c.totalRatingCount AS long), 0L) AS tie
        FROM Creation c
        WHERE c.isPublic = true
          AND :day MEMBER OF c.publishDays
          AND (
              :cursorAvg IS NULL
              OR COALESCE(CAST(c.totalRatingAverage AS double), 0.0) < :cursorAvg
              OR (
                  COALESCE(CAST(c.totalRatingAverage AS double), 0.0) = :cursorAvg
                  AND COALESCE(CAST(c.totalRatingCount AS long), 0L) < :cursorRatingCount
              )
              OR (
                  COALESCE(CAST(c.totalRatingAverage AS double), 0.0) = :cursorAvg
                  AND COALESCE(CAST(c.totalRatingCount AS long), 0L) = :cursorRatingCount
                  AND c.id < :cursorId
              )
          )
        ORDER BY COALESCE(CAST(c.totalRatingAverage AS double), 0.0) DESC,
                 COALESCE(CAST(c.totalRatingCount AS long), 0L) DESC,
                 c.id DESC
    """)
    List<CreationSeekRow> findByDayOrderByRatingSeek(
            @Param("day") PublishDay day,
            @Param("cursorAvg") Double cursorAvg,
            @Param("cursorRatingCount") Long cursorRatingCount,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}