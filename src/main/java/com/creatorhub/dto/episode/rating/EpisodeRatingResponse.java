package com.creatorhub.dto.episode.rating;

import java.math.BigDecimal;

public record EpisodeRatingResponse(
        Long episodeId,
        Integer myScore,  // 내가 준 점수
        Integer ratingCount,
        BigDecimal ratingAvg
) {
    public static EpisodeRatingResponse of(Long episodeId, Integer myScore, Integer ratingCount, BigDecimal ratingAvg) {
        return new EpisodeRatingResponse(episodeId, myScore, ratingCount, ratingAvg);
    }
}
