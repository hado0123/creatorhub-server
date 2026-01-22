package com.creatorhub.dto.creation.favorite;

import java.time.LocalDateTime;

public record FavoriteCreationItem(
        Long creationId,
        String title,
        String plot,
        boolean isPublic,
        Integer favoriteCount,
        LocalDateTime favoritedAt
) {
}