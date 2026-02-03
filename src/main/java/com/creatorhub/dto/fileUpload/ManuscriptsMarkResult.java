package com.creatorhub.dto.fileUpload;

import java.util.List;

public record ManuscriptsMarkResult(
        int total,
        int readyCount,
        int failedCount,
        List<FailedItem> failedItems
) {
    public record FailedItem(Long fileObjectId, long sizeBytes, long maxBytes) {}
}