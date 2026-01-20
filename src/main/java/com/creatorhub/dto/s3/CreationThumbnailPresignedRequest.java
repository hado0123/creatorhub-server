package com.creatorhub.dto.s3;

import com.creatorhub.constant.CreationThumbnailType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreationThumbnailPresignedRequest(

        @NotBlank(message = "콘텐츠 타입이 존재하지 않습니다.")
        String contentType,

        @NotNull(message = "썸네일 타입이 존재하지 않습니다.")
        CreationThumbnailType thumbnailType,

        @NotBlank(message = "원본 파일명이 존재하지 않습니다.")
        String originalFilename

        ) implements PresignedPutRequest {
    @Override public String resolveSuffix() {
        return thumbnailType.resolveSuffix();
    }
}

