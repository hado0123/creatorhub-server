package com.creatorhub.dto;

import com.creatorhub.entity.Creator;

public record CreatorResponse(
        Long id,
        Long memberId,
        String creatorName,
        String introduction
) {
    public static CreatorResponse from(Creator creator) {
        return new CreatorResponse(
                creator.getId(),
                creator.getMember().getId(),
                creator.getCreatorName(),
                creator.getIntroduction()
        );
    }
}