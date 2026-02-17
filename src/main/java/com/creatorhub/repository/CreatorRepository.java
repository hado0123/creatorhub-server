package com.creatorhub.repository;


import com.creatorhub.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
    boolean existsByMemberId(Long memberId);
    Optional<Creator> findByMemberId(Long id);
}
