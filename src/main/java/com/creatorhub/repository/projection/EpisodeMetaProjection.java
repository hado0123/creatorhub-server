package com.creatorhub.repository.projection;

import java.math.BigDecimal;

public interface EpisodeMetaProjection {
    Long getEpisodeId();
    Integer getEpisodeNum();
    String getTitle();
    Integer getLikeCount();
    BigDecimal getRatingAverage();
    Integer getRatingCount();
}
