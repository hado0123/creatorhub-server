package com.creatorhub.dto.fileUpload.s3;

public record ManuscriptPresignedUrlResponse(
        int displayOrder,
        Long fileObjectId,
        String presignedUrl,
        String storageKey
) {}