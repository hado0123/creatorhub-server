package com.creatorhub.service;

import com.creatorhub.constant.EpisodeThumbnailType;
import com.creatorhub.dto.episode.*;
import com.creatorhub.entity.*;
import com.creatorhub.exception.creation.CreationNotFoundException;
import com.creatorhub.exception.episode.AlreadyEpisodeException;
import com.creatorhub.exception.episode.EpisodeNotFoundException;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.repository.*;
import com.creatorhub.repository.projection.EpisodeListProjection;
import com.creatorhub.repository.projection.EpisodeMetaProjection;
import com.creatorhub.repository.projection.ManuscriptRowProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodeServiceTest {

    @Mock CreationRepository creationRepository;
    @Mock EpisodeRepository episodeRepository;
    @Mock FileObjectRepository fileObjectRepository;
    @Mock ManuscriptImageRepository manuscriptImageRepository;
    @Mock ManuscriptImageService manuscriptImageService;
    @Mock EpisodeThumbnailService episodeThumbnailService;

    @InjectMocks EpisodeService episodeService;

    private static final String CDN_BASE = "https://cdn.example.com";

    @BeforeEach
    void injectCloudfrontBase() {
        ReflectionTestUtils.setField(episodeService, "cloudfrontBase", CDN_BASE);
    }

    private static EpisodeRequest validReq(Long creationId) {
        return new EpisodeRequest(
                creationId,
                1,
                "1화 제목",
                "작가의 말",
                true,
                true,
                10L,
                11L,
                List.of(new ManuscriptRegisterItem(1L, 1), new ManuscriptRegisterItem(2L, 2))
        );
    }

    // ----------------------------------------------------------------------
    // publishEpisode
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("publishEpisode 성공: creation 조회 -> 인가 확인 -> 회차 중복 체크 -> 원고 순서 중복 체크 -> FileObject 조회 -> episode/원고/썸네일 저장")
    void publishEpisode_success() {
        long memberId = 1L;
        long creationId = 100L;
        EpisodeRequest req = validReq(creationId);

        Creation creation = mock(Creation.class);
        given(creationRepository.findById(creationId)).willReturn(Optional.of(creation));
        given(creationRepository.findOwnerMemberIdByCreationId(creationId)).willReturn(Optional.of(memberId));
        given(episodeRepository.existsByCreationIdAndEpisodeNum(creationId, 1)).willReturn(false);
        willDoNothing().given(manuscriptImageService).validateDisplayOrders(req.manuscripts());

        FileObject fo1 = mock(FileObject.class);
        FileObject fo2 = mock(FileObject.class);
        FileObject episodeThumb = mock(FileObject.class);
        FileObject snsThumb = mock(FileObject.class);
        given(fo1.getId()).willReturn(1L);
        given(fo2.getId()).willReturn(2L);
        given(episodeThumb.getId()).willReturn(10L);
        given(snsThumb.getId()).willReturn(11L);
        given(fileObjectRepository.findAllById(anySet())).willReturn(List.of(fo1, fo2, episodeThumb, snsThumb));

        Episode savedEpisode = mock(Episode.class);
        given(savedEpisode.getId()).willReturn(200L);
        given(savedEpisode.getEpisodeNum()).willReturn(1);
        given(episodeRepository.save(any(Episode.class))).willReturn(savedEpisode);

        ManuscriptImage m1 = mock(ManuscriptImage.class);
        ManuscriptImage m2 = mock(ManuscriptImage.class);
        given(m1.getId()).willReturn(1L);
        given(m2.getId()).willReturn(2L);
        given(manuscriptImageService.createAndSave(any(), any(), any())).willReturn(List.of(m1, m2));

        EpisodeThumbnail t1 = mock(EpisodeThumbnail.class);
        EpisodeThumbnail t2 = mock(EpisodeThumbnail.class);
        given(t1.getId()).willReturn(10L);
        given(t2.getId()).willReturn(11L);
        given(episodeThumbnailService.createAndSave(any(), any(), any())).willReturn(List.of(t1, t2));

        EpisodeResponse res = episodeService.publishEpisode(req, memberId);

        assertThat(res.episodeId()).isEqualTo(200L);
        assertThat(res.episodeNum()).isEqualTo(1);
        assertThat(res.manuscriptImageIds()).containsExactly(1L, 2L);
        assertThat(res.episodeThumbnailIds()).containsExactly(10L, 11L);

        then(creationRepository).should(times(1)).findById(creationId);
        then(episodeRepository).should(times(1)).existsByCreationIdAndEpisodeNum(creationId, 1);
        then(episodeRepository).should(times(1)).save(any(Episode.class));
        then(manuscriptImageService).should(times(1)).createAndSave(any(), any(), any());
        then(episodeThumbnailService).should(times(1)).createAndSave(any(), any(), any());
    }

    @Test
    @DisplayName("publishEpisode 실패: creation 없으면 CreationNotFoundException")
    void publishEpisode_creationNotFound() {
        long memberId = 1L;
        long creationId = 999L;
        EpisodeRequest req = validReq(creationId);

        given(creationRepository.findById(creationId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> episodeService.publishEpisode(req, memberId))
                .isInstanceOf(CreationNotFoundException.class);

        then(episodeRepository).should(never()).save(any());
        then(fileObjectRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("publishEpisode 실패: 작품 소유자가 아니면 AccessDeniedException")
    void publishEpisode_notOwner() {
        long memberId = 1L;
        long ownerId = 99L;
        long creationId = 100L;
        EpisodeRequest req = validReq(creationId);

        given(creationRepository.findById(creationId)).willReturn(Optional.of(mock(Creation.class)));
        given(creationRepository.findOwnerMemberIdByCreationId(creationId)).willReturn(Optional.of(ownerId));

        assertThatThrownBy(() -> episodeService.publishEpisode(req, memberId))
                .isInstanceOf(AccessDeniedException.class);

        then(episodeRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("publishEpisode 실패: 이미 존재하는 회차이면 AlreadyEpisodeException")
    void publishEpisode_duplicateEpisodeNum() {
        long memberId = 1L;
        long creationId = 100L;
        EpisodeRequest req = validReq(creationId);

        given(creationRepository.findById(creationId)).willReturn(Optional.of(mock(Creation.class)));
        given(creationRepository.findOwnerMemberIdByCreationId(creationId)).willReturn(Optional.of(memberId));
        given(episodeRepository.existsByCreationIdAndEpisodeNum(creationId, 1)).willReturn(true);

        assertThatThrownBy(() -> episodeService.publishEpisode(req, memberId))
                .isInstanceOf(AlreadyEpisodeException.class);

        then(fileObjectRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("publishEpisode 실패: 원고 displayOrder 중복이면 IllegalArgumentException")
    void publishEpisode_duplicateDisplayOrder() {
        long memberId = 1L;
        long creationId = 100L;
        EpisodeRequest req = validReq(creationId);

        given(creationRepository.findById(creationId)).willReturn(Optional.of(mock(Creation.class)));
        given(creationRepository.findOwnerMemberIdByCreationId(creationId)).willReturn(Optional.of(memberId));
        given(episodeRepository.existsByCreationIdAndEpisodeNum(creationId, 1)).willReturn(false);
        willThrow(new IllegalArgumentException("중복된 displayOrder가 있습니다: 1"))
                .given(manuscriptImageService).validateDisplayOrders(req.manuscripts());

        assertThatThrownBy(() -> episodeService.publishEpisode(req, memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복된 displayOrder");

        then(fileObjectRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("publishEpisode 실패: FileObject 일부 누락이면 FileObjectNotFoundException")
    void publishEpisode_fileObjectMissing() {
        long memberId = 1L;
        long creationId = 100L;
        EpisodeRequest req = validReq(creationId);

        given(creationRepository.findById(creationId)).willReturn(Optional.of(mock(Creation.class)));
        given(creationRepository.findOwnerMemberIdByCreationId(creationId)).willReturn(Optional.of(memberId));
        given(episodeRepository.existsByCreationIdAndEpisodeNum(creationId, 1)).willReturn(false);
        willDoNothing().given(manuscriptImageService).validateDisplayOrders(req.manuscripts());

        // 4개 중 3개만 반환 -> 누락 유도
        given(fileObjectRepository.findAllById(anySet())).willReturn(List.of(
                mock(FileObject.class), mock(FileObject.class), mock(FileObject.class)
        ));

        assertThatThrownBy(() -> episodeService.publishEpisode(req, memberId))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(episodeRepository).should(never()).save(any());
    }

    // ----------------------------------------------------------------------
    // getEpisodesByCreation
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getEpisodesByCreation 성공: projection 목록을 EpisodeListResponse로 변환하여 반환")
    void getEpisodesByCreation_success() {
        long creationId = 100L;

        given(creationRepository.existsById(creationId)).willReturn(true);

        EpisodeListProjection p1 = mock(EpisodeListProjection.class);
        given(p1.getId()).willReturn(1L);
        given(p1.getEpisodeNum()).willReturn(1);
        given(p1.getTitle()).willReturn("1화");
        given(p1.getIsPublic()).willReturn(true);
        given(p1.getRatingAverage()).willReturn(new BigDecimal("4.500"));
        given(p1.getStorageKey()).willReturn("upload/thumb1.jpg");
        given(p1.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0));

        EpisodeListProjection p2 = mock(EpisodeListProjection.class);
        given(p2.getId()).willReturn(2L);
        given(p2.getEpisodeNum()).willReturn(2);
        given(p2.getTitle()).willReturn("2화");
        given(p2.getIsPublic()).willReturn(false);
        given(p2.getRatingAverage()).willReturn(null);
        given(p2.getStorageKey()).willReturn(null);
        given(p2.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 8, 0, 0));

        given(episodeRepository.findEpisodeListProjection(creationId, EpisodeThumbnailType.EPISODE))
                .willReturn(List.of(p1, p2));

        List<EpisodeListResponse> result = episodeService.getEpisodesByCreation(creationId);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).episodeId()).isEqualTo(1L);
        assertThat(result.get(0).episodeNum()).isEqualTo(1);
        assertThat(result.get(0).title()).isEqualTo("1화");
        assertThat(result.get(0).isPublic()).isTrue();
        assertThat(result.get(0).ratingAverage()).isEqualTo(new BigDecimal("4.500"));
        assertThat(result.get(0).episodeThumbnailUrl()).isEqualTo(CDN_BASE + "/upload/thumb1.jpg");

        assertThat(result.get(1).episodeId()).isEqualTo(2L);
        assertThat(result.get(1).episodeThumbnailUrl()).isNull();
        assertThat(result.get(1).ratingAverage()).isNull();

        then(episodeRepository).should(times(1))
                .findEpisodeListProjection(creationId, EpisodeThumbnailType.EPISODE);
    }

    @Test
    @DisplayName("getEpisodesByCreation 성공: 등록된 회차가 없으면 빈 리스트 반환")
    void getEpisodesByCreation_empty() {
        long creationId = 100L;

        given(creationRepository.existsById(creationId)).willReturn(true);
        given(episodeRepository.findEpisodeListProjection(creationId, EpisodeThumbnailType.EPISODE))
                .willReturn(List.of());

        List<EpisodeListResponse> result = episodeService.getEpisodesByCreation(creationId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getEpisodesByCreation 실패: creation 없으면 CreationNotFoundException")
    void getEpisodesByCreation_creationNotFound() {
        long creationId = 999L;

        given(creationRepository.existsById(creationId)).willReturn(false);

        assertThatThrownBy(() -> episodeService.getEpisodesByCreation(creationId))
                .isInstanceOf(CreationNotFoundException.class);

        then(episodeRepository).shouldHaveNoInteractions();
    }

    // ----------------------------------------------------------------------
    // getEpisodeDetail
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getEpisodeDetail 성공: meta + 원고 storageKey 조회 후 CDN URL로 변환, viewCount 증가")
    void getEpisodeDetail_success() {
        long creationId = 100L;
        long episodeId = 1L;

        EpisodeMetaProjection meta = mock(EpisodeMetaProjection.class);
        given(meta.getEpisodeId()).willReturn(episodeId);
        given(meta.getEpisodeNum()).willReturn(1);
        given(meta.getTitle()).willReturn("1화 제목");
        given(meta.getLikeCount()).willReturn(42);
        given(meta.getRatingAverage()).willReturn(new BigDecimal("4.500"));
        given(meta.getRatingCount()).willReturn(10);

        given(episodeRepository.findEpisodeMeta(creationId, episodeId))
                .willReturn(Optional.of(meta));

        ManuscriptRowProjection row1 = mock(ManuscriptRowProjection.class);
        ManuscriptRowProjection row2 = mock(ManuscriptRowProjection.class);
        given(row1.getStorageKey()).willReturn("upload/manuscript/1.jpg");
        given(row2.getStorageKey()).willReturn("upload/manuscript/2.jpg");

        given(manuscriptImageRepository.findManuscripts(creationId, episodeId))
                .willReturn(List.of(row1, row2));

        EpisodeDetailResponse res = episodeService.getEpisodeDetail(creationId, episodeId);

        assertThat(res.episodeId()).isEqualTo(episodeId);
        assertThat(res.episodeNum()).isEqualTo(1);
        assertThat(res.title()).isEqualTo("1화 제목");
        assertThat(res.likeCount()).isEqualTo(42);
        assertThat(res.ratingAverage()).isEqualTo(new BigDecimal("4.500"));
        assertThat(res.ratingCount()).isEqualTo(10);
        assertThat(res.manuscriptImageUrls()).containsExactly(
                CDN_BASE + "/upload/manuscript/1.jpg",
                CDN_BASE + "/upload/manuscript/2.jpg"
        );

        // 조회수 증가 호출 확인
        then(episodeRepository).should(times(1)).incrementViewCount(episodeId);
    }

    @Test
    @DisplayName("getEpisodeDetail 성공: 원고가 없으면 manuscriptImageUrls가 빈 리스트 반환")
    void getEpisodeDetail_noManuscripts() {
        long creationId = 100L;
        long episodeId = 1L;

        EpisodeMetaProjection meta = mock(EpisodeMetaProjection.class);
        given(meta.getEpisodeId()).willReturn(episodeId);
        given(meta.getEpisodeNum()).willReturn(1);
        given(meta.getTitle()).willReturn("1화 제목");
        given(meta.getLikeCount()).willReturn(0);
        given(meta.getRatingAverage()).willReturn(null);
        given(meta.getRatingCount()).willReturn(0);

        given(episodeRepository.findEpisodeMeta(creationId, episodeId))
                .willReturn(Optional.of(meta));
        given(manuscriptImageRepository.findManuscripts(creationId, episodeId))
                .willReturn(List.of());

        EpisodeDetailResponse res = episodeService.getEpisodeDetail(creationId, episodeId);

        assertThat(res.manuscriptImageUrls()).isEmpty();
        assertThat(res.ratingAverage()).isNull();
        then(episodeRepository).should(times(1)).incrementViewCount(episodeId);
    }

    @Test
    @DisplayName("getEpisodeDetail 실패: 해당 회차 없으면 EpisodeNotFoundException, viewCount 증가 미호출")
    void getEpisodeDetail_episodeNotFound() {
        long creationId = 100L;
        long episodeId = 999L;

        given(episodeRepository.findEpisodeMeta(creationId, episodeId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> episodeService.getEpisodeDetail(creationId, episodeId))
                .isInstanceOf(EpisodeNotFoundException.class);

        then(manuscriptImageRepository).shouldHaveNoInteractions();
        then(episodeRepository).should(never()).incrementViewCount(anyLong());
    }
}
