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
     * status 변경(UPLOADED)
     */
    @Transactional // 더티체킹
    public void markReady(Long fileObjectId) {
        FileObject fo = fileObjectRepository.findById(fileObjectId)
                .orElseThrow(() -> new FileObjectNotFoundException("해당 FileObject를 찾을 수 없습니다: " + fileObjectId));

        long originalSize = checker.fetchSize(fo.getStorageKey());

        fo.markReady();
        fo.markSize(originalSize);
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

        List<FileObject> toInsert = new ArrayList<>();

        // 3. 6개 모두 없으면 insert 있으면 update
        for (String key : derivedKeys) {
            long sizeBytes = sizeByKey.getOrDefault(key, 0L);

            boolean isMissing = missingSet.contains(key);
            FileObjectStatus status = isMissing ? FileObjectStatus.FAILED : FileObjectStatus.READY;

            FileObject fo = existingMap.get(key);

            if (fo == null) {
                // 없으면 insert
                toInsert.add(
                        FileObject.create(
                                key,
                                original.getOriginalFilename(),
                                status,
                                original.getContentType(),
                                sizeBytes
                        )
                );
            } else {
                // 있으면 update
                if (status == FileObjectStatus.READY) fo.markReady();
                else fo.markFailed();

                fo.markSize(sizeBytes);
            }
        }

        // 4. 동시 폴링 대비: UNIQUE(storage_key) 기준으로 중복 insert는 무시
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
