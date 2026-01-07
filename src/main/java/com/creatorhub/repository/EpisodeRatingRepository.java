package com.creatorhub.repository;

import com.creatorhub.entity.EpisodeRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodeRatingRepository extends JpaRepository<EpisodeRating, Long> {

    Optional<EpisodeRating> findByMemberIdAndEpisodeId(Long memberId, Long episodeId);

    boolean existsByMemberIdAndEpisodeId(Long memberId, Long episodeId);
}