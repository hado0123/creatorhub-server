package com.creatorhub.dto.fileUpload;


public record ThumbnailMarkResult(
        Long fileObjectId,
        boolean ready,
        long sizeBytes,
        long maxBytes
) { }
