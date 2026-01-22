package com.creatorhub.dto.creation;

import jakarta.validation.constraints.NotNull;

public record CreationFavoriteRequest(
        @NotNull(message = "creationId가 존재하지 않습니다.")
        Long creationId
) {
}
