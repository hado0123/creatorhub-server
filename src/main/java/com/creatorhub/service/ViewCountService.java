package com.creatorhub.service;

import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final EpisodeRepository episodeRepository;
    private final CreationRepository creationRepository;

    /**
     * 조회수 증가를 비동기로 처리
     */
    @Async("viewCountExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementAsync(Long episodeId, Long creationId) {
        episodeRepository.incrementViewCount(episodeId);
        creationRepository.incrementTotalViewCount(creationId);
    }
}
