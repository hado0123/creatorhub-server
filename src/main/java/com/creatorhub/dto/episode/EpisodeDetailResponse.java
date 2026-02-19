package com.creatorhub.dto.episode;

import com.creatorhub.repository.projection.EpisodeMetaProjection;

import java.math.BigDecimal;
import java.util.List;

public record EpisodeDetailResponse(
        Long episodeId,
        Integer episodeNum,
        String title,
        Integer likeCount,
        BigDecimal ratingAverage,
        Integer ratingCount,
        List<String> manuscriptImageUrls
) {

    public static EpisodeDetailResponse from(
            EpisodeMetaProjection p,
            String cdnBase,
            List<String> storageKeys
    ) {

        List<String> urls = storageKeys.stream()
                .map(key -> cdnBase + "/" + key)
                .toList();

        return new EpisodeDetailResponse(
                p.getEpisodeId(),
                p.getEpisodeNum(),
                p.getTitle(),
                p.getLikeCount(),
                p.getRatingAverage(),
                p.getRatingCount(),
                urls
        );
    }
}
