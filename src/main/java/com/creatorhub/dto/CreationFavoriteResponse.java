package com.creatorhub.dto;

public record CreationFavoriteResponse(
        Long creationId,
        Integer favoriteCount,
        boolean favorited,
        boolean changed
) {
    public static CreationFavoriteResponse of(
            Long creationId,
            Integer favoriteCount,
            boolean favorited,
            boolean changed
    ) {
        return new CreationFavoriteResponse(creationId, favoriteCount, favorited, changed);
    }
}
