package com.creatorhub.service;

import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.fileUpload.ManuscriptsMarkResult;
import com.creatorhub.dto.fileUpload.DerivativesCheckResponse;
import com.creatorhub.dto.fileUpload.FileObjectResponse;
import com.creatorhub.dto.fileUpload.ThumbnailMarkResult;
import com.creatorhub.dto.s3.ResizeCompleteRequest;
import com.creatorhub.entity.FileObject;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.repository.FileObjectRepository;
import com.creatorhub.service.s3.ImageProcessingChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.creatorhub.common.logging.LogMasking.maskStoragekey;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileObjectService {
    private final FileObjectRepository fileObjectRepository;
    private final ImageProcessingChecker checker;

    /**
     * 썸네일 status 변경(Ready), 사이즈 insert
     */
    @Transactional
    public ThumbnailMarkResult markThumbnailReady(Long fileObjectId) {
        FileObject fo = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + fileObjectId));

        long size = checker.fetchSize(fo.getStorageKey());
        long maxSize = 1L * 1024 * 1024;

        // 썸네일 이미지 사이즈가 1MB 초과시
        if (size > maxSize) {
            fo.markFailed();
            fo.markSize(size);

            checker.deleteObject(fo.getStorageKey());
            log.warn("S3에 업로드된 썸네일 파일 사이즈의 허용 용량(1MB)을 초과했습니다 - uploaded size: {}, storageKey: {}",
                    size, maskStoragekey(fo.getStorageKey())
            );

            return new ThumbnailMarkResult(fo.getId(), false, size, maxSize);
        }

        fo.markSize(size);
        fo.markReady();

        return new ThumbnailMarkResult(fo.getId(), true, size, maxSize);
    }

    /**
     * 원고 status 변경(Ready), 사이즈 insert
     */

    @Transactional
    public ManuscriptsMarkResult markManuscriptsReady(List<Long> fileObjectIds) {
        // 1. 중복 제거
        List<Long> distinctIds = fileObjectIds.stream().distinct().toList();

        // 2. 한번에 조회
        List<FileObject> fileObjects = fileObjectRepository.findAllById(distinctIds);

        // 3. 존재하지 않는 id 체크
        if (fileObjects.size() != distinctIds.size()) {
            Set<Long> found = fileObjects.stream().map(FileObject::getId).collect(Collectors.toSet());
            List<Long> missing = distinctIds.stream().filter(id -> !found.contains(id)).toList();
            throw new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + missing);
        }

        long max = 5L * 1024 * 1024;
        List<ManuscriptsMarkResult.FailedItem> failed = new ArrayList<>();

        // 4. 상태/사이즈 처리
        // checker.fetchSize가 S3 HEAD라면 네트워크 N번 호출됨(원고 50장이면 50번)
        // 지금은 최대 50이니까 허용 가능. 나중에 최적화(배치 HEAD or S3 inventory/메타) 고려.
        for (FileObject fo : fileObjects) {
            long size = checker.fetchSize(fo.getStorageKey());

            // 원고 1장 사이즈가 5MB 초과시
            if (size > max) {
                fo.markSize(size);
                fo.markFailed();

                // S3 삭제
                checker.deleteObject(fo.getStorageKey());

                failed.add(new ManuscriptsMarkResult.FailedItem(fo.getId(), size, max));
                log.warn("S3에 업로드된 원고 파일 사이즈가 허용 용량(5MB)을 초과했습니다 - uploaded size: {}, storageKey: {}",
                        size, maskStoragekey(fo.getStorageKey())
                );
                continue;
            }

            fo.markSize(size);
            fo.markReady();
        }

        int total = distinctIds.size();
        int failedCount = failed.size();
        int readyCount = total - failedCount;

        return new ManuscriptsMarkResult(total, readyCount, failedCount, failed);
    }


    /**
     * file_object 파일 status 변경(Failed), 사이즈 insert
     */
    @Transactional
    public void markFailed(Long fileObjectId) {
        FileObject fo = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + fileObjectId));

        fo.markFailed();
    }


    /**
     * 람다에서 백엔드 콜백시 리사이징 이미지 file_object 테이블에 insert or update
     */
    @Transactional
    public List<FileObjectResponse> resizeComplete(ResizeCompleteRequest req) {

        String baseKey = req.baseKey();
        String originalKey = baseKey + ThumbnailKeys.HORIZONTAL_SUFFIX;

        FileObject original = fileObjectRepository.findByStorageKey(originalKey)
                .orElseThrow(() -> new FileObjectNotFoundException(
                        "해당 StorageKey를 가진 FileObject를 찾을 수 없습니다: " + originalKey
                ));

        DerivativesCheckResponse resp = checker.getDerivedSizes(baseKey, req.derivedFiles());

        // 6개 키 생성
        List<String> expectedKeys = ThumbnailKeys.DERIVED_SUFFIXES.stream()
                .map(suffix -> baseKey + suffix)
                .toList();

        // 기존 존재하는 것 조회
        Map<String, FileObject> existingMap = fileObjectRepository.findByStorageKeyIn(expectedKeys).stream()
                .collect(Collectors.toMap(FileObject::getStorageKey, fo -> fo));

        List<FileObject> toInsert = new ArrayList<>();

        for (String key : expectedKeys) {
            long sizeBytes = resp.derivedSizeByKey().getOrDefault(key, 0L);
            long maxSize = 1L * 1024 * 1024; // 1MB

            // size가 0또는 1MB 초과면 '없음(업로드 실패)'로 간주
            FileObjectStatus status = sizeBytes == 0L || sizeBytes > maxSize?
                    FileObjectStatus.FAILED : FileObjectStatus.READY;

            FileObject fo = existingMap.get(key);

            if (fo == null) {
                toInsert.add(FileObject.create(
                        key,
                        original.getOriginalFilename(),
                        status,
                        original.getContentType(),
                        sizeBytes
                ));
            } else {
                if (status == FileObjectStatus.READY) fo.markReady();
                else fo.markFailed();
                fo.markSize(sizeBytes);
            }
        }

        if (!toInsert.isEmpty()) {
            fileObjectRepository.saveAll(toInsert);
        }

        List<FileObject> allFiles = fileObjectRepository.findByStorageKeyStartingWith(baseKey);
        return FileObjectResponse.listFrom(allFiles);
    }
}
