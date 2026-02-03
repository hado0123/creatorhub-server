package com.creatorhub.dto.episode.like;

public record EpisodeLikeResponse(
        Long episodeId,
        boolean liked
) {
    public static EpisodeLikeResponse of(Long episodeId, boolean liked) {
        return new EpisodeLikeResponse(episodeId, liked);
    }
}