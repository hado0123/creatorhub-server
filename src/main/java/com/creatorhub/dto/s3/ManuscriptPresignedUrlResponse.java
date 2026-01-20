package com.creatorhub.dto.s3;

public record ManuscriptPresignedUrlResponse(
        int order,
        Long fileObjectId,
        String presignedUrl,
        String storageKey
) {}