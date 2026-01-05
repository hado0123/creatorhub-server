package com.creatorhub.repository;

import com.creatorhub.entity.Creation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreationRepository extends JpaRepository<Creation, Long> {

    // favoriteCount값이 0 아래로 더 이상 감소되지 않도록 처리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Creation c
           SET c.favoriteCount =
               CASE
                   WHEN :delta < 0 AND COALESCE(c.favoriteCount, 0) <= 0
                       THEN 0
                   ELSE COALESCE(c.favoriteCount, 0) + :delta
               END
         WHERE c.id = :creationId
    """)
    void updateFavoriteCountSafely(@Param("creationId") Long creationId,
                                  @Param("delta") int delta);
}

