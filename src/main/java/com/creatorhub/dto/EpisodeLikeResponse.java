package com.creatorhub.dto;

public record EpisodeLikeResponse(
        Long episodeId,
        Integer likeCount,
        boolean liked
) {
    public static EpisodeLikeResponse of(Long episodeId, Integer likeCount, boolean liked) {
        return new EpisodeLikeResponse(episodeId, likeCount, liked);
    }
}