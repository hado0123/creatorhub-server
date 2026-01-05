package com.creatorhub.constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ThumbnailKeys {

    // 작품 썸네일 poster형
    public static final String POSTER_SUFFIX = "_480x623.jpg";

    // 작품 썸네일 가로형(리사이징 원본), Lambda 트리거 suffix
    public static final String HORIZONTAL_SUFFIX = "_434x330.jpg";

    // 리사이징 6개
    public static final List<String> DERIVED_SUFFIXES = List.of(
            "_83x90.jpg",
            "_98x79.jpg",
            "_125x101.jpg",
            "_202x164.jpg",
            "_217x165.jpg",
            "_218x120.jpg"
    );

    public static List<String> allSuffixes() {
        List<String> all = new ArrayList<>();
        all.add(HORIZONTAL_SUFFIX);
        all.addAll(DERIVED_SUFFIXES);
        return Collections.unmodifiableList(all);
    }
}
