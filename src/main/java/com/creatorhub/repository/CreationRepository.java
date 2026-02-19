package com.creatorhub.repository;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.entity.Creation;
import com.creatorhub.repository.projection.CreationBaseProjection;
import com.creatorhub.repository.projection.CreationListProjection;
import org.springframework.data.jpa.repository.EntityGraph;
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
        SELECT c.id
        FROM Creation c
        WHERE c.isPublic = TRUE
        ORDER BY c.id DESC
    """)
    List<Long> findPublicCreationIdsOrderByIdDesc();

    @EntityGraph(attributePaths = {"publishDays"})
    @Query("""
        SELECT DISTINCT c
        FROM Creation c
        WHERE c.id IN :ids
    """)
    List<Creation> findWithPublishDaysByIdIn(@Param("ids") List<Long> ids);

    @Query("""
        select
            c.id as creationId,
            c.title as title,
            c.plot as plot,
            cr.creatorName as creatorName,
            fo.storageKey as storageKey
        from Creation c
        join c.creator cr
        left join c.creationThumbnails ct
            on ct.type = :posterType and ct.displayOrder = 0
        left join ct.fileObject fo
        where c.id = :creationId
    """)
    Optional<CreationBaseProjection> findCreationDetailBase(
            @Param("creationId") Long creationId,
            @Param("posterType") CreationThumbnailType posterType
    );
}