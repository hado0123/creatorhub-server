package com.creatorhub.dto.s3;

public record ManuscriptPresignedUrlResponse(
        int displayOrder,
        Long fileObjectId,
        String presignedUrl,
        String storageKey
) {}