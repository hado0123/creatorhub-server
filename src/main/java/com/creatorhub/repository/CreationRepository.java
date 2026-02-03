package com.creatorhub.repository;

import com.creatorhub.entity.Creation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreationRepository extends JpaRepository<Creation, Long> {

    // JPA 엔티티 방식으로 작업시 읽기 -> 계산 -> 쓰기 방식으로 진행되기 때문에 동시 요청시 favoriteCount값이 -1이 될 수 있음
    // 이를 방지하기 위해 UPDATE 쿼리에서 증감 연산을 수행해 DB에서 원자성을 보장하도록 구현
    // 동시에 favoriteCount값이 0 아래로 더 이상 감소되지 않도록 처리
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
    void updateFavoriteCount(@Param("creationId") Long creationId,
                                  @Param("delta") int delta);

}

