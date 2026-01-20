package com.creatorhub.dto.s3;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ManuscriptPresignedRequest(

        @NotNull(message = "creatorId가 존재하지 않습니다.")
        Long creationId,

        @NotEmpty(message = "원고 파일 목록이 비어있습니다.")
        @Size(max = 50, message = "원고는 50장 이하여야 합니다.")
        List<@Valid ManuscriptFileRequest> files

) { }

