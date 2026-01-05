package com.creatorhub.repository;

import com.creatorhub.entity.Creation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreationRepository  extends JpaRepository<Creation, Long> {
}
