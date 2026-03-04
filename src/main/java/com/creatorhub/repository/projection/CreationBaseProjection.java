package com.creatorhub.repository.projection;

public interface CreationBaseProjection {
    Long getCreationId();
    String getTitle();
    String getPlot();
    String getCreatorName();
    String getStorageKey();
}