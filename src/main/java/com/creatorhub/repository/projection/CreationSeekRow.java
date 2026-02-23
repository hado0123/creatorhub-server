package com.creatorhub.repository.projection;

public interface CreationSeekRow {
    Long getId();
    Long getLongValue();      // views/likes 합계용
    Double getDoubleValue();  // rating avg 용
    Long getTie();            // ratingCount 합계용
}
