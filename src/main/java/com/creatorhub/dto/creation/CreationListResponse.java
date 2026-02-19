package com.creatorhub.dto.creation;

import com.creatorhub.repository.projection.CreationListProjection;

public record CreationListResponse(
        Long creationId,
        String title,
        String posterThumbnailUrl
) {
    public static CreationListResponse from(
            CreationListProjection p,
            String cdnBase
    ) {
        String url = p.getStorageKey() == null
                ? null
                : cdnBase + "/" + p.getStorageKey();

        return new CreationListResponse(
                p.getCreationId(),
                p.getTitle(),
                url
        );
    }
}
