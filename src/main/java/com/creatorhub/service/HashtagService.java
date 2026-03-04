package com.creatorhub.service;

import com.creatorhub.dto.hashtag.HashtagResponse;
import com.creatorhub.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagService {

    private final HashtagRepository hashtagRepository;

    /**
     * 전체 해시태그 목록 조회
     */
    public List<HashtagResponse> getAllHashtags() {
        return hashtagRepository.findAll().stream()
                .map(HashtagResponse::from)
                .toList();
    }
}
