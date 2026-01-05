package com.creatorhub.dto;

import com.creatorhub.constant.CreationThumbnailType;

public record S3PresignedUrlRequest(
        String contentType,
        CreationThumbnailType thumbnailType,
        String originalFilename
) { }
