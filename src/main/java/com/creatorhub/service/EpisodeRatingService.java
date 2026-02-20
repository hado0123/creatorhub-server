package com.creatorhub.service;

import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeRating;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.episode.rating.AlreadyEpisodeRatingException;
import com.creatorhub.exception.episode.EpisodeNotFoundException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.EpisodeRatingRepository;
import com.creatorhub.repository.EpisodeRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EpisodeRatingService {
    private final EpisodeRatingRepository episodeRatingRepository;
    private final EpisodeRepository episodeRepository;
    private final MemberRepository memberRepository;

    /**
     * (회차별) 별점 등록
     */
    @Transactional
    public void rate(Long memberId, Long episodeId, int score) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(EpisodeNotFoundException::new);

        // 최초 등록만 허용 (memberId, episodeId 유니크로 보장)
        try {
            // flush로 insert를 즉시 실행해 유니크(중복) 실패를 여기서 확정
            episodeRatingRepository.saveAndFlush(EpisodeRating.create(member, episode, score));
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyEpisodeRatingException();
        }

        // 집계 갱신
        episodeRepository.addRating(episodeId, score);
    }
}
