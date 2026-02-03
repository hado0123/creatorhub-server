package com.creatorhub.dto.s3;

import jakarta.validation.constraints.*;

import java.util.List;

public record ResizeCompleteRequest(
        @NotBlank(message = "bucket이 존재하지 않습니다.")
        String bucket,

        @NotBlank(message = "triggerKey가 존재하지 않습니다.")
        String triggerKey,

        @NotBlank(message = "baseKey가 존재하지 않습니다.")
        String baseKey,

        @NotEmpty(message = "derivedKeys(리사이징 이미지 key)가 비어있습니다.")
        List<DerivedFile> derivedFiles,

        @NotBlank(message = "resizedAt이 존재하지 않습니다.")
        String resizedAt
) {
    public record DerivedFile(
            @NotBlank String key,
            @NotNull Integer width,
            @NotNull Integer height,
            @NotNull Long sizeBytes
    ) {}
}