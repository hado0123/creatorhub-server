package com.creatorhub.repository;

import com.creatorhub.entity.CreationFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreationFavoriteRepository extends JpaRepository<CreationFavorite, Long> {
    Page<CreationFavorite> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    int deleteByMemberIdAndCreationId(Long memberId, Long creationId);
    boolean existsByMemberIdAndCreationId(Long memberId, Long creationId);
}
