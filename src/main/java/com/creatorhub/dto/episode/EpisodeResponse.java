package com.creatorhub.dto.episode;

import java.util.List;

public record EpisodeResponse(
        Long episodeId,
        Integer episodeNum,
        List<Long> manuscriptImageIds,
        List<Long> episodeThumbnailIds
) { }

