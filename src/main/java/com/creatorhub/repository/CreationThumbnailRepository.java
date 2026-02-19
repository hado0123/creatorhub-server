package com.creatorhub.repository;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.entity.CreationThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreationThumbnailRepository extends JpaRepository<CreationThumbnail, Long> {

    @Query("""
        select ct
        from CreationThumbnail ct
        join fetch ct.fileObject fo
        join fetch ct.creation c
        where c.id in :ids
          and ct.type = :type
    """)
    List<CreationThumbnail> findByCreationIdsAndTypeAndSizeType(
            @Param("ids") List<Long> ids,
            @Param("type") CreationThumbnailType type
    );
}