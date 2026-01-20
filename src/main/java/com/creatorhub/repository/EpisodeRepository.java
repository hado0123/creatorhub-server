package com.creatorhub.repository;

import com.creatorhub.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    boolean existsByCreationIdAndEpisodeNum(Long creationId, Integer episodeNum);
}