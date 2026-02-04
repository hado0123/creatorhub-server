package com.creatorhub.service;

import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.fileUpload.ManuscriptsMarkResult;
import com.creatorhub.dto.fileUpload.FileObjectResponse;
import com.creatorhub.dto.fileUpload.ThumbnailMarkResult;
import com.creatorhub.dto.fileUpload.DerivativesCheckResponse;
import com.creatorhub.dto.fileUpload.s3.ResizeCompleteRequest;
import com.creatorhub.entity.FileObject;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.repository.FileObjectRepository;
import com.creatorhub.service.fileObject.FileObjectService;
import com.creatorhub.service.fileObject.s3.ImageProcessingChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FileObjectServiceTest {

    @Mock FileObjectRepository fileObjectRepository;
    @Mock ImageProcessingChecker checker;

    @InjectMocks
    FileObjectService service;

    private static FileObject newFoWithId(Long id, String key) {
        FileObject fo = FileObject.create(
                key,
                "orig.jpg",
                FileObjectStatus.INIT,
                "image/jpeg",
                0L
        );
        ReflectionTestUtils.setField(fo, "id", id);
        return fo;
    }

    private static ResizeCompleteRequest.DerivedFile df(String key, int w, int h, long sizeBytes) {
        return new ResizeCompleteRequest.DerivedFile(key, w, h, sizeBytes);
    }

    private static ResizeCompleteRequest newResizeReq(String baseKey, List<ResizeCompleteRequest.DerivedFile> derived) {
        return new ResizeCompleteRequest(
                "test-bucket",
                "trigger/key.jpg",
                baseKey,
                derived,
                "2026-01-27T16:00:00"
        );
    }

    // ------------------------------------------------------------------------
    // markThumbnailReady
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("markThumbnailReady 성공: 1MB 이하이면 size 저장 + READY + ready=true 응답")
    void markThumbnailReady_success() {
        Long fileObjectId = 10L;
        String key = "upload/2026/01/27/xxx" + ThumbnailKeys.POSTER_SUFFIX;

        FileObject fo = newFoWithId(fileObjectId, key);

        given(fileObjectRepository.findById(fileObjectId)).willReturn(Optional.of(fo));
        given(checker.fetchSize(key)).willReturn(500_000L); // 0.5MB

        ThumbnailMarkResult resp = service.markThumbnailReady(fileObjectId);

        assertThat(resp.fileObjectId()).isEqualTo(fileObjectId);
        assertThat(resp.ready()).isTrue();
        assertThat(resp.sizeBytes()).isEqualTo(500_000L);
        assertThat(resp.maxBytes()).isEqualTo(1L * 1024 * 1024);

        assertThat(fo.getStatus()).isEqualTo(FileObjectStatus.READY);
        assertThat(fo.getSizeBytes()).isEqualTo(500_000L);

        then(checker).should(never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("markThumbnailReady 실패: 1MB 초과이면 FAILED + size 저장 + S3 delete + ready=false 응답")
    void markThumbnailReady_fail_overMax() {
        Long fileObjectId = 11L;
        String key = "upload/2026/01/27/yyy" + ThumbnailKeys.POSTER_SUFFIX;

        FileObject fo = newFoWithId(fileObjectId, key);

        given(fileObjectRepository.findById(fileObjectId)).willReturn(Optional.of(fo));
        given(checker.fetchSize(key)).willReturn(1_200_000L); // 1.2MB

        ThumbnailMarkResult resp = service.markThumbnailReady(fileObjectId);

        assertThat(resp.fileObjectId()).isEqualTo(fileObjectId);
        assertThat(resp.ready()).isFalse();
        assertThat(resp.sizeBytes()).isEqualTo(1_200_000L);
        assertThat(resp.maxBytes()).isEqualTo(1L * 1024 * 1024);

        assertThat(fo.getStatus()).isEqualTo(FileObjectStatus.FAILED);
        assertThat(fo.getSizeBytes()).isEqualTo(1_200_000L);

        then(checker).should(times(1)).deleteObject(key);
    }

    @Test
    @DisplayName("markThumbnailReady 실패: fileObjectId 없으면 FileObjectNotFoundException")
    void markThumbnailReady_notFound() {
        given(fileObjectRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.markThumbnailReady(999L))
                .isInstanceOf(FileObjectNotFoundException.class);
    }

    // ------------------------------------------------------------------------
    // markManuscriptsReady
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("markManuscriptsReady 성공: 중복 제거 후 전체 READY 처리")
    void markManuscriptsReady_allReady() {
        List<Long> ids = List.of(1L, 2L, 2L);

        FileObject fo1 = newFoWithId(1L, "upload/2026/01/27/99/1" + ThumbnailKeys.MANUSCRIPT_SUFFIX);
        FileObject fo2 = newFoWithId(2L, "upload/2026/01/27/99/2" + ThumbnailKeys.MANUSCRIPT_SUFFIX);

        given(fileObjectRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(fo1, fo2));
        given(checker.fetchSize(fo1.getStorageKey())).willReturn(100_000L);
        given(checker.fetchSize(fo2.getStorageKey())).willReturn(200_000L);

        ManuscriptsMarkResult resp = service.markManuscriptsReady(ids);

        assertThat(resp.total()).isEqualTo(2);
        assertThat(resp.readyCount()).isEqualTo(2);
        assertThat(resp.failedCount()).isEqualTo(0);
        assertThat(resp.failedItems()).isEmpty();

        assertThat(fo1.getStatus()).isEqualTo(FileObjectStatus.READY);
        assertThat(fo2.getStatus()).isEqualTo(FileObjectStatus.READY);

        then(checker).should(never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("markManuscriptsReady 성공: 일부 5MB 초과면 FAILED + deleteObject + failed 목록 반환")
    void markManuscriptsReady_partialFail() {
        List<Long> ids = List.of(1L, 2L);

        FileObject fo1 = newFoWithId(1L, "upload/2026/01/27/99/1" + ThumbnailKeys.MANUSCRIPT_SUFFIX);
        FileObject fo2 = newFoWithId(2L, "upload/2026/01/27/99/2" + ThumbnailKeys.MANUSCRIPT_SUFFIX);

        given(fileObjectRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(fo1, fo2));

        long max = 5L * 1024 * 1024;
        given(checker.fetchSize(fo1.getStorageKey())).willReturn(max + 1);   // fail
        given(checker.fetchSize(fo2.getStorageKey())).willReturn(max - 100); // ok

        ManuscriptsMarkResult resp = service.markManuscriptsReady(ids);

        assertThat(resp.total()).isEqualTo(2);
        assertThat(resp.readyCount()).isEqualTo(1);
        assertThat(resp.failedCount()).isEqualTo(1);
        assertThat(resp.failedItems()).hasSize(1);

        ManuscriptsMarkResult.FailedItem failedItem = resp.failedItems().get(0);
        assertThat(failedItem.fileObjectId()).isEqualTo(1L);
        assertThat(failedItem.maxBytes()).isEqualTo(max);
        assertThat(failedItem.sizeBytes()).isGreaterThan(max);

        assertThat(fo1.getStatus()).isEqualTo(FileObjectStatus.FAILED);
        assertThat(fo2.getStatus()).isEqualTo(FileObjectStatus.READY);

        then(checker).should(times(1)).deleteObject(fo1.getStorageKey());
        then(checker).should(never()).deleteObject(fo2.getStorageKey());
    }

    @Test
    @DisplayName("markManuscriptsReady 실패: 일부 id가 존재하지 않으면 FileObjectNotFoundException(누락 id 포함)")
    void markManuscriptsReady_missingIds() {
        List<Long> ids = List.of(1L, 2L, 3L);

        FileObject fo1 = newFoWithId(1L, "upload/2026/01/27/99/1" + ThumbnailKeys.MANUSCRIPT_SUFFIX);
        FileObject fo2 = newFoWithId(2L, "upload/2026/01/27/99/2" + ThumbnailKeys.MANUSCRIPT_SUFFIX);

        given(fileObjectRepository.findAllById(List.of(1L, 2L, 3L))).willReturn(List.of(fo1, fo2));

        assertThatThrownBy(() -> service.markManuscriptsReady(ids))
                .isInstanceOf(FileObjectNotFoundException.class)
                .hasMessageContaining("3");
    }

    // ------------------------------------------------------------------------
    // markFailed
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("markFailed 성공: 존재하면 status FAILED로 변경")
    void markFailed_success() {
        FileObject fo = newFoWithId(77L, "upload/2026/01/27/99/77" + ThumbnailKeys.MANUSCRIPT_SUFFIX);
        given(fileObjectRepository.findById(77L)).willReturn(Optional.of(fo));

        service.markFailed(77L);

        assertThat(fo.getStatus()).isEqualTo(FileObjectStatus.FAILED);
    }

    @Test
    @DisplayName("markFailed 실패: 없으면 FileObjectNotFoundException")
    void markFailed_notFound() {
        given(fileObjectRepository.findById(88L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.markFailed(88L))
                .isInstanceOf(FileObjectNotFoundException.class);
    }

    // ------------------------------------------------------------------------
    // resizeComplete
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("resizeComplete 실패: original(horizontal) storageKey가 없으면 FileObjectNotFoundException")
    void resizeComplete_originalNotFound() {
        String baseKey = "upload/2026/01/27/abc";
        String originalKey = baseKey + ThumbnailKeys.HORIZONTAL_SUFFIX;

        ResizeCompleteRequest req = newResizeReq(
                baseKey,
                List.of(df(baseKey + ThumbnailKeys.DERIVED_SUFFIXES.getFirst(), 320, 180, 123L))
        );

        given(fileObjectRepository.findByStorageKey(originalKey)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.resizeComplete(req))
                .isInstanceOf(FileObjectNotFoundException.class)
                .hasMessageContaining(originalKey);
    }

    @Test
    @DisplayName("resizeComplete 성공: 기존 파생 없으면 6개 insert(saveAll 1회) + 최종 목록 조회")
    void resizeComplete_insertAll() {
        String baseKey = "upload/2026/01/27/xyz";
        String originalKey = baseKey + ThumbnailKeys.HORIZONTAL_SUFFIX;

        FileObject original = FileObject.create(originalKey, "orig.jpg", FileObjectStatus.READY, "image/jpeg", 111L);
        ReflectionTestUtils.setField(original, "id", 100L);

        ResizeCompleteRequest req = newResizeReq(
                baseKey,
                List.of(
                        df(baseKey + ThumbnailKeys.DERIVED_SUFFIXES.get(0), 320, 180, 10L),
                        df(baseKey + ThumbnailKeys.DERIVED_SUFFIXES.get(1), 640, 360, 20L)
                )
        );

        List<String> expectedKeys = ThumbnailKeys.DERIVED_SUFFIXES.stream()
                .map(s -> baseKey + s)
                .toList();

        Map<String, Long> sizeMap = new HashMap<>();
        long max = 1L * 1024 * 1024;

        for (int i = 0; i < expectedKeys.size(); i++) {
            String k = expectedKeys.get(i);
            if (i == 0) sizeMap.put(k, 123_000L);      // READY
            else if (i == 1) sizeMap.put(k, 0L);       // FAILED
            else if (i == 2) sizeMap.put(k, max + 1);  // FAILED
            else sizeMap.put(k, 456_000L);             // READY
        }

        DerivativesCheckResponse checkerResp = DerivativesCheckResponse.ready(sizeMap);

        given(fileObjectRepository.findByStorageKey(originalKey)).willReturn(Optional.of(original));
        given(checker.getDerivedSizes(eq(baseKey), eq(req.derivedFiles()))).willReturn(checkerResp);
        given(fileObjectRepository.findByStorageKeyIn(expectedKeys)).willReturn(List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileObject>> insertCaptor = ArgumentCaptor.forClass(List.class);

        given(fileObjectRepository.findByStorageKeyStartingWith(baseKey))
                .willReturn(List.of(original));

        List<FileObjectResponse> resp = service.resizeComplete(req);

        then(fileObjectRepository).should(times(1)).saveAll(insertCaptor.capture());

        List<FileObject> inserted = insertCaptor.getValue();
        assertThat(inserted).hasSize(ThumbnailKeys.DERIVED_SUFFIXES.size());

        Map<String, FileObject> insertedMap = inserted.stream()
                .collect(Collectors.toMap(FileObject::getStorageKey, f -> f));

        for (String k : expectedKeys) {
            FileObject fo = insertedMap.get(k);
            assertThat(fo).isNotNull();

            long size = sizeMap.getOrDefault(k, 0L);
            assertThat(fo.getSizeBytes()).isEqualTo(size);

            if (size == 0L || size > max) {
                assertThat(fo.getStatus()).isEqualTo(FileObjectStatus.FAILED);
            } else {
                assertThat(fo.getStatus()).isEqualTo(FileObjectStatus.READY);
            }

            assertThat(fo.getOriginalFilename()).isEqualTo(original.getOriginalFilename());
            assertThat(fo.getContentType()).isEqualTo(original.getContentType());
        }

        assertThat(resp).isNotNull();
        then(fileObjectRepository).should(times(1)).findByStorageKeyStartingWith(baseKey);
    }

    @Test
    @DisplayName("resizeComplete 성공: 일부 existing은 update(READY/FAILED/size), 나머지는 insert")
    void resizeComplete_updateAndInsertMix() {
        String baseKey = "upload/2026/01/27/mix";
        String originalKey = baseKey + ThumbnailKeys.HORIZONTAL_SUFFIX;

        FileObject original = FileObject.create(originalKey, "orig.jpg", FileObjectStatus.READY, "image/jpeg", 111L);
        ReflectionTestUtils.setField(original, "id", 200L);

        ResizeCompleteRequest req = newResizeReq(
                baseKey,
                List.of(df(baseKey + ThumbnailKeys.DERIVED_SUFFIXES.getFirst(), 320, 180, 10L))
        );

        List<String> expectedKeys = ThumbnailKeys.DERIVED_SUFFIXES.stream()
                .map(s -> baseKey + s)
                .toList();

        long max = 1L * 1024 * 1024;

        Map<String, Long> sizeMap = new HashMap<>();
        for (int i = 0; i < expectedKeys.size(); i++) {
            String k = expectedKeys.get(i);
            if (i == 2) sizeMap.put(k, 0L);      // FAILED
            else sizeMap.put(k, 333_000L);       // READY
        }

        DerivativesCheckResponse checkerResp = DerivativesCheckResponse.ready(sizeMap);

        FileObject exist1 = FileObject.create(expectedKeys.get(0), "orig.jpg", FileObjectStatus.INIT, "image/jpeg", 1L);
        ReflectionTestUtils.setField(exist1, "id", 301L);

        FileObject exist2 = FileObject.create(expectedKeys.get(2), "orig.jpg", FileObjectStatus.READY, "image/jpeg", 999L);
        ReflectionTestUtils.setField(exist2, "id", 302L);

        given(fileObjectRepository.findByStorageKey(originalKey)).willReturn(Optional.of(original));
        given(checker.getDerivedSizes(eq(baseKey), eq(req.derivedFiles()))).willReturn(checkerResp);
        given(fileObjectRepository.findByStorageKeyIn(expectedKeys)).willReturn(List.of(exist1, exist2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileObject>> insertCaptor = ArgumentCaptor.forClass(List.class);

        // 실행
        service.resizeComplete(req);

        // existing update 검증
        assertThat(exist1.getStatus()).isEqualTo(FileObjectStatus.READY);
        assertThat(exist1.getSizeBytes()).isEqualTo(333_000L);

        assertThat(exist2.getStatus()).isEqualTo(FileObjectStatus.FAILED);
        assertThat(exist2.getSizeBytes()).isEqualTo(0L);

        // insert 6-2(이미 insert된 row)=4개
        then(fileObjectRepository).should(times(1)).saveAll(insertCaptor.capture());
        List<FileObject> inserted = insertCaptor.getValue();
        assertThat(inserted).hasSize(ThumbnailKeys.DERIVED_SUFFIXES.size() - 2);

        for (FileObject insertedFo : inserted) {
            long size = sizeMap.getOrDefault(insertedFo.getStorageKey(), 0L);
            if (size == 0L || size > max) {
                assertThat(insertedFo.getStatus()).isEqualTo(FileObjectStatus.FAILED);
            } else {
                assertThat(insertedFo.getStatus()).isEqualTo(FileObjectStatus.READY);
            }
        }
    }
}

