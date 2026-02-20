package com.creatorhub.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EpisodeListProjection {
    Long getId();
    Integer getEpisodeNum();
    String getTitle();
    Boolean getIsPublic();
    BigDecimal getRatingAverage();
    String getStorageKey();
    LocalDateTime getCreatedAt();
}
