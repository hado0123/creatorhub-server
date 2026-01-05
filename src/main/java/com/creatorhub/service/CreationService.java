package com.creatorhub.service;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.CreationRequest;
import com.creatorhub.entity.*;
import com.creatorhub.exception.CreatorNotFoundException;
import com.creatorhub.exception.FileObjectNotFoundException;
import com.creatorhub.exception.FileObjectStatusException;
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

        // 1. 해당 작가가 있는지 확인
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

        // 3. 가로형 fileObject 조회 + baseKey 추출
        FileObject horizontalOriginal = fileObjectRepository.findById(req.horizontalOriginalFileObjectId())
                .orElseThrow(() -> new FileObjectNotFoundException("가로형 FileObject를 찾을 수 없습니다:" + req.horizontalOriginalFileObjectId()));

        log.info("horizontalOriginal 데이터 조회 결과- horizontalOriginalFileObjectId: {}, horizontalOriginal: {}", req.horizontalOriginalFileObjectId(), horizontalOriginal);

        // (선택) 상태 검증: READY 아니면 등록 막기
        if (horizontalOriginal.getStatus() != FileObjectStatus.READY) {
            throw new FileObjectStatusException("가로형 썸네일이 READY 상태가 아닙니다. status: " + horizontalOriginal.getStatus());
        }

        String baseKey = horizontalOriginal.extractBaseKey();

        // 4. 7개 key(가로형 1개 + 리사이징 6개) 만들어서 FileObject에서 해당 데이터들 조회
        List<String> keys = ThumbnailKeys.allSuffixes().stream()
                .map(suffix -> baseKey + suffix)
                .toList();

        List<FileObject> all = fileObjectRepository.findByStorageKeyIn(keys);
        Map<String, FileObject> byKey = new HashMap<>();
        for (FileObject fo : all) byKey.put(fo.getStorageKey(), fo);

        // 5. 누락 체크
        List<String> missing = new ArrayList<>();
        for (String k : keys) {
            if (!byKey.containsKey(k)) missing.add(k);
        }
        if (!missing.isEmpty()) {
            throw new FileObjectNotFoundException("썸네일 파일이 누락되었습니다.: " + missing);
        }

        // 6. 포스터형 fileObject 조회
        FileObject posterOriginal = fileObjectRepository.findById(req.posterOriginalFileObjectId())
                .orElseThrow(() -> new FileObjectNotFoundException("포스터형 FileObject를 찾을 수 없습니다:" + req.posterOriginalFileObjectId()));

        // 7. CreationThumbnail 생성
        // 포스터형, 가로형 썸네일은 대표 이미지 이므로 displayOrder = 0
        // 포스터형 썸네일 - displayOrder = 0
        CreationThumbnail posterOriginalThumb = CreationThumbnail.create(
                creation,
                posterOriginal,
                CreationThumbnailType.POSTER,
                (short) 0,
                null
        );
        creation.addThumbnail(posterOriginalThumb);

        // 가로형 썸네일 - displayOrder = 0
        CreationThumbnail horizontalOriginalThumb = CreationThumbnail.create(
                creation,
                byKey.get(baseKey + ThumbnailKeys.HORIZONTAL_SUFFIX),
                CreationThumbnailType.HORIZONTAL,
                (short) 0,
                null
        );
        creation.addThumbnail(horizontalOriginalThumb);

        // 리사이징 이미지 6개 - displayOrder = 1..6, sourceImage = horizontalOriginalThumb
        short order = 1;
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

        // 8. 해시태그 조회: 자동완성으로 선택된 id들만 저장
        List<Hashtag> hashtags = hashtagRepository.findByIdIn(req.hashtagIds());

        // 누락된 해시태그 id 찾기
        if (hashtags.size() != req.hashtagIds().size()) {
            Set<Long> found = hashtags.stream().map(Hashtag::getId).collect(Collectors.toSet());
            Set<Long> missingHashtagIds = new HashSet<>(req.hashtagIds());

            missingHashtagIds.removeAll(found);
            throw new IllegalArgumentException("존재하지 않는 hashtagId가 포함되어 있습니다: " + missingHashtagIds);
        }

        for (Hashtag h : hashtags) { creation.addHashtag(h); }

        // 9. 최종 저장
        Creation saved = creationRepository.save(creation);
        return saved.getId();
    }
}
