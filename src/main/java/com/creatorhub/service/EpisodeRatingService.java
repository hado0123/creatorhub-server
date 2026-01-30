package com.creatorhub.service;

import com.creatorhub.dto.EpisodeRatingResponse;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeRating;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.AlreadyEpisodeRatingException;
import com.creatorhub.exception.EpisodeNotFoundException;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.EpisodeRatingRepository;
import com.creatorhub.repository.EpisodeRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EpisodeRatingService {
    private final EpisodeRatingRepository episodeRatingRepository;
    private final EpisodeRepository episodeRepository;
    private final MemberRepository memberRepository;

    /**
     * 별점 등록
     */
    @Transactional
    public EpisodeRatingResponse rate(Long memberId, Long episodeId, int score) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(EpisodeNotFoundException::new);

        try {
            // 최초 등록만 허용 (DB UNIQUE로 보장)
            episodeRatingRepository.save(EpisodeRating.create(member, episode, score));
        } catch (DataIntegrityViolationException e) {
            // (member_id, episode_id) UNIQUE 위반
            throw new AlreadyEpisodeRatingException();
        }

        // 집계 갱신
        episodeRepository.addRating(episodeId, score);

        // 최신 회차 데이터 가져오기
        Episode latest = episodeRepository.findById(episodeId)
                .orElseThrow(EpisodeNotFoundException::new);

        Integer count = latest.getRatingCount() == null ? 0 : latest.getRatingCount();
        BigDecimal average = latest.getRatingAverage() == null ? BigDecimal.ZERO : latest.getRatingAverage();

        return EpisodeRatingResponse.of(episodeId, score, count, average);
    }
}
