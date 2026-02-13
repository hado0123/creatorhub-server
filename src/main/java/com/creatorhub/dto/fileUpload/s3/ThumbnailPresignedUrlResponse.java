package com.creatorhub.dto.fileUpload.s3;

public record ThumbnailPresignedUrlResponse(
        Long fileObjectId,
        String uploadUrl,
        String storageKey
) { }
