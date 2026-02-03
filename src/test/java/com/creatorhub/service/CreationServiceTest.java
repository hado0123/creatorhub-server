package com.creatorhub.service;

import com.creatorhub.constant.*;
import com.creatorhub.dto.creation.CreationRequest;
import com.creatorhub.entity.Creation;
import com.creatorhub.entity.Creator;
import com.creatorhub.entity.FileObject;
import com.creatorhub.entity.Hashtag;
import com.creatorhub.exception.creator.CreatorNotFoundException;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.CreatorRepository;
import com.creatorhub.repository.FileObjectRepository;
import com.creatorhub.repository.HashtagRepository;
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

    @InjectMocks CreationService creationService;

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

    private static CreationRequest validReq(Long creatorId,
                                            Set<Long> hashtagIds,
                                            Long horizontalId,
                                            Long posterId) {
        return new CreationRequest(
                creatorId,
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
        long creatorId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;
        Set<Long> hashtagIds = Set.of(100L, 101L);

        CreationRequest req = validReq(creatorId, hashtagIds, horizontalId, posterId);

        Creator creator = mock(Creator.class);
        given(creatorRepository.findById(creatorId)).willReturn(Optional.of(creator));

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

        Long creationId = creationService.createCreation(req);

        assertThat(creationId).isEqualTo(999L);

        then(creatorRepository).should(times(1)).findById(creatorId);
        then(fileObjectRepository).should(times(1)).findById(horizontalId);
        then(fileObjectRepository).should(times(1)).findByStorageKeyIn(thumbKeys);
        then(fileObjectRepository).should(times(1)).findById(posterId);
        then(hashtagRepository).should(times(1)).findByIdIn(hashtagIds);
        then(creationRepository).should(times(1)).save(any(Creation.class));
    }

    @Test
    @DisplayName("createCreation 실패: creator 없으면 CreatorNotFoundException")
    void createCreation_creatorNotFound() {
        long creatorId = 1L;

        CreationRequest req = validReq(creatorId, Set.of(100L), 10L, 20L);

        given(creatorRepository.findById(creatorId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.createCreation(req))
                .isInstanceOf(CreatorNotFoundException.class);

        then(creationRepository).should(never()).save(any());
        then(fileObjectRepository).shouldHaveNoInteractions();
        then(hashtagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createCreation 실패: 가로형 원본 FileObject 없으면 FileObjectNotFoundException")
    void createCreation_horizontalOriginalNotFound() {
        CreationRequest req = validReq(1L, Set.of(100L), 10L, 20L);

        given(creatorRepository.findById(1L))
                .willReturn(Optional.of(mock(Creator.class)));

        given(fileObjectRepository.findById(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> creationService.createCreation(req))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(fileObjectRepository).should(never()).findByStorageKeyIn(anyList());
        then(fileObjectRepository).should(never()).findById(20L);
        then(hashtagRepository).shouldHaveNoInteractions();
        then(creationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCreation 실패: 썸네일 7개 중 누락 있으면 FileObjectNotFoundException")
    void createCreation_thumbnailMissing() {
        long creatorId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;

        CreationRequest req = validReq(creatorId, Set.of(100L), horizontalId, posterId);

        given(creatorRepository.findById(creatorId)).willReturn(Optional.of(mock(Creator.class)));

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

        assertThatThrownBy(() -> creationService.createCreation(req))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(fileObjectRepository).should(never()).findById(posterId);
        then(creationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCreation 실패: 포스터 원본 FileObject 없으면 FileObjectNotFoundException")
    void createCreation_posterOriginalNotFound() {
        long creatorId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;

        CreationRequest req = validReq(creatorId, Set.of(100L), horizontalId, posterId);

        given(creatorRepository.findById(creatorId)).willReturn(Optional.of(mock(Creator.class)));

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

        assertThatThrownBy(() -> creationService.createCreation(req))
                .isInstanceOf(FileObjectNotFoundException.class);

        then(creationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createCreation 실패: hashtagIds 중 누락이 있으면 IllegalArgumentException")
    void createCreation_hashtagMissing() {
        long creatorId = 1L;
        long horizontalId = 10L;
        long posterId = 20L;

        Set<Long> hashtagIds = Set.of(100L, 101L, 102L);
        CreationRequest req = validReq(creatorId, hashtagIds, horizontalId, posterId);

        given(creatorRepository.findById(creatorId)).willReturn(Optional.of(mock(Creator.class)));

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

        assertThatThrownBy(() -> creationService.createCreation(req))
                .isInstanceOf(IllegalArgumentException.class);

        then(creationRepository).should(never()).save(any());
    }
}
