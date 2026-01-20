package com.creatorhub.dto;

import jakarta.validation.constraints.*;

public record ManuscriptRegisterItem(
        @NotNull(message = "fileObjectId가 존재하지 않습니다.")
        Long fileObjectId,

        @NotNull(message = "displayOrder가 존재하지 않습니다.")
        @Min(value = 1, message = "displayOrder는 1 이상이어야 합니다.")
        Integer displayOrder
) { }
