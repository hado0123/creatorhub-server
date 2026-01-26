package com.creatorhub.dto.s3;

import com.creatorhub.constant.CreationThumbnailType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreationThumbnailPresignedRequest(

        @NotBlank(message = "콘텐츠 타입이 존재하지 않습니다.")
        String contentType,

        @NotNull(message = "썸네일 타입이 존재하지 않습니다.")
        CreationThumbnailType thumbnailType,

        @NotBlank(message = "원본 파일명이 존재하지 않습니다.")
        String originalFilename,

        @NotNull(message = "썸네일 파일 사이즈가 존재하지 않습니다.")
        @Positive(message = "썸네일 파일 사이즈는 1byte 이상이어야 합니다.")
        @Max(value = 1024 * 1024, message = "썸네일 파일 사이즈는 1MB 이하여야 합니다.")
        Long sizeBytes

        ) implements PresignedPutRequest {
    @Override public String resolveSuffix() {
        return thumbnailType.resolveSuffix();
    }
}

