package com.creatorhub.service;

import com.creatorhub.constant.CreationSort;
import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.constant.PublishDay;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.creation.*;
import com.creatorhub.entity.*;
import com.creatorhub.exception.creation.CreationNotFoundException;
import com.creatorhub.exception.creator.CreatorNotFoundException;
import com.creatorhub.exception.fileUpload.FileObjectNotFoundException;
import com.creatorhub.exception.hashtag.HashtagNotFoundException;
import com.creatorhub.repository.*;
import com.creatorhub.repository.projection.CreationBaseProjection;
import com.creatorhub.repository.projection.CreationSeekRow;
import com.creatorhub.repository.projection.HashtagTitleProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    public CursorSliceResponse<CreationListItem> getCreationsByDay(
            PublishDay day,
            CreationSort sort,
            SeekCursor cursor,
            int size
    ) {
        final String dayName = day.name();
        final Long cursorValue =
                cursor == null ? null : cursor.value().longValue();
        final Long cursorId =
                cursor == null ? null : cursor.id();

        // 1. 네이티브 seek 쿼리 1회로 id + 정렬값 + title + poster 모두 조회
        List<CreationSeekRow> rows = switch (sort) {
            case VIEWS -> creationRepository.findByDayOrderByViewsSeek(
                    dayName, cursorValue, cursorId, size
            );
            case POPULAR -> creationRepository.findByDayOrderByLikesSeek(
                    dayName, cursorValue, cursorId, size
            );
            case RATING -> creationRepository.findByDayOrderByRatingSeek(
                    dayName,
                    cursor == null ? null : cursor.value(),
                    cursor == null ? 0L : (cursor.tie() == null ? 0L : cursor.tie()),
                    cursorId,
                    size
            );
        };

        // 2. 더 이상 가져올 데이터가 없는 경우
        if (rows.isEmpty()) {
            return new CursorSliceResponse<>(List.of(), false, null);
        }

        // 3. 쿼리 결과 순서 그대로 DTO 변환
        List<CreationListItem> items = rows.stream()
                .map(row -> new CreationListItem(
                        row.getId(),
                        row.getTitle(),
                        row.getStorageKey() != null
                                ? cloudfrontBase + "/" + row.getStorageKey()
                                : null
                ))
                .toList();

        // 4. 다음 페이지용 커서 생성
        CreationSeekRow last = rows.getLast();

        SeekCursor nextCursor = switch (sort) {
            case VIEWS, POPULAR -> new SeekCursor(
                    last.getId(),
                    last.getLongValue().doubleValue(), // 통일
                    null
            );
            case RATING -> new SeekCursor(
                    last.getId(),
                    last.getDoubleValue(),
                    last.getTie()
            );
        };

        // size만큼 꽉 찼으면 다음 페이지가 있을 가능성 있음
        boolean hasNext = rows.size() == size;

        // 7. 최종 반환
        CursorSliceResponse<CreationListItem> response = new CursorSliceResponse<>(items, hasNext, nextCursor);

        log.debug("getCreationsByDay response: itemCount={}, hasNext={}, nextCursor={}", response.items().size(), response.hasNext(), response.nextCursor());
        return response;
    }
}
