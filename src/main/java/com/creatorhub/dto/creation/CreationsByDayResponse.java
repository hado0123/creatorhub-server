package com.creatorhub.dto.creation;

import com.creatorhub.constant.PublishDay;

import java.util.List;
import java.util.Map;

public record CreationsByDayResponse(
        Map<PublishDay, List<CreationByDayItem>> webtoonsByDay
) {
    public record CreationByDayItem(
            Long creationId,
            String title,
            String posterThumbnailUrl
    ) { }
}