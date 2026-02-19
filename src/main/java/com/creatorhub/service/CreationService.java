package com.creatorhub.service;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.constant.PublishDay;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.creation.CreationListResponse;
import com.creatorhub.dto.creation.CreationRequest;
import com.creatorhub.dto.creation.CreationResponse;
import com.creatorhub.dto.creation.CreationsByDayResponse;
import com.creatorhub.entity.*;
import com.creatorhub.exception.creation.CreationNotFoundException;
import com.creatorhub.exception.creator.CreatorNotFoundException;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.exception.hashtag.HashtagNotFoundException;
import com.creatorhub.repository.*;
import com.creatorhub.repository.projection.CreationBaseProjection;
import com.creatorhub.repository.projection.HashtagTitleProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreationService {
    private final CreationRepository creationRepository;
    private final FileObjectRepository fileObjectRepository;
    private final CreatorRepository creatorRepository;
    private final HashtagRepository hashtagRepository;
    private final CreationThumbnailRepository creationThumbnailRepository;
    private final CreationHashtagRepository creationHashtagRepository;

    @Value("${cloud.front.base}")
    private String cloudfrontBase;

    /**
     * 작품 등록
     */
    @Transactional
    public Long createCreation(CreationRequest req, Long id) {

        // 1. 작가 조회
        Creator creator = creatorRepository.findByMemberId(id)
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

    /**
     * 하나의 작품 조회
     */
    public CreationResponse getCreation(Long creationId) {
        CreationBaseProjection base = creationRepository
                .findCreationDetailBase(creationId, CreationThumbnailType.POSTER)
                .orElseThrow(() ->
                        new CreationNotFoundException("해당 Creation을 찾을 수 없습니다: " + creationId)
                );

        List<String> hashtags = creationHashtagRepository
                .findHashtagTitlesByCreationId(creationId)
                .stream()
                .map(HashtagTitleProjection::getTitle)
                .toList();

        return CreationResponse.from(base, cloudfrontBase, hashtags);
    }

    /**
     * 특정 작가가 등록한 모든 작품 조회
     */
    public List<CreationListResponse> getMyCreations(Long memberId) {
        Creator creator = creatorRepository.findByMemberId(memberId)
                .orElseThrow(CreatorNotFoundException::new);

        return creationRepository
                .findAllByCreatorIdWithThumbnails(
                        creator.getId(),
                        CreationThumbnailType.POSTER
                )
                .stream()
                .map(p -> CreationListResponse.from(p, cloudfrontBase))
                .toList();
    }

    /**
     * 모든 요일별 연재 작품 조회 (한 번에)
     */
    public CreationsByDayResponse getAllCreationsByDay() {
        List<Long> ids = creationRepository.findPublicCreationIdsOrderByIdDesc();
        if (ids.isEmpty()) {
            return emptyResponse();
        }

        // 1. 요일만 로딩
        List<Creation> creations = creationRepository.findWithPublishDaysByIdIn(ids);

        // 2. 대표 포스터(사이즈 1개)만 로딩
        List<CreationThumbnail> posters = creationThumbnailRepository
                .findByCreationIdsAndTypeAndSizeType(
                        ids,
                        CreationThumbnailType.POSTER
                );

        // creationId -> posterUrl
        Map<Long, String> posterUrlByCreationId = posters.stream()
                .collect(Collectors.toMap(
                        ct -> ct.getCreation().getId(),
                        ct -> cloudfrontBase + "/" +ct.getFileObject().getStorageKey(),
                        (a, b) -> a
                ));

        // id -> creation (정렬 유지하려고 맵으로)
        Map<Long, Creation> creationById = creations.stream()
                .collect(Collectors.toMap(Creation::getId, Function.identity()));

        EnumMap<PublishDay, List<CreationsByDayResponse.CreationByDayItem>> result =
                new EnumMap<>(PublishDay.class);

        for (PublishDay day : PublishDay.values()) {
            result.put(day, new ArrayList<>());
        }

        // ids 순서대로 처리하면 id desc 정렬 유지됨
        for (Long id : ids) {
            Creation c = creationById.get(id);
            if (c == null) continue;

            var item = new CreationsByDayResponse.CreationByDayItem(
                    c.getId(),
                    c.getTitle(),
                    posterUrlByCreationId.get(c.getId())
            );

            for (PublishDay day : c.getPublishDays()) {
                result.get(day).add(item);
            }
        }

        return new CreationsByDayResponse(result);

    }

    private CreationsByDayResponse emptyResponse() {
        EnumMap<PublishDay, List<CreationsByDayResponse.CreationByDayItem>> map =
                new EnumMap<>(PublishDay.class);
        for (PublishDay day : PublishDay.values()) {
            map.put(day, new ArrayList<>());
        }
        return new CreationsByDayResponse(map);
    }
}
