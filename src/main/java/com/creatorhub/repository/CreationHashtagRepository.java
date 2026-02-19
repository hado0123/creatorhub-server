package com.creatorhub.repository;

import com.creatorhub.entity.CreationHashtag;
import com.creatorhub.repository.projection.HashtagTitleProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreationHashtagRepository extends JpaRepository<CreationHashtag, Long> {

    @Query("""
        select h.title as title
        from CreationHashtag ch
        join ch.hashtag h
        where ch.creation.id = :creationId
        order by h.title asc
    """)
    List<HashtagTitleProjection> findHashtagTitlesByCreationId(@Param("creationId") Long creationId);
}
