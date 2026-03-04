package com.creatorhub.repository;

import com.creatorhub.entity.ManuscriptImage;
import com.creatorhub.repository.projection.ManuscriptRowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManuscriptImageRepository extends JpaRepository<ManuscriptImage, Long> {
    @Query("""
        SELECT
            mi.displayOrder AS displayOrder,
            fo.storageKey AS storageKey
        FROM ManuscriptImage mi
        JOIN mi.fileObject fo
        WHERE mi.episode.id = :episodeId
          AND mi.episode.creation.id = :creationId
        ORDER BY mi.displayOrder ASC
    """)
    List<ManuscriptRowProjection> findManuscripts(
            @Param("creationId") Long creationId,
            @Param("episodeId") Long episodeId
    );
}