package com.creatorhub.repository;

import com.creatorhub.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    List<Hashtag> findByIdIn(Collection<Long> ids);
}