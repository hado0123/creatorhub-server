package com.creatorhub.dto.episode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record EpisodeRequest(
        @NotNull(message = "creationId가 존재하지 않습니다.")
        Long creationId,

        @NotNull(message = "episodeNum이 존재하지 않습니다.")
        @Min(value = 1, message = "episodeNum은 1 이상이어야 합니다.")
        Integer episodeNum,

        @NotBlank(message = "title이 존재하지 않습니다.")
        @Size(max = 35, message = "title은 35자 이하여야 합니다.")
        String title,

        @NotBlank(message = "creatorNote가 존재하지 않습니다.")
        @Size(max = 100, message = "creatorNote는 100자 이하여야 합니다.")
        String creatorNote,

        Boolean isCommentEnabled,
        Boolean isPublic,

        @NotNull(message = "회차 썸네일의 FileObjectId가 존재하지 않습니다.")
        Long episodeFileObjectId,

        @NotNull(message = "sns 썸네일의 FileObjectId가 존재하지 않습니다.")
        Long snsFileObjectId,

        @NotEmpty(message = "원고 파일 목록이 비어있습니다.")
        @Size(max = 200, message = "원고는 200장 이하여야 합니다.") // 정책값
                List<@Valid ManuscriptRegisterItem> manuscripts

        ) { }