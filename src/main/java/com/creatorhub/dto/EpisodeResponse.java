package com.creatorhub.dto;

import java.util.List;

public record EpisodeResponse(
        Long episodeId,
        Integer episodeNum,
        List<Long> manuscriptImageIds,
        List<Long> episodeThumbnailIds
) { }

