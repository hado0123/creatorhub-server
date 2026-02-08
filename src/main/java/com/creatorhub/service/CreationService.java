package com.creatorhub.service;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.creation.CreationRequest;
import com.creatorhub.entity.*;
import com.creatorhub.exception.creator.CreatorNotFoundException;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.exception.hashtag.HashtagNotFoundException;
import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.CreatorRepository;
import com.creatorhub.repository.FileObjectRepository;
import com.creatorhub.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreationService {
    private final CreationRepository creationRepository;
    private final FileObjectRepository fileObjectRepository;
    private final CreatorRepository creatorRepository;
    private final HashtagRepository hashtagRepository;

    @Transactional
    public Long createCreation(CreationRequest req) {

        // 1. 작가 조회
        Creator creator = creatorRepository.findById(req.creatorId())
                .orElseThrow(CreatorNotFoundException::new);

        // 2. Creation 생성
        Creation creation = Creation.create(
                creator,
                req.format(),
                req.genre(),
                req.title(),
                req.plot(),
                req.isPublic()
        );
        creation.setPublishDays(req.publishDays());

        // 3. 가로형 원본(READY) 조회 + baseKey
        FileObject horizontalOriginal = fileObjectRepository.findById(req.horizontalOriginalFileObjectId())
                .orElseThrow(() -> new FileObjectNotFoundException(
                        "가로형 FileObject를 찾을 수 없습니다:" + req.horizontalOriginalFileObjectId()
                ));
        String baseKey = horizontalOriginal.extractBaseKey();

        // 4. 썸네일 7개(FileObject) 모두 조회 + 누락 검증
        Map<String, FileObject> byKey = loadAllThumbnailFileObjectsOrThrow(baseKey);

        // 5. 포스터 원본 조회
        FileObject posterOriginal = fileObjectRepository.findById(req.posterOriginalFileObjectId())
                .orElseThrow(() -> new FileObjectNotFoundException(
                        "포스터형 FileObject를 찾을 수 없습니다:" + req.posterOriginalFileObjectId()
                ));

        // 6. 썸네일 엔티티 생성/연결
        attachThumbnails(creation, posterOriginal, byKey, baseKey);

        // 7. 해시태그 연결(누락 검증 포함)
        attachHashtagsOrThrow(creation, req.hashtagIds());

        // 8. 저장
        return creationRepository.save(creation).getId();
    }

    private void attachThumbnails(
            Creation creation,
            FileObject posterOriginal,
            Map<String, FileObject> byKey,
            String baseKey
    ) {
        // 포스터형 - displayOrder=0
        CreationThumbnail posterOriginalThumb = CreationThumbnail.create(
                creation,
                posterOriginal,
                CreationThumbnailType.POSTER,
                0,
                null
        );
        creation.addThumbnail(posterOriginalThumb);

        // 가로형 - displayOrder=0
        CreationThumbnail horizontalOriginalThumb = CreationThumbnail.create(
                creation,
                byKey.get(baseKey + ThumbnailKeys.HORIZONTAL_SUFFIX),
                CreationThumbnailType.HORIZONTAL,
                0,
                null
        );
        creation.addThumbnail(horizontalOriginalThumb);

        // 파생 6개 - displayOrder=1..6, sourceImage=horizontalOriginalThumb
        int order = 1;
        for (String suffix : ThumbnailKeys.DERIVED_SUFFIXES) {
            FileObject derivedFo = byKey.get(baseKey + suffix);

            CreationThumbnail derivedThumb = CreationThumbnail.create(
                    creation,
                    derivedFo,
                    CreationThumbnailType.DERIVED,
                    order,
                    horizontalOriginalThumb
            );
            creation.addThumbnail(derivedThumb);
            order++;
        }
    }

    private Map<String, FileObject> loadAllThumbnailFileObjectsOrThrow(String baseKey) {
        List<String> keys = ThumbnailKeys.allSuffixes().stream()
                .map(suffix -> baseKey + suffix)
                .toList();

        List<FileObject> all = fileObjectRepository.findByStorageKeyIn(keys);

        Map<String, FileObject> byKey = new HashMap<>();
        for (FileObject fo : all) {
            byKey.put(fo.getStorageKey(), fo);
        }

        List<String> missing = new ArrayList<>();
        for (String k : keys) {
            if (!byKey.containsKey(k)) missing.add(k);
        }
        if (!missing.isEmpty()) {
            throw new FileObjectNotFoundException("썸네일 파일이 누락되었습니다.: " + missing);
        }

        return byKey;
    }

    private void attachHashtagsOrThrow(Creation creation, Set<Long> hashtagIds) {
        List<Hashtag> hashtags = hashtagRepository.findByIdIn(hashtagIds);

        if (hashtags.size() != hashtagIds.size()) {
            Set<Long> found = hashtags.stream()
                    .map(Hashtag::getId)
                    .collect(Collectors.toSet());

            Set<Long> missing = new HashSet<>(hashtagIds);
            missing.removeAll(found);

            throw new HashtagNotFoundException("존재하지 않는 hashtagId가 포함되어 있습니다: " + missing);
        }

        for (Hashtag h : hashtags) {
            creation.addHashtag(h);
        }
    }

}
