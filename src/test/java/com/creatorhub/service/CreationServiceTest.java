package com.creatorhub.service;

import com.creatorhub.constant.*;
import com.creatorhub.dto.creation.*;
import com.creatorhub.entity.*;
import com.creatorhub.exception.creation.CreationNotFoundException;
import com.creatorhub.exception.creator.CreatorNotFoundException;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.exception.hashtag.HashtagNotFoundException;
import com.creatorhub.repository.*;
import com.creatorhub.repository.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CreationServiceTest {

    @Mock CreationRepository creationRepository;
    @Mock FileObjectRepository fileObjectRepository;
    @Mock CreatorRepository creatorRepository;
    @Mock HashtagRepository hashtagRepository;
    @Mock CreationHashtagRepository creationHashtagRepository;

    @InjectMocks CreationService creationService;

    private static final String CDN_BASE = "https://cdn.example.com";

    @BeforeEach
    void injectCloudfrontBase() {
        ReflectionTestUtils.setField(creationService, "cloudfrontBase", CDN_BASE);
    }

    private static FileObject foWithStorageKey(String key) {
        return FileObject.create(
                key,
                "thumb.jpg",
                FileObjectStatus.READY,
                "image/jpeg",
                123L
        );
    }

    private static Hashtag hashtagEntityWithId(long id) {
        Hashtag h = Hashtag.create("tag-" + id);
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }

    private static CreationRequest validReq(Set<Long> hashtagIds,
                                            Long horizontalId,
                                            Long posterId) {
        return new CreationRequest(
                CreationFormat.EPISODE,
                CreationGenre.DAILY_LIFE,
                "me의 일상",
                "느긋한듯 바쁘게 사는 me의 현대 일상물.",
                true,
                Set.of(PublishDay.MON, PublishDay.FRI),
                hashtagIds,
                horizontalId,
                posterId
        );
    }

    // ----------------------------------------------------------------------
    // createCreation
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("createCreation 성공: creator 조회 -> 썸네일 7개 존재 검증 -> poster 조회 -> hashtag 연결 -> creation 저장")
    void createCreation_success() {
        long memberId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;
        Set<Long> hashtagIds = Set.of(100L, 101L);

        CreationRequest req = validReq(hashtagIds, horizontalId, posterId);

        Creator creator = mock(Creator.class);
        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.of(creator));

        // horizontal original + baseKey
        String baseKey = "upload/2026/01/27/creation-abc";
        FileObject horizontalOriginal = mock(FileObject.class);
        given(horizontalOriginal.extractBaseKey()).willReturn(baseKey);
        given(fileObjectRepository.findById(horizontalId)).willReturn(Optional.of(horizontalOriginal));

        // 썸네일 7개 키 생성(가로 원본 + 파생6)
        List<String> thumbKeys = ThumbnailKeys.allSuffixes().stream()
                .map(suffix -> baseKey + suffix)
                .toList();

        // 7개 모두 존재하도록 반환
        given(fileObjectRepository.findByStorageKeyIn(thumbKeys))
                .willReturn(thumbKeys.stream().map(CreationServiceTest::foWithStorageKey).toList());

        // poster original
        FileObject posterOriginal = mock(FileObject.class);
        given(fileObjectRepository.findById(posterId)).willReturn(Optional.of(posterOriginal));

        // hashtags 전부 존재
        given(hashtagRepository.findByIdIn(hashtagIds))
                .willReturn(List.of(
                        hashtagEntityWithId(100L),
                        hashtagEntityWithId(101L)
                ));

        // save 결과(id 필요)
        Creation saved = mock(Creation.class);
        given(saved.getId()).willReturn(999L);
        given(creationRepository.save(any(Creation.class))).willReturn(saved);

        Long creationId = creationService.createCreation(req, memberId);

        assertThat(creationId).isEqualTo(999L);

        then(creatorRepository).should(times(1)).findByMemberId(memberId);
        then(fileObjectRepository).should(times(1)).findById(horizontalId);
        then(fileObjectRepository).should(times(1)).findByStorageKeyIn(thumbKeys);
        then(fileObjectRepository).should(times(1)).findById(posterId);
        then(hashtagRepository).should(times(1)).findByIdIn(hashtagIds);
        then(creationRepository).should(times(1)).save(any(Creation.class));
    }

    @Test
    @DisplayName("createCreation 실패: creator 없으면 CreatorNotFoundException")
    void createCreation_creatorNotFound() {
        long memberId = 1L;

        CreationRequest req = validReq(Set.of(100L), 10L, 20L);

        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.createCreation(req, memberId))
                .isInstanceOf(CreatorNotFoundException.class);

        then(creationRepository).should(never()).save(any());
        then(fileObjectRepository).shouldHaveNoInteractions();
        then(hashtagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createCreation 실패: 가로형 원본 FileObject 없으면 FileObjectNotFoundException")
    void createCreation_horizontalOriginalNotFound() {
        long memberId = 1L;
        CreationRequest req = validReq(Set.of(100L), 10L, 20L);

        given(creatorRepository.findByMemberId(memberId))
                .willReturn(Optional.of(mock(Creator.class)));

        given(fileObjectRepository.findById(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.createCreation(req, memberId))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(fileObjectRepository).should(never()).findByStorageKeyIn(anyList());
        then(fileObjectRepository).should(never()).findById(20L);
        then(hashtagRepository).shouldHaveNoInteractions();
        then(creationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCreation 실패: 썸네일 7개 중 누락 있으면 FileObjectNotFoundException")
    void createCreation_thumbnailMissing() {
        long memberId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;

        CreationRequest req = validReq(Set.of(100L), horizontalId, posterId);

        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.of(mock(Creator.class)));

        String baseKey = "upload/2026/01/27/creation-missing";
        FileObject horizontalOriginal = mock(FileObject.class);
        given(horizontalOriginal.extractBaseKey()).willReturn(baseKey);
        given(fileObjectRepository.findById(horizontalId)).willReturn(Optional.of(horizontalOriginal));

        List<String> keys = ThumbnailKeys.allSuffixes().stream()
                .map(s -> baseKey + s)
                .toList();

        // 일부만 반환해서 누락 유도
        given(fileObjectRepository.findByStorageKeyIn(keys))
                .willReturn(keys.subList(0, keys.size() - 1).stream()
                        .map(CreationServiceTest::foWithStorageKey)
                        .toList());

        assertThatThrownBy(() -> creationService.createCreation(req, memberId))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(fileObjectRepository).should(never()).findById(posterId);
        then(creationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCreation 실패: 포스터 원본 FileObject 없으면 FileObjectNotFoundException")
    void createCreation_posterOriginalNotFound() {
        long memberId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;

        CreationRequest req = validReq(Set.of(100L), horizontalId, posterId);

        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.of(mock(Creator.class)));

        String baseKey = "upload/2026/01/27/creation-poster";
        FileObject horizontalOriginal = mock(FileObject.class);
        given(horizontalOriginal.extractBaseKey()).willReturn(baseKey);
        given(fileObjectRepository.findById(horizontalId)).willReturn(Optional.of(horizontalOriginal));

        List<String> keys = ThumbnailKeys.allSuffixes().stream()
                .map(s -> baseKey + s)
                .toList();

        // 썸네일 7개는 전부 존재
        given(fileObjectRepository.findByStorageKeyIn(keys))
                .willReturn(keys.stream().map(CreationServiceTest::foWithStorageKey).toList());

        // 포스터 원본 없음
        given(fileObjectRepository.findById(posterId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.createCreation(req, memberId))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(creationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCreation 실패: hashtagIds 중 누락이 있으면 IllegalArgumentException")
    void createCreation_hashtagMissing() {
        long memberId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;

        Set<Long> hashtagIds = Set.of(100L, 101L, 102L);
        CreationRequest req = validReq(hashtagIds, horizontalId, posterId);

        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.of(mock(Creator.class)));

        String baseKey = "upload/2026/01/27/creation-hash";
        FileObject horizontalOriginal = mock(FileObject.class);
        given(horizontalOriginal.extractBaseKey()).willReturn(baseKey);
        given(fileObjectRepository.findById(horizontalId)).willReturn(Optional.of(horizontalOriginal));

        List<String> keys = ThumbnailKeys.allSuffixes().stream()
                .map(s -> baseKey + s)
                .toList();

        given(fileObjectRepository.findByStorageKeyIn(keys))
                .willReturn(keys.stream().map(CreationServiceTest::foWithStorageKey).toList());

        given(fileObjectRepository.findById(posterId)).willReturn(Optional.of(mock(FileObject.class)));

        // 일부만 반환해서 누락 유도(102 누락)
        given(hashtagRepository.findByIdIn(hashtagIds))
                .willReturn(List.of(
                        hashtagEntityWithId(100L),
                        hashtagEntityWithId(101L)
                ));

        assertThatThrownBy(() -> creationService.createCreation(req, memberId))
                .isInstanceOf(HashtagNotFoundException.class);

        then(creationRepository).should(never()).save(any());
    }

    // ----------------------------------------------------------------------
    // getCreation
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getCreation 성공: projection + hashtag 조회 후 CreationResponse 반환")
    void getCreation_success() {
        long creationId = 1L;
        String storageKey = "upload/2026/01/27/poster.jpg";

        CreationBaseProjection projection = mock(CreationBaseProjection.class);
        given(projection.getCreationId()).willReturn(creationId);
        given(projection.getTitle()).willReturn("테스트 작품");
        given(projection.getPlot()).willReturn("줄거리");
        given(projection.getCreatorName()).willReturn("작가명");
        given(projection.getStorageKey()).willReturn(storageKey);

        HashtagTitleProjection tag1 = mock(HashtagTitleProjection.class);
        HashtagTitleProjection tag2 = mock(HashtagTitleProjection.class);
        given(tag1.getTitle()).willReturn("일상");
        given(tag2.getTitle()).willReturn("힐링");

        given(creationRepository.findCreationDetailBase(creationId, CreationThumbnailType.POSTER))
                .willReturn(Optional.of(projection));
        given(creationHashtagRepository.findHashtagTitlesByCreationId(creationId))
                .willReturn(List.of(tag1, tag2));

        CreationResponse resp = creationService.getCreation(creationId);

        assertThat(resp.creationId()).isEqualTo(creationId);
        assertThat(resp.title()).isEqualTo("테스트 작품");
        assertThat(resp.plot()).isEqualTo("줄거리");
        assertThat(resp.creatorName()).isEqualTo("작가명");
        assertThat(resp.posterThumbnailUrl()).isEqualTo(CDN_BASE + "/" + storageKey);
        assertThat(resp.hashtags()).containsExactlyInAnyOrder("일상", "힐링");

        then(creationRepository).should(times(1)).findCreationDetailBase(creationId, CreationThumbnailType.POSTER);
        then(creationHashtagRepository).should(times(1)).findHashtagTitlesByCreationId(creationId);
    }

    @Test
    @DisplayName("getCreation 성공: storageKey가 null이면 posterThumbnailUrl도 null 반환")
    void getCreation_nullStorageKey() {
        long creationId = 2L;

        CreationBaseProjection projection = mock(CreationBaseProjection.class);
        given(projection.getCreationId()).willReturn(creationId);
        given(projection.getTitle()).willReturn("썸네일 없는 작품");
        given(projection.getPlot()).willReturn("줄거리");
        given(projection.getCreatorName()).willReturn("작가명");
        given(projection.getStorageKey()).willReturn(null);

        given(creationRepository.findCreationDetailBase(creationId, CreationThumbnailType.POSTER))
                .willReturn(Optional.of(projection));
        given(creationHashtagRepository.findHashtagTitlesByCreationId(creationId))
                .willReturn(List.of());

        CreationResponse resp = creationService.getCreation(creationId);

        assertThat(resp.posterThumbnailUrl()).isNull();
        assertThat(resp.hashtags()).isEmpty();
    }

    @Test
    @DisplayName("getCreation 실패: creationId 없으면 CreationNotFoundException")
    void getCreation_notFound() {
        long creationId = 999L;

        given(creationRepository.findCreationDetailBase(creationId, CreationThumbnailType.POSTER))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.getCreation(creationId))
                .isInstanceOf(CreationNotFoundException.class);

        then(creationHashtagRepository).shouldHaveNoInteractions();
    }

    // ----------------------------------------------------------------------
    // getMyCreations
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("getMyCreations 성공: creator 조회 후 작품 목록 반환")
    void getMyCreations_success() {
        long memberId = 1L;
        long creatorId = 10L;

        Creator creator = mock(Creator.class);
        given(creator.getId()).willReturn(creatorId);
        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.of(creator));

        CreationListProjection p1 = mock(CreationListProjection.class);
        given(p1.getCreationId()).willReturn(100L);
        given(p1.getTitle()).willReturn("작품A");
        given(p1.getStorageKey()).willReturn("upload/2026/poster-a.jpg");

        CreationListProjection p2 = mock(CreationListProjection.class);
        given(p2.getCreationId()).willReturn(200L);
        given(p2.getTitle()).willReturn("작품B");
        given(p2.getStorageKey()).willReturn(null);

        given(creationRepository.findAllByCreatorIdWithThumbnails(creatorId, CreationThumbnailType.POSTER))
                .willReturn(List.of(p1, p2));

        List<CreationListResponse> result = creationService.getMyCreations(memberId);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).creationId()).isEqualTo(100L);
        assertThat(result.get(0).title()).isEqualTo("작품A");
        assertThat(result.get(0).posterThumbnailUrl()).isEqualTo(CDN_BASE + "/upload/2026/poster-a.jpg");

        assertThat(result.get(1).creationId()).isEqualTo(200L);
        assertThat(result.get(1).title()).isEqualTo("작품B");
        assertThat(result.get(1).posterThumbnailUrl()).isNull();

        then(creatorRepository).should(times(1)).findByMemberId(memberId);
        then(creationRepository).should(times(1)).findAllByCreatorIdWithThumbnails(creatorId, CreationThumbnailType.POSTER);
    }

    @Test
    @DisplayName("getMyCreations 성공: 등록한 작품이 없으면 빈 리스트 반환")
    void getMyCreations_empty() {
        long memberId = 1L;
        long creatorId = 10L;

        Creator creator = mock(Creator.class);
        given(creator.getId()).willReturn(creatorId);
        given(creatorRepository.findByMemberId(memberId)).willReturn(Optional.of(creator));
        given(creationRepository.findAllByCreatorIdWithThumbnails(creatorId, CreationThumbnailType.POSTER))
                .willReturn(List.of());

        List<CreationListResponse> result = creationService.getMyCreations(memberId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMyCreations 실패: creator 없으면 CreatorNotFoundException")
    void getMyCreations_creatorNotFound() {
        given(creatorRepository.findByMemberId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.getMyCreations(99L))
                .isInstanceOf(CreatorNotFoundException.class);

        then(creationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getCreationsByDay 성공(VIEWS): 첫 페이지 조회 - 커서 없이 호출, hasNext=true, nextCursor 반환")
    void getCreationsByDay_views_firstPage() {
        PublishDay day = PublishDay.MON;
        int size = 2;

        CreationSeekRow row1 = mock(CreationSeekRow.class);
        given(row1.getId()).willReturn(10L);
        given(row1.getTitle()).willReturn("작품A");
        given(row1.getStorageKey()).willReturn("upload/poster-a.jpg");

        CreationSeekRow row2 = mock(CreationSeekRow.class);
        given(row2.getId()).willReturn(20L);
        given(row2.getLongValue()).willReturn(300L);
        given(row2.getTitle()).willReturn("작품B");
        given(row2.getStorageKey()).willReturn("upload/poster-b.jpg");

        given(creationRepository.findByDayOrderByViewsSeek("MON", null, null, size))
                .willReturn(List.of(row1, row2));

        CursorSliceResponse<CreationListItem> resp = creationService.getCreationsByDay(day, CreationSort.VIEWS, null, size);

        assertThat(resp.items()).hasSize(2);
        assertThat(resp.hasNext()).isTrue();
        assertThat(resp.nextCursor()).isNotNull();
        assertThat(resp.nextCursor().id()).isEqualTo(20L);
        assertThat(resp.nextCursor().value()).isEqualTo(300.0);
        assertThat(resp.nextCursor().tie()).isNull();

        assertThat(resp.items().get(0).id()).isEqualTo(10L);
        assertThat(resp.items().get(0).title()).isEqualTo("작품A");
        assertThat(resp.items().get(0).posterUrl()).isEqualTo(CDN_BASE + "/upload/poster-a.jpg");

        assertThat(resp.items().get(1).id()).isEqualTo(20L);
        assertThat(resp.items().get(1).posterUrl()).isEqualTo(CDN_BASE + "/upload/poster-b.jpg");
    }

    @Test
    @DisplayName("getCreationsByDay 성공(POPULAR): 커서 있으면 cursorValue/cursorId 전달")
    void getCreationsByDay_popular_withCursor() {
        PublishDay day = PublishDay.TUE;
        int size = 2;
        SeekCursor cursor = new SeekCursor(20L, 300.0, null);

        CreationSeekRow row = mock(CreationSeekRow.class);
        given(row.getId()).willReturn(30L);
        given(row.getLongValue()).willReturn(100L);
        given(row.getTitle()).willReturn("작품C");
        given(row.getStorageKey()).willReturn("upload/poster-c.jpg");

        given(creationRepository.findByDayOrderByLikesSeek("TUE", 300L, 20L, size))
                .willReturn(List.of(row));

        CursorSliceResponse<CreationListItem> resp = creationService.getCreationsByDay(day, CreationSort.POPULAR, cursor, size);

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.hasNext()).isFalse();
        assertThat(resp.items().get(0).id()).isEqualTo(30L);

        then(creationRepository).should(times(1)).findByDayOrderByLikesSeek("TUE", 300L, 20L, size);
    }

    @Test
    @DisplayName("getCreationsByDay 성공(RATING): 별점순 커서 파라미터(avg, ratingCount, id) 올바르게 전달")
    void getCreationsByDay_rating_withCursor() {
        PublishDay day = PublishDay.WED;
        int size = 2;
        SeekCursor cursor = new SeekCursor(10L, 4.5, 100L);

        CreationSeekRow row = mock(CreationSeekRow.class);
        given(row.getId()).willReturn(20L);
        given(row.getDoubleValue()).willReturn(4.2);
        given(row.getTie()).willReturn(80L);
        given(row.getTitle()).willReturn("별점 작품");
        given(row.getStorageKey()).willReturn(null);

        given(creationRepository.findByDayOrderByRatingSeek("WED", 4.5, 100L, 10L, size))
                .willReturn(List.of(row));

        CursorSliceResponse<CreationListItem> resp = creationService.getCreationsByDay(day, CreationSort.RATING, cursor, size);

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.hasNext()).isFalse();
        assertThat(resp.nextCursor().id()).isEqualTo(20L);
        assertThat(resp.nextCursor().value()).isEqualTo(4.2);
        assertThat(resp.nextCursor().tie()).isEqualTo(80L);
        assertThat(resp.items().get(0).posterUrl()).isNull();

        then(creationRepository).should(times(1)).findByDayOrderByRatingSeek("WED", 4.5, 100L, 10L, size);
    }

    @Test
    @DisplayName("getCreationsByDay 성공: 결과 없으면 빈 items + hasNext=false + nextCursor=null 반환")
    void getCreationsByDay_empty() {
        given(creationRepository.findByDayOrderByViewsSeek("SUN", null, null, 10))
                .willReturn(List.of());

        CursorSliceResponse<CreationListItem> resp = creationService.getCreationsByDay(PublishDay.SUN, CreationSort.VIEWS, null, 10);

        assertThat(resp.items()).isEmpty();
        assertThat(resp.hasNext()).isFalse();
        assertThat(resp.nextCursor()).isNull();
    }

    @Test
    @DisplayName("getCreationsByDay 성공(RATING): 커서 null이면 cursorAvg=null, cursorRatingCount=0 전달")
    void getCreationsByDay_rating_firstPage() {
        int size = 5;

        given(creationRepository.findByDayOrderByRatingSeek("FRI", null, 0L, null, size))
                .willReturn(List.of());

        CursorSliceResponse<CreationListItem> resp = creationService.getCreationsByDay(PublishDay.FRI, CreationSort.RATING, null, size);

        assertThat(resp.items()).isEmpty();
        then(creationRepository).should(times(1)).findByDayOrderByRatingSeek("FRI", null, 0L, null, size);
    }
}
