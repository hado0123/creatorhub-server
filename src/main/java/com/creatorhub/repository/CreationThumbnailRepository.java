package com.creatorhub.repository;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.entity.CreationThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreationThumbnailRepository extends JpaRepository<CreationThumbnail, Long> {

    @Query("""
        SELECT ct
        FROM CreationThumbnail ct
        JOIN FETCH ct.fileObject fo
        JOIN FETCH ct.creation c
        WHERE c.id IN :ids
          AND ct.type = :type
    """)
    List<CreationThumbnail> findPostersByCreationIds(
            @Param("ids") List<Long> ids,
            @Param("type") CreationThumbnailType type
    );
}