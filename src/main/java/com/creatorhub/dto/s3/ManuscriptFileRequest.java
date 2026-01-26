package com.creatorhub.dto.s3;

import jakarta.validation.constraints.*;

public record ManuscriptFileRequest (
        @NotNull(message = "displayOrder가 존재하지 않습니다.")
        @Min(value = 1, message = "displayOrder는 1 이상이어야 합니다.")
        Integer displayOrder,

        @NotBlank(message = "콘텐츠 타입이 존재하지 않습니다.")
        String contentType,

        @NotBlank(message = "원본 파일명이 존재하지 않습니다.")
        String originalFilename,

        @NotNull(message = "원고 파일 사이즈가 존재하지 않습니다.")
        @Positive(message = "원고 파일 사이즈는 1byte 이상이어야 합니다.")
        @Max(value = 5 *1024 * 1024, message = "원고 파일 사이즈는 1개당 5MB 이하여야 합니다.")
        Long sizeBytes
){ }