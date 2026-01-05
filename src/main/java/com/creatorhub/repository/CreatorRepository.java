package com.creatorhub.repository;


import com.creatorhub.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
    boolean existsByMemberId(Long memberId);
}
