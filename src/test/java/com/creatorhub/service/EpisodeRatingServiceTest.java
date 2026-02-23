package com.creatorhub.service;

import com.creatorhub.dto.episode.rating.EpisodeRatingStatusResponse;
import com.creatorhub.entity.Creation;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeRating;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.episode.EpisodeNotFoundException;
import com.creatorhub.exception.episode.rating.AlreadyEpisodeRatingException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.EpisodeRatingRepository;
import com.creatorhub.repository.EpisodeRepository;
import com.creatorhub.repository.MemberRepository;
import com.creatorhub.repository.projection.EpisodeMetaProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodeRatingServiceTest {

    @Mock EpisodeRatingRepository episodeRatingRepository;
    @Mock EpisodeRepository episodeRepository;
    @Mock MemberRepository memberRepository;

    @InjectMocks EpisodeRatingService episodeRatingService;

    // ----------------------------------------------------------------------
    // rate
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("rate 성공: member/episode 조회 -> EpisodeRating 저장 -> 집계 갱신 -> 갱신된 평균/개수 반환")
    void rate_success() {
        long memberId = 1L;
        long episodeId = 10L;
        long creationId = 100L;
        int score = 4;

        Episode episode = mock(Episode.class);
        Creation creation = mock(com.creatorhub.entity.Creation.class);
        given(creation.getId()).willReturn(creationId);
        given(episode.getCreation()).willReturn(creation);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(episodeRepository.findById(episodeId)).willReturn(Optional.of(episode));
        given(episodeRatingRepository.saveAndFlush(any(EpisodeRating.class))).willReturn(mock(EpisodeRating.class));

        EpisodeMetaProjection meta = mock(EpisodeMetaProjection.class);
        given(meta.getRatingAverage()).willReturn(new BigDecimal("4.000"));
        given(meta.getRatingCount()).willReturn(1);
        given(episodeRepository.findEpisodeMeta(creationId, episodeId)).willReturn(Optional.of(meta));

        EpisodeRatingStatusResponse res = episodeRatingService.rate(memberId, episodeId, score);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.rated()).isTrue();
        assertThat(res.score()).isEqualTo(score);
        assertThat(res.ratingAverage()).isEqualTo(new BigDecimal("4.000"));
        assertThat(res.ratingCount()).isEqualTo(1);

        then(episodeRatingRepository).should(times(1)).saveAndFlush(any(EpisodeRating.class));
        then(episodeRepository).should(times(1)).addRating(episodeId, score);
        then(episodeRepository).should(times(1)).findEpisodeMeta(creationId, episodeId);
    }

    @Test
    @DisplayName("rate 실패: 이미 별점 등록한 경우(유니크 위반) AlreadyEpisodeRatingException")
    void rate_alreadyRated() {
        long memberId = 1L;
        long episodeId = 10L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(episodeRepository.findById(episodeId)).willReturn(Optional.of(mock(Episode.class)));
        given(episodeRatingRepository.saveAndFlush(any(EpisodeRating.class)))
                .willThrow(new DataIntegrityViolationException("uk violation"));

        assertThatThrownBy(() -> episodeRatingService.rate(memberId, episodeId, 3))
                .isInstanceOf(AlreadyEpisodeRatingException.class);

        then(episodeRepository).should(never()).addRating(anyLong(), anyInt());
        then(episodeRepository).should(never()).findEpisodeMeta(anyLong(), anyLong());
    }

    @Test
    @DisplayName("rate 실패: member 없으면 MemberNotFoundException")
    void rate_memberNotFound() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> episodeRatingService.rate(99L, 10L, 5))
                .isInstanceOf(MemberNotFoundException.class);

        then(episodeRatingRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).addRating(anyLong(), anyInt());
    }

    @Test
    @DisplayName("rate 실패: episode 없으면 EpisodeNotFoundException")
    void rate_episodeNotFound() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(episodeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> episodeRatingService.rate(1L, 99L, 5))
                .isInstanceOf(EpisodeNotFoundException.class);

        then(episodeRatingRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).addRating(anyLong(), anyInt());
    }

    // ----------------------------------------------------------------------
    // getRatingStatus
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getRatingStatus: 별점 등록한 경우 rated=true, score 반환")
    void getRatingStatus_rated() {
        long memberId = 1L;
        long episodeId = 10L;

        EpisodeRating rating = mock(EpisodeRating.class);
        given(rating.getScore()).willReturn(5);
        given(episodeRatingRepository.findByMemberIdAndEpisodeId(memberId, episodeId))
                .willReturn(Optional.of(rating));

        EpisodeRatingStatusResponse res = episodeRatingService.getRatingStatus(memberId, episodeId);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.rated()).isTrue();
        assertThat(res.score()).isEqualTo(5);
    }

    @Test
    @DisplayName("getRatingStatus: 별점 미등록이면 rated=false, score=null 반환")
    void getRatingStatus_notRated() {
        long memberId = 1L;
        long episodeId = 10L;

        given(episodeRatingRepository.findByMemberIdAndEpisodeId(memberId, episodeId))
                .willReturn(Optional.empty());

        EpisodeRatingStatusResponse res = episodeRatingService.getRatingStatus(memberId, episodeId);

        assertThat(res.rated()).isFalse();
        assertThat(res.score()).isNull();
    }
}
