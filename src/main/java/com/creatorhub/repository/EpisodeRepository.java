package com.creatorhub.repository;

import com.creatorhub.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    boolean existsByCreationIdAndEpisodeNum(Long creationId, Integer episodeNum);

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
    void updateLikeCountSafely(@Param("episodeId") Long episodeId,
                              @Param("delta") int delta);
}