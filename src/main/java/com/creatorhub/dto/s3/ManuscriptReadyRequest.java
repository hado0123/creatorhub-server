package com.creatorhub.dto.s3;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ManuscriptReadyRequest(
        @NotEmpty(message = "fileObjectIds가 비어있습니다.")
        @Size(max = 50, message = "fileObjectIds는 50개 이하여야 합니다.")
        List<Long> fileObjectIds
) {}