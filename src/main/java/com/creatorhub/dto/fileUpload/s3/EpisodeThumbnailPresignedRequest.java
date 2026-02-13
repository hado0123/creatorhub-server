package com.creatorhub.dto.fileUpload.s3;

import com.creatorhub.constant.EpisodeThumbnailType;
import com.creatorhub.common.validation.annotation.ValidThumbnailSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EpisodeThumbnailPresignedRequest(

        @NotBlank(message = "콘텐츠 타입이 존재하지 않습니다.")
        String contentType,

        @NotNull(message = "썸네일 타입이 존재하지 않습니다.")
        EpisodeThumbnailType thumbnailType,

        @NotBlank(message = "원본 파일명이 존재하지 않습니다.")
        String originalFilename,

        @NotNull(message = "썸네일 파일 사이즈가 존재하지 않습니다.")
        @Positive(message = "썸네일 파일 사이즈는 1byte 이상이어야 합니다.")
        @ValidThumbnailSize
        Long sizeBytes

) implements PresignedPutRequest {
    @Override public String resolveSuffix() {
        return thumbnailType.resolveSuffix();
    }
}

