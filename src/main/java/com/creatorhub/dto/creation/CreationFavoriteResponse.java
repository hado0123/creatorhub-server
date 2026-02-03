package com.creatorhub.dto.creation;

public record CreationFavoriteResponse(
        Long creationId,
        boolean favorited
) {
    public static CreationFavoriteResponse of(
            Long creationId,
            boolean favorited
    ) {
        return new CreationFavoriteResponse(creationId, favorited);
    }
}
