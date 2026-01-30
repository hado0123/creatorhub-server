package com.creatorhub.dto;

import java.math.BigDecimal;

public record EpisodeRatingResponse(
        Long episodeId,
        Integer myScore,  // 내가 준 점수
        Integer ratingCount,
        BigDecimal ratingAverage
) {
    public static EpisodeRatingResponse of(Long episodeId, Integer myScore, Integer ratingCount, BigDecimal ratingAverage) {
        return new EpisodeRatingResponse(episodeId, myScore, ratingCount, ratingAverage);
    }
}
