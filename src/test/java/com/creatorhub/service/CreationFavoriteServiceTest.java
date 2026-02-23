package com.creatorhub.service;

import com.creatorhub.dto.creation.CreationFavoriteResponse;
import com.creatorhub.dto.creation.favorite.FavoriteCreationItem;
import com.creatorhub.entity.Creation;
import com.creatorhub.entity.CreationFavorite;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.creation.CreationNotFoundException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.CreationFavoriteRepository;
import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CreationFavoriteServiceTest {

    @Mock CreationFavoriteRepository creationFavoriteRepository;
    @Mock CreationRepository creationRepository;
    @Mock MemberRepository memberRepository;

    @InjectMocks CreationFavoriteService creationFavoriteService;

    // ----------------------------------------------------------------------
    // favorite
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("favorite 성공: member/creation 조회 -> CreationFavorite 저장 -> favoriteCount +1 -> favorited=true 반환")
    void favorite_success() {
        long memberId = 1L;
        long creationId = 100L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(creationRepository.findById(creationId)).willReturn(Optional.of(mock(Creation.class)));
        given(creationFavoriteRepository.saveAndFlush(any(CreationFavorite.class))).willReturn(mock(CreationFavorite.class));

        CreationFavoriteResponse res = creationFavoriteService.favorite(memberId, creationId);

        assertThat(res.creationId()).isEqualTo(creationId);
        assertThat(res.favorited()).isTrue();

        then(creationFavoriteRepository).should(times(1)).saveAndFlush(any(CreationFavorite.class));
        then(creationRepository).should(times(1)).updateFavoriteCount(creationId, 1);
    }

    @Test
    @DisplayName("favorite 멱등: 이미 관심 등록 상태(유니크 위반)면 favoriteCount 변경 없이 favorited=true 반환")
    void favorite_alreadyFavorited_idempotent() {
        long memberId = 1L;
        long creationId = 100L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(creationRepository.findById(creationId)).willReturn(Optional.of(mock(Creation.class)));
        given(creationFavoriteRepository.saveAndFlush(any(CreationFavorite.class)))
                .willThrow(new DataIntegrityViolationException("uk violation"));

        CreationFavoriteResponse res = creationFavoriteService.favorite(memberId, creationId);

        assertThat(res.favorited()).isTrue();

        then(creationRepository).should(never()).updateFavoriteCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("favorite 실패: member 없으면 MemberNotFoundException")
    void favorite_memberNotFound() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> creationFavoriteService.favorite(99L, 100L))
                .isInstanceOf(MemberNotFoundException.class);

        then(creationFavoriteRepository).shouldHaveNoInteractions();
        then(creationRepository).should(never()).updateFavoriteCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("favorite 실패: creation 없으면 CreationNotFoundException")
    void favorite_creationNotFound() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(creationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> creationFavoriteService.favorite(1L, 999L))
                .isInstanceOf(CreationNotFoundException.class);

        then(creationFavoriteRepository).shouldHaveNoInteractions();
        then(creationRepository).should(never()).updateFavoriteCount(anyLong(), anyInt());
    }

    // ----------------------------------------------------------------------
    // unfavorite
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("unfavorite 성공: 관심 row 삭제 -> favoriteCount -1 -> favorited=false 반환")
    void unfavorite_success() {
        long memberId = 1L;
        long creationId = 100L;

        given(creationFavoriteRepository.deleteByMemberIdAndCreationId(memberId, creationId)).willReturn(1);

        CreationFavoriteResponse res = creationFavoriteService.unfavorite(memberId, creationId);

        assertThat(res.creationId()).isEqualTo(creationId);
        assertThat(res.favorited()).isFalse();

        then(creationRepository).should(times(1)).updateFavoriteCount(creationId, -1);
    }

    @Test
    @DisplayName("unfavorite 멱등: 관심 row 없으면(삭제 0건) favoriteCount 변경 없이 favorited=false 반환")
    void unfavorite_notFavorited_idempotent() {
        long memberId = 1L;
        long creationId = 100L;

        given(creationFavoriteRepository.deleteByMemberIdAndCreationId(memberId, creationId)).willReturn(0);

        CreationFavoriteResponse res = creationFavoriteService.unfavorite(memberId, creationId);

        assertThat(res.favorited()).isFalse();

        then(creationRepository).should(never()).updateFavoriteCount(anyLong(), anyInt());
    }

    // ----------------------------------------------------------------------
    // getFavoriteStatus
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getFavoriteStatus: 관심 등록 상태이면 favorited=true 반환")
    void getFavoriteStatus_favorited() {
        long memberId = 1L;
        long creationId = 100L;

        given(creationFavoriteRepository.existsByMemberIdAndCreationId(memberId, creationId)).willReturn(true);

        CreationFavoriteResponse res = creationFavoriteService.getFavoriteStatus(memberId, creationId);

        assertThat(res.creationId()).isEqualTo(creationId);
        assertThat(res.favorited()).isTrue();
    }

    @Test
    @DisplayName("getFavoriteStatus: 관심 미등록이면 favorited=false 반환")
    void getFavoriteStatus_notFavorited() {
        long memberId = 1L;
        long creationId = 100L;

        given(creationFavoriteRepository.existsByMemberIdAndCreationId(memberId, creationId)).willReturn(false);

        CreationFavoriteResponse res = creationFavoriteService.getFavoriteStatus(memberId, creationId);

        assertThat(res.favorited()).isFalse();
    }

    // ----------------------------------------------------------------------
    // getMyFavorites
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getMyFavorites 성공: CreationFavorite 페이지를 FavoriteCreationItem으로 변환하여 반환")
    void getMyFavorites_success() {
        long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Creation creation1 = mock(Creation.class);
        given(creation1.getId()).willReturn(100L);
        given(creation1.getTitle()).willReturn("작품A");
        given(creation1.getPlot()).willReturn("줄거리A");
        given(creation1.isPublic()).willReturn(true);
        given(creation1.getFavoriteCount()).willReturn(5);

        CreationFavorite cf1 = mock(CreationFavorite.class);
        given(cf1.getCreation()).willReturn(creation1);
        given(cf1.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 10, 0, 0));

        Creation creation2 = mock(Creation.class);
        given(creation2.getId()).willReturn(200L);
        given(creation2.getTitle()).willReturn("작품B");
        given(creation2.getPlot()).willReturn("줄거리B");
        given(creation2.isPublic()).willReturn(false);
        given(creation2.getFavoriteCount()).willReturn(0);

        CreationFavorite cf2 = mock(CreationFavorite.class);
        given(cf2.getCreation()).willReturn(creation2);
        given(cf2.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 5, 0, 0));

        Page<CreationFavorite> page = new PageImpl<>(List.of(cf1, cf2), pageable, 2);
        given(creationFavoriteRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable))
                .willReturn(page);

        Page<FavoriteCreationItem> result = creationFavoriteService.getMyFavorites(memberId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);

        FavoriteCreationItem item1 = result.getContent().get(0);
        assertThat(item1.creationId()).isEqualTo(100L);
        assertThat(item1.title()).isEqualTo("작품A");
        assertThat(item1.plot()).isEqualTo("줄거리A");
        assertThat(item1.isPublic()).isTrue();
        assertThat(item1.favoriteCount()).isEqualTo(5);
        assertThat(item1.favoritedAt()).isEqualTo(LocalDateTime.of(2026, 1, 10, 0, 0));

        FavoriteCreationItem item2 = result.getContent().get(1);
        assertThat(item2.creationId()).isEqualTo(200L);
        assertThat(item2.isPublic()).isFalse();
        assertThat(item2.favoriteCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getMyFavorites 성공: 관심 작품 없으면 빈 페이지 반환")
    void getMyFavorites_empty() {
        long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(creationFavoriteRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable))
                .willReturn(Page.empty(pageable));

        Page<FavoriteCreationItem> result = creationFavoriteService.getMyFavorites(memberId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}
