package com.creatorhub.dto.creation;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.entity.Creation;
import com.creatorhub.entity.CreationThumbnail;

public record CreationListResponse(
        Long creationId,
        String title,
        String posterThumbnailUrl   // CloudFront URL
) {
    private static final String CLOUDFRONT_BASE = "https://d3tenc5c954qkn.cloudfront.net/";

    public static CreationListResponse from(Creation creation) {
        String posterUrl = creation.getCreationThumbnails().stream()
                .filter(t -> t.getType() == CreationThumbnailType.POSTER
                          && t.getDisplayOrder() == 0)
                .findFirst()
                .map(CreationThumbnail::getFileObject)
                .map(fo -> CLOUDFRONT_BASE + fo.getStorageKey())
                .orElse(null);

        return new CreationListResponse(
                creation.getId(),
                creation.getTitle(),
                posterUrl
        );
    }
}
