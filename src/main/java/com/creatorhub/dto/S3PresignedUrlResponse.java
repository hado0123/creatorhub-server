package com.creatorhub.dto;

public record S3PresignedUrlResponse (
        Long fileObjectId,
        String uploadUrl,
        String objectKey
) { }
