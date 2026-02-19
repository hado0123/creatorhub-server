package com.creatorhub.dto.episode;

import com.creatorhub.repository.projection.EpisodeListProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EpisodeListResponse(
        Long episodeId,
        Integer episodeNum,
        String title,
        String creatorNote,
        boolean isCommentEnabled,
        boolean isPublic,
        Integer likeCount,
        Integer favoriteCount,
        Integer ratingCount,
        BigDecimal ratingAverage,
        String episodeThumbnailUrl,
        LocalDateTime createdAt
) {
    public static EpisodeListResponse from(
            EpisodeListProjection p,
            String cdnBase
    ) {
        String url = p.getStorageKey() == null
                ? null
                : cdnBase + "/" + p.getStorageKey();

        return new EpisodeListResponse(
                p.getId(),
                p.getEpisodeNum(),
                p.getTitle(),
                p.getCreatorNote(),
                p.getIsCommentEnabled(),
                p.getIsPublic(),
                p.getLikeCount(),
                p.getFavoriteCount(),
                p.getRatingCount(),
                p.getRatingAverage(),
                url,
                p.getCreatedAt()
        );
    }
}
