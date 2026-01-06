package com.creatorhub.repository;

import com.creatorhub.entity.EpisodeLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodeLikeRepository extends JpaRepository<EpisodeLike, Long> {

    boolean existsByMemberIdAndEpisodeId(Long memberId, Long episodeId);

    Optional<EpisodeLike> findByMemberIdAndEpisodeId(Long memberId, Long episodeId);
}
