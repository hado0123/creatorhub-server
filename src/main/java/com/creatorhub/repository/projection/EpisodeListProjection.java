package com.creatorhub.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EpisodeListProjection {
    Long getId();
    Integer getEpisodeNum();
    String getTitle();
    String getCreatorNote();
    Boolean getIsCommentEnabled();
    Boolean getIsPublic();
    Integer getLikeCount();
    Integer getFavoriteCount();
    Integer getRatingCount();
    BigDecimal getRatingAverage();
    String getStorageKey();
    LocalDateTime getCreatedAt();
}
