package com.creatorhub.service;

import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.dto.DerivativesCheckResponse;
import com.creatorhub.dto.FileObjectResponse;
import com.creatorhub.entity.FileObject;
import com.creatorhub.exception.FileObjectNotFoundException;
import com.creatorhub.repository.FileObjectRepository;
import com.creatorhub.service.s3.ImageProcessingChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileObjectService {
    private final FileObjectRepository fileObjectRepository;
    private final ImageProcessingChecker checker;

    /**
     * 썸네일 status 변경(Ready), 사이즈 insert
     */
    @Transactional // 더티체킹
    public void markThumbnailReady(Long fileObjectId) {
        FileObject fo = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + fileObjectId));

        long originalSize = checker.fetchSize(fo.getStorageKey());

        fo.markReady();
        fo.markSize(originalSize);
    }

    /**
     * 원고 status 변경(Ready), 사이즈 insert
     */
    @Transactional
    public void markManuscriptsReady(List<Long> fileObjectIds) {

        // 1. 중복 제거
        List<Long> distinctIds = fileObjectIds.stream().distinct().toList();

        // 2. 한번에 조회
        List<FileObject> fileObjects = fileObjectRepository.findAllById(fileObjectIds);

        // 3. 존재하지 않는 id 체크
        if (fileObjects.size() != distinctIds.size()) {
            java.util.Set<Long> found = fileObjects.stream().map(FileObject::getId).collect(java.util.stream.Collectors.toSet());
            List<Long> missing = distinctIds.stream().filter(id -> !found.contains(id)).toList();
            throw new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + missing);
        }

        // 4. 상태/사이즈 처리
        // checker.fetchSize가 S3 HEAD라면 네트워크 N번 호출됨(원고 50장이면 50번)
        // 지금은 최대 50이니까 허용 가능. 나중에 최적화(배치 HEAD or S3 inventory/메타) 고려.
        for (FileObject fo : fileObjects) {
            long size = checker.fetchSize(fo.getStorageKey());

            fo.markReady();
            fo.markSize(size);
        }
    }


    @Transactional // 더티체킹
    public List<FileObjectResponse> checkAndGetStatus(Long fileObjectId) {
        FileObject original = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + fileObjectId));

        String baseKey = original.extractBaseKey();

        // 1. 6개 리사이징 이미지 존재 확인 & 사이즈 읽기(존재하면 size, 없으면 0 + missingKeys 기록)
        DerivativesCheckResponse resp = checker.checkDerivedAndFetchSizes(baseKey);

        Map<String, Long> sizeByKey = resp.derivedSizeByKey(); // 6개 모두 key 존재 (없으면 0)
        List<String> missingKeys = resp.missingKeys() == null ? List.of() : resp.missingKeys();
        Set<String> missingSet = new HashSet<>(missingKeys);

        List<String> derivedKeys = new ArrayList<>(sizeByKey.keySet()); // 6개 키

        // 2. DB에 이미 있는 리사이징 이미지 file_object에서 조회
        List<FileObject> existing = fileObjectRepository.findByStorageKeyIn(derivedKeys);
        Map<String, FileObject> existingMap = existing.stream()
                .collect(Collectors.toMap(FileObject::getStorageKey, fo -> fo));


        // 3. 6개 file_object 없으면 insert 있으면 update

        // 3-1. 있으면 update
        derivedKeys.stream()
                .map(existingMap::get)
                .filter(Objects::nonNull)
                .forEach(fo -> {
                    String key = fo.getStorageKey();
                    long sizeBytes = sizeByKey.getOrDefault(key, 0L);

                    if (missingSet.contains(key)) fo.markFailed();
                    else fo.markReady();

                    fo.markSize(sizeBytes);
                });


        // 3-2. 없는 것들은 toInsert 생성
        List<FileObject> toInsert = derivedKeys.stream()
                .filter(key -> !missingSet.contains(key))
                .map(key -> {
                    long sizeBytes = sizeByKey.getOrDefault(key, 0L);
                    FileObjectStatus status = missingSet.contains(key) ? FileObjectStatus.FAILED : FileObjectStatus.READY;

                    return FileObject.create(
                            key,
                            original.getOriginalFilename(),
                            status,
                            original.getContentType(),
                            sizeBytes
                    );
                })
                .toList();

        if (!toInsert.isEmpty()) {
            try {
                fileObjectRepository.saveAll(toInsert);
            } catch (DataIntegrityViolationException e) {
                log.debug(
                        "해당 리사이징 이미지 file_object가 이미 존재하므로 insert를 하지 않습니다. keys={}",
                        toInsert.stream().map(FileObject::getStorageKey).toList()
                );
            }
        }

        // 5. 원본 + 리사이징 데이터 응답
        List<FileObject> allFiles = fileObjectRepository.findByStorageKeyStartingWith(baseKey);

        return FileObjectResponse.listFrom(allFiles);
    }
}
