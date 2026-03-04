package com.creatorhub.dto.creation;

import com.creatorhub.repository.projection.CreationBaseProjection;

import java.util.List;

public record CreationResponse(
        Long creationId,
        String title,
        String plot,
        String posterThumbnailUrl,
        String creatorName,
        List<String> hashtags
) {
    public static CreationResponse from(
            CreationBaseProjection p,
            String cdnBase,
            List<String> hashtags
    ) {
        String url = p.getStorageKey() == null
                ? null
                : cdnBase + "/" + p.getStorageKey();

        return new CreationResponse(
                p.getCreationId(),
                p.getTitle(),
                p.getPlot(),
                url,
                p.getCreatorName(),
                hashtags
        );
    }
}
