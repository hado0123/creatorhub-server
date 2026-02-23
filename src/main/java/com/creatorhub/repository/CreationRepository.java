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

    // 조회순
    @Query("""
        SELECT
            c.id AS id,
            COALESCE(SUM(e.viewCount), 0) AS longValue,
            NULL AS doubleValue,
            NULL AS tie
        FROM Creation c
        JOIN Episode e ON e.creation = c AND e.isPublic = true
        WHERE c.isPublic = true
          AND :day MEMBER OF c.publishDays
        GROUP BY c.id
        HAVING (
            :cursorValue IS NULL
            OR COALESCE(SUM(e.viewCount), 0) < :cursorValue
            OR (COALESCE(SUM(e.viewCount), 0) = :cursorValue AND c.id < :cursorId)
        )
        ORDER BY COALESCE(SUM(e.viewCount), 0) DESC, c.id DESC
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
            COALESCE(SUM(e.likeCount), 0) AS longValue,
            NULL AS doubleValue,
            NULL AS tie
        FROM Creation c
        JOIN Episode e ON e.creation = c AND e.isPublic = true
        WHERE c.isPublic = true
          AND :day MEMBER OF c.publishDays
        GROUP BY c.id
        HAVING (
            :cursorValue IS NULL
            OR COALESCE(SUM(e.likeCount), 0) < :cursorValue
            OR (COALESCE(SUM(e.likeCount), 0) = :cursorValue AND c.id < :cursorId)
        )
        ORDER BY COALESCE(SUM(e.likeCount), 0) DESC, c.id DESC
    """)
    List<CreationSeekRow> findByDayOrderByLikesSeek(
            @Param("day") PublishDay day,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 별점순
    // 가중평균 = SUM(ratingSum) / SUM(ratingCount)
    // ratingCount 합이 0인 경우 0 처리
    @Query("""
        SELECT
            c.id AS id,
            NULL AS longValue,
            (CASE
                WHEN COALESCE(SUM(e.ratingCount), 0) = 0
                    THEN 0.0
                ELSE (1.0 * COALESCE(SUM(e.ratingSum), 0))
                     / COALESCE(SUM(e.ratingCount), 0)
             END) AS doubleValue,
            COALESCE(SUM(e.ratingCount), 0) AS tie
        FROM Creation c
        JOIN Episode e ON e.creation = c AND e.isPublic = true
        WHERE c.isPublic = true
          AND :day MEMBER OF c.publishDays
        GROUP BY c.id
        HAVING (
            :cursorAvg IS NULL
            OR (
                (CASE
                    WHEN COALESCE(SUM(e.ratingCount), 0) = 0
                        THEN 0.0
                    ELSE (1.0 * COALESCE(SUM(e.ratingSum), 0))
                         / COALESCE(SUM(e.ratingCount), 0)
                 END) < :cursorAvg
            )
            OR (
                (CASE
                    WHEN COALESCE(SUM(e.ratingCount), 0) = 0
                        THEN 0.0
                    ELSE (1.0 * COALESCE(SUM(e.ratingSum), 0))
                         / COALESCE(SUM(e.ratingCount), 0)
                 END) = :cursorAvg
                AND COALESCE(SUM(e.ratingCount), 0) < :cursorRatingCount
            )
            OR (
                (CASE
                    WHEN COALESCE(SUM(e.ratingCount), 0) = 0
                        THEN 0.0
                    ELSE (1.0 * COALESCE(SUM(e.ratingSum), 0))
                         / COALESCE(SUM(e.ratingCount), 0)
                 END) = :cursorAvg
                AND COALESCE(SUM(e.ratingCount), 0) = :cursorRatingCount
                AND c.id < :cursorId
            )
        )
        ORDER BY
            (CASE
                WHEN COALESCE(SUM(e.ratingCount), 0) = 0
                    THEN 0.0
                ELSE (1.0 * COALESCE(SUM(e.ratingSum), 0))
                     / COALESCE(SUM(e.ratingCount), 0)
             END) DESC,
            COALESCE(SUM(e.ratingCount), 0) DESC,
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