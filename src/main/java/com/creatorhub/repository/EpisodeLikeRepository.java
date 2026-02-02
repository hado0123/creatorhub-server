package com.creatorhub.repository;

import com.creatorhub.entity.EpisodeLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeLikeRepository extends JpaRepository<EpisodeLike, Long> {
    int deleteByMemberIdAndEpisodeId(Long memberId, Long episodeId);
}
