package com.creatorhub.service;

import com.creatorhub.dto.episode.like.EpisodeLikeResponse;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeLike;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.episode.EpisodeNotFoundException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.EpisodeLikeRepository;
import com.creatorhub.repository.EpisodeRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EpisodeLikeService {
    private final EpisodeLikeRepository episodeLikeRepository;
    private final EpisodeRepository episodeRepository;
    private final MemberRepository memberRepository;
    private final CreationRepository creationRepository;

    /**
     * (회차별) 좋아요
     */
    @Transactional
    public EpisodeLikeResponse like(Long memberId, Long episodeId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(EpisodeNotFoundException::new);

        // 유니크 + 예외로 멱등 처리
        try {
            // flush로 insert를 즉시 실행해 유니크(중복) 실패를 여기서 확정
            episodeLikeRepository.saveAndFlush(EpisodeLike.like(member, episode));
        } catch (DataIntegrityViolationException e) {
            return EpisodeLikeResponse.of(episodeId, true);
        }

        episodeRepository.updateLikeCount(episodeId, 1);
        creationRepository.updateTotalLikeCount(episode.getCreation().getId(), 1);

        return EpisodeLikeResponse.of(episodeId, true);
    }

    /**
     * (회차별) 좋아요 여부 조회
     */
    public EpisodeLikeResponse getLikeStatus(Long memberId, Long episodeId) {
        boolean liked = episodeLikeRepository.existsByMemberIdAndEpisodeId(memberId, episodeId);
        return EpisodeLikeResponse.of(episodeId, liked);
    }

    /**
     * (회차별) 좋아요 해제
     */
    @Transactional
    public EpisodeLikeResponse unlike(Long memberId, Long episodeId) {
        // delete row count 기반 멱등 처리 (동시성에서도 count 정합성 보장)
        int deleted = episodeLikeRepository
                .deleteByMemberIdAndEpisodeId(memberId, episodeId);

        if (deleted > 0) {
            Episode episode = episodeRepository.findById(episodeId)
                    .orElseThrow(EpisodeNotFoundException::new);
            episodeRepository.updateLikeCount(episodeId, -1);
            creationRepository.updateTotalLikeCount(episode.getCreation().getId(), -1);
        }

        return EpisodeLikeResponse.of(episodeId, false);
    }
}
