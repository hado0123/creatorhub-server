package com.creatorhub.dto.s3;

public record ThumbnailPresignedUrlResponse(
        Long fileObjectId,
        String uploadUrl,
        String objectKey
) { }
