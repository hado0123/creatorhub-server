package com.creatorhub.repository;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.entity.Creation;
import com.creatorhub.repository.projection.CreationBaseProjection;
import com.creatorhub.repository.projection.CreationListProjection;
import com.creatorhub.repository.projection.CreationSeekRow;
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

    // 조회수 증가: +1 단순 증가 (SUM 집계 서브쿼리 제거)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Creation c
           SET c.totalViewCount = COALESCE(c.totalViewCount, 0) + 1
         WHERE c.id = :creationId
    """)
    void incrementTotalViewCount(@Param("creationId") Long creationId);

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

    // 조회순 (네이티브 — 쿼리 1회로 title + poster까지 조회)
    @Query(value = """
        SELECT c.id              AS id,
               c.total_view_count AS longValue,
               NULL               AS doubleValue,
               NULL               AS tie,
               c.title            AS title,
               fo.storage_key     AS storageKey
          FROM creation c
          JOIN creation_publish_day cpd
            ON cpd.creation_id = c.id AND cpd.publish_day = :day
          LEFT JOIN creation_thumbnail ct
            ON ct.creation_id = c.id AND ct.type = 'POSTER' AND ct.display_order = 0
          LEFT JOIN file_object fo
            ON fo.id = ct.file_object_id
         WHERE c.is_public = true
           AND c.deleted_at IS NULL
           AND (:cursorValue IS NULL
                OR c.total_view_count < :cursorValue
                OR (c.total_view_count = :cursorValue AND c.id < :cursorId))
         ORDER BY c.total_view_count DESC, c.id DESC
         LIMIT :size
        """, nativeQuery = true)
    List<CreationSeekRow> findByDayOrderByViewsSeek(
            @Param("day") String day,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    // 인기순(좋아요) (네이티브 — 쿼리 1회로 title + poster까지 조회)
    @Query(value = """
        SELECT c.id               AS id,
               c.total_like_count  AS longValue,
               NULL                AS doubleValue,
               NULL                AS tie,
               c.title             AS title,
               fo.storage_key      AS storageKey
          FROM creation c
          JOIN creation_publish_day cpd
            ON cpd.creation_id = c.id AND cpd.publish_day = :day
          LEFT JOIN creation_thumbnail ct
            ON ct.creation_id = c.id AND ct.type = 'POSTER' AND ct.display_order = 0
          LEFT JOIN file_object fo
            ON fo.id = ct.file_object_id
         WHERE c.is_public = true
           AND c.deleted_at IS NULL
           AND (:cursorValue IS NULL
                OR c.total_like_count < :cursorValue
                OR (c.total_like_count = :cursorValue AND c.id < :cursorId))
         ORDER BY c.total_like_count DESC, c.id DESC
         LIMIT :size
        """, nativeQuery = true)
    List<CreationSeekRow> findByDayOrderByLikesSeek(
            @Param("day") String day,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    // 별점순 (네이티브 — 쿼리 1회로 title + poster까지 조회)
    @Query(value = """
        SELECT c.id                    AS id,
               NULL                     AS longValue,
               c.total_rating_average + 0.0 AS doubleValue,
               CAST(c.total_rating_count AS SIGNED) AS tie,
               c.title                 AS title,
               fo.storage_key          AS storageKey
          FROM creation c
          JOIN creation_publish_day cpd
            ON cpd.creation_id = c.id AND cpd.publish_day = :day
          LEFT JOIN creation_thumbnail ct
            ON ct.creation_id = c.id AND ct.type = 'POSTER' AND ct.display_order = 0
          LEFT JOIN file_object fo
            ON fo.id = ct.file_object_id
         WHERE c.is_public = true
           AND c.deleted_at IS NULL
           AND (:cursorAvg IS NULL
                OR c.total_rating_average < :cursorAvg
                OR (c.total_rating_average = :cursorAvg AND c.total_rating_count < :cursorRatingCount)
                OR (c.total_rating_average = :cursorAvg AND c.total_rating_count = :cursorRatingCount AND c.id < :cursorId))
         ORDER BY c.total_rating_average DESC, c.total_rating_count DESC, c.id DESC
         LIMIT :size
        """, nativeQuery = true)
    List<CreationSeekRow> findByDayOrderByRatingSeek(
            @Param("day") String day,
            @Param("cursorAvg") Double cursorAvg,
            @Param("cursorRatingCount") Long cursorRatingCount,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );
}