package com.creatorhub.service;

import com.creatorhub.dto.episode.like.EpisodeLikeResponse;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeLike;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.episode.EpisodeNotFoundException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.EpisodeLikeRepository;
import com.creatorhub.repository.EpisodeRepository;
import com.creatorhub.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodeLikeServiceTest {

    @Mock EpisodeLikeRepository episodeLikeRepository;
    @Mock EpisodeRepository episodeRepository;
    @Mock MemberRepository memberRepository;

    @InjectMocks EpisodeLikeService episodeLikeService;

    // ----------------------------------------------------------------------
    // like
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("like 성공: member/episode 조회 -> EpisodeLike 저장 -> likeCount +1 -> liked=true 반환")
    void like_success() {
        long memberId = 1L;
        long episodeId = 10L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(episodeRepository.findById(episodeId)).willReturn(Optional.of(mock(Episode.class)));
        given(episodeLikeRepository.saveAndFlush(any(EpisodeLike.class))).willReturn(mock(EpisodeLike.class));

        EpisodeLikeResponse res = episodeLikeService.like(memberId, episodeId);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.liked()).isTrue();

        then(episodeLikeRepository).should(times(1)).saveAndFlush(any(EpisodeLike.class));
        then(episodeRepository).should(times(1)).updateLikeCount(episodeId, 1);
    }

    @Test
    @DisplayName("like 멱등: 이미 좋아요 상태(유니크 위반)면 likeCount 변경 없이 liked=true 반환")
    void like_alreadyLiked_idempotent() {
        long memberId = 1L;
        long episodeId = 10L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(episodeRepository.findById(episodeId)).willReturn(Optional.of(mock(Episode.class)));
        given(episodeLikeRepository.saveAndFlush(any(EpisodeLike.class)))
                .willThrow(new DataIntegrityViolationException("uk violation"));

        EpisodeLikeResponse res = episodeLikeService.like(memberId, episodeId);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.liked()).isTrue();

        then(episodeRepository).should(never()).updateLikeCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("like 실패: member 없으면 MemberNotFoundException")
    void like_memberNotFound() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> episodeLikeService.like(99L, 10L))
                .isInstanceOf(MemberNotFoundException.class);

        then(episodeLikeRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).updateLikeCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("like 실패: episode 없으면 EpisodeNotFoundException")
    void like_episodeNotFound() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(episodeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> episodeLikeService.like(1L, 99L))
                .isInstanceOf(EpisodeNotFoundException.class);

        then(episodeLikeRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).updateLikeCount(anyLong(), anyInt());
    }

    // ----------------------------------------------------------------------
    // unlike
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("unlike 성공: 좋아요 row 삭제 -> likeCount -1 -> liked=false 반환")
    void unlike_success() {
        long memberId = 1L;
        long episodeId = 10L;

        given(episodeLikeRepository.deleteByMemberIdAndEpisodeId(memberId, episodeId)).willReturn(1);

        EpisodeLikeResponse res = episodeLikeService.unlike(memberId, episodeId);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.liked()).isFalse();

        then(episodeRepository).should(times(1)).updateLikeCount(episodeId, -1);
    }

    @Test
    @DisplayName("unlike 멱등: 좋아요 row 없으면(삭제 0건) likeCount 변경 없이 liked=false 반환")
    void unlike_notLiked_idempotent() {
        long memberId = 1L;
        long episodeId = 10L;

        given(episodeLikeRepository.deleteByMemberIdAndEpisodeId(memberId, episodeId)).willReturn(0);

        EpisodeLikeResponse res = episodeLikeService.unlike(memberId, episodeId);

        assertThat(res.liked()).isFalse();

        then(episodeRepository).should(never()).updateLikeCount(anyLong(), anyInt());
    }

    // ----------------------------------------------------------------------
    // getLikeStatus
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getLikeStatus: 좋아요 상태이면 liked=true 반환")
    void getLikeStatus_liked() {
        long memberId = 1L;
        long episodeId = 10L;

        given(episodeLikeRepository.existsByMemberIdAndEpisodeId(memberId, episodeId)).willReturn(true);

        EpisodeLikeResponse res = episodeLikeService.getLikeStatus(memberId, episodeId);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.liked()).isTrue();
    }

    @Test
    @DisplayName("getLikeStatus: 좋아요 안 한 상태이면 liked=false 반환")
    void getLikeStatus_notLiked() {
        long memberId = 1L;
        long episodeId = 10L;

        given(episodeLikeRepository.existsByMemberIdAndEpisodeId(memberId, episodeId)).willReturn(false);

        EpisodeLikeResponse res = episodeLikeService.getLikeStatus(memberId, episodeId);

        assertThat(res.liked()).isFalse();
    }
}
