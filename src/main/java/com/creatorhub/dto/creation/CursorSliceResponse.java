package com.creatorhub.dto.creation;

import java.util.List;

public record CursorSliceResponse<T>(
        List<T> items,
        boolean hasNext,
        SeekCursor nextCursor
) {}
