package com.creatorhub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EpisodeRatingRequest(
        @NotNull(message = "episodeId가 존재하지 않습니다.")
        Long episodeId,

        @NotNull(message = "별점을 주세요. ")
        @Min(value = 1, message = "score는 1 이상이어야 합니다.")
        @Max(value = 5, message = "score는 5 이하여야 합니다.")
        Integer score
) {
}