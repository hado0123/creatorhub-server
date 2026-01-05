package com.creatorhub.repository;

import com.creatorhub.entity.CreationFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreationFavoriteRepository extends JpaRepository<CreationFavorite, Long> {

    Optional<CreationFavorite> findByMemberIdAndCreationId(Long memberId, Long creationId);

    boolean existsByMemberIdAndCreationId(Long memberId, Long creationId);

    Page<CreationFavorite> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
