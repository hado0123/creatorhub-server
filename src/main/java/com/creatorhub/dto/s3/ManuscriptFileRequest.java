package com.creatorhub.dto.s3;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManuscriptFileRequest (
        @NotNull(message = "displayOrder가 존재하지 않습니다.")
        @Min(value = 1, message = "displayOrder는 1 이상이어야 합니다.")
        Integer displayOrder,

        @NotBlank(message = "콘텐츠 타입이 존재하지 않습니다.")
        String contentType,

        @NotBlank(message = "원본 파일명이 존재하지 않습니다.")
        String originalFilename
){ }