package com.creatorhub.service;

import com.creatorhub.dto.episode.like.EpisodeLikeResponse;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeLike;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.creator.AlreadyEpisodeLikeException;
import com.creatorhub.exception.creator.EpisodeLikeNotFoundException;
import com.creatorhub.exception.creator.EpisodeNotFoundException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.EpisodeLikeRepository;
import com.creatorhub.repository.EpisodeRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EpisodeLikeService {
    private final EpisodeLikeRepository episodeLikeRepository;
    private final EpisodeRepository episodeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public EpisodeLikeResponse like(Long memberId, Long episodeId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(EpisodeNotFoundException::new);

        // 이미 좋아요
        if (episodeLikeRepository.existsByMemberIdAndEpisodeId(memberId, episodeId)) {
            throw new AlreadyEpisodeLikeException();
        }

        episodeLikeRepository.save(EpisodeLike.like(member, episode));

        episodeRepository.updateLikeCountSafely(episodeId, +1);

        Integer newCount = episodeRepository.findById(episodeId)
                .map(ep -> ep.getLikeCount() == null ? 0 : ep.getLikeCount())
                .orElse(0);

        return EpisodeLikeResponse.of(episodeId, newCount, true);
    }

    @Transactional
    public EpisodeLikeResponse unlike(Long memberId, Long episodeId) {
        EpisodeLike like = episodeLikeRepository.findByMemberIdAndEpisodeId(memberId, episodeId)
                .orElseThrow(EpisodeLikeNotFoundException::new);

        episodeLikeRepository.delete(like);

        episodeRepository.updateLikeCountSafely(episodeId, -1);

        Integer newCount = episodeRepository.findById(episodeId)
                .map(ep -> ep.getLikeCount() == null ? 0 : ep.getLikeCount())
                .orElse(0);

        return EpisodeLikeResponse.of(episodeId, newCount, false);
    }
}
