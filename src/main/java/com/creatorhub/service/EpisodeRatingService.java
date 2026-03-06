package com.creatorhub.service;

import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeRating;
import com.creatorhub.entity.Member;
import com.creatorhub.dto.episode.rating.EpisodeRatingStatusResponse;
import com.creatorhub.exception.episode.rating.AlreadyEpisodeRatingException;
import com.creatorhub.exception.episode.EpisodeNotFoundException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.CreationRepository;
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
    private final CreationRepository creationRepository;

    /**
     * (회차별) 별점 등록 - 등록 후 갱신된 평균/개수 반환
     */
    @Transactional
    public EpisodeRatingStatusResponse rate(Long memberId, Long episodeId, int score) {
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
        creationRepository.addTotalRating(episode.getCreation().getId(), score);

        // 갱신된 평균/개수 조회해서 반환
        // 레이스 컨디션 우려: 클라이언트에 보여주는건 내 점수가 아니라 현재 평균이므로 최신 평균을 보여주는게 오히려 더 정확
        var meta = episodeRepository.findEpisodeMeta(episode.getCreation().getId(), episodeId)
                .orElseThrow(EpisodeNotFoundException::new);

        return EpisodeRatingStatusResponse.of(episodeId, true, score,
                meta.getRatingAverage(), meta.getRatingCount());
    }

    /**
     * (회차별) 내 평점 조회
     */
    public EpisodeRatingStatusResponse getRatingStatus(Long memberId, Long episodeId) {
        return episodeRatingRepository.findByMemberIdAndEpisodeId(memberId, episodeId)
                .map(r -> EpisodeRatingStatusResponse.of(episodeId, true, r.getScore()))
                .orElse(EpisodeRatingStatusResponse.of(episodeId, false, null));
    }
}
