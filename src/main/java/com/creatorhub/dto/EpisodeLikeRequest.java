package com.creatorhub.dto;

import jakarta.validation.constraints.NotNull;

public record EpisodeLikeRequest(
        @NotNull(message = "episodeId가 존재하지 않습니다.")
        Long episodeId
) {
}
