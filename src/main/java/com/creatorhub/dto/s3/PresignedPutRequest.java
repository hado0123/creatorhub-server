package com.creatorhub.dto.s3;

public interface PresignedPutRequest {
    String contentType();
    String originalFilename();
    String resolveSuffix(); // 업로드 키에 붙을 suffix
}

