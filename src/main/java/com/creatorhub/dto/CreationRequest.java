package com.creatorhub.dto;

import com.creatorhub.constant.CreationFormat;
import com.creatorhub.constant.CreationGenre;
import com.creatorhub.constant.PublishDay;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreationRequest(
        @NotNull(message = "creatorId가 존재하지 않습니다.")
        Long creatorId,

        @NotNull(message = "형식은 필수입니다.")
        CreationFormat format,

        @NotNull(message = "장르는 필수입니다.")
        CreationGenre genre,

        @NotBlank(message = "작품명은 필수입니다.")
        @Size(max = 30, message = "작품명은 30자 이하여야 합니다.")
        String title,

        @NotBlank(message = "줄거리는 필수입니다.")
        @Size(max = 400, message = "줄거리는 400자 이하여야 합니다.")
        String plot,

        @NotNull(message = "공개 여부는 필수입니다.")
        Boolean isPublic,

        @NotEmpty(message = "공개 여부는 필수입니다.")
        Set<PublishDay> publishDays,

        @NotEmpty(message = "해시태그는 최소 1개 이상 선택해야 합니다.")
        Set<Long> hashtagIds,

        @NotNull(message = "가로형 썸네일의 FileObjectId가 존재하지 않습니다.")
        Long horizontalOriginalFileObjectId,

        @NotNull(message = "포스터형 썸네일의 FileObjectId가 존재하지 않습니다.")
        Long posterOriginalFileObjectId
) { }