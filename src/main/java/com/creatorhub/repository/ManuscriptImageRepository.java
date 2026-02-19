package com.creatorhub.repository;

import com.creatorhub.entity.ManuscriptImage;
import com.creatorhub.repository.projection.ManuscriptRowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManuscriptImageRepository extends JpaRepository<ManuscriptImage, Long> {
    @Query("""
        select
            mi.displayOrder as displayOrder,
            fo.storageKey as storageKey
        from ManuscriptImage mi
        join mi.fileObject fo
        where mi.episode.id = :episodeId
          and mi.episode.creation.id = :creationId
        order by mi.displayOrder asc
    """)
    List<ManuscriptRowProjection> findManuscripts(
            @Param("creationId") Long creationId,
            @Param("episodeId") Long episodeId
    );
}