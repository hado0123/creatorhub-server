package com.creatorhub.dto.episode.rating;

import java.math.BigDecimal;

public record EpisodeRatingStatusResponse(
        Long episodeId,
        boolean rated,
        Integer score,          // 등록하지 않은 경우 null
        BigDecimal ratingAverage,
        Integer ratingCount
) {
    public static EpisodeRatingStatusResponse of(Long episodeId, boolean rated, Integer score) {
        return new EpisodeRatingStatusResponse(episodeId, rated, score, null, null);
    }

    public static EpisodeRatingStatusResponse of(Long episodeId, boolean rated, Integer score,
                                                  BigDecimal ratingAverage, Integer ratingCount) {
        return new EpisodeRatingStatusResponse(episodeId, rated, score, ratingAverage, ratingCount);
    }
}
