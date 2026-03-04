package com.creatorhub.repository;

import com.creatorhub.entity.CreationHashtag;
import com.creatorhub.repository.projection.HashtagTitleProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreationHashtagRepository extends JpaRepository<CreationHashtag, Long> {

    @Query("""
        SELECT h.title AS title
        FROM CreationHashtag ch
        JOIN ch.hashtag h
        WHERE ch.creation.id = :creationId
        ORDER BY h.title ASC
    """)
    List<HashtagTitleProjection> findHashtagTitlesByCreationId(@Param("creationId") Long creationId);
}
