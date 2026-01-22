package com.creatorhub.dto.creation;

public record CreationFavoriteResponse(
        Long creationId,
        Integer favoriteCount,
        boolean favorited
) {
    public static CreationFavoriteResponse of(Long creationId, Integer favoriteCount, boolean favorited) {
        return new CreationFavoriteResponse(creationId, favoriteCount, favorited);
    }
}
