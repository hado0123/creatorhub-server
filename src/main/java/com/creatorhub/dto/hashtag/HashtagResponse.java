package com.creatorhub.dto.hashtag;

import com.creatorhub.entity.Hashtag;

public record HashtagResponse(
        Long id,
        String title
) {
    public static HashtagResponse from(Hashtag hashtag) {
        return new HashtagResponse(hashtag.getId(), hashtag.getTitle());
    }
}
