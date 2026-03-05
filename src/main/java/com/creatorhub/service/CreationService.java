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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public CursorSliceResponse<CreationListItem> getCreationsByDay(
            PublishDay day,
            CreationSort sort,
            SeekCursor cursor,
            int size
    ) {
        // 항상 0페이지 + size 만큼만 가져오는 이유:
        // OFFSET 방식이 아니라 SEEK(커서) 방식이므로
        // 앞에서 몇 개 건너뛰기가 아니라 조건으로 다음 구간을 가져온다
        Pageable pageable = PageRequest.of(0, size);

        final Long cursorValue =
                cursor == null ? null : cursor.value().longValue();
        final Long cursorId =
                cursor == null ? null : cursor.id();

        // 1. 정렬 기준에 따라 "집계 + 커서 조건" 쿼리 실행
        List<CreationSeekRow> rows = switch (sort) {
            // 조회순
            case VIEWS -> creationRepository.findByDayOrderByViewsSeek(
                    day,
                    // 커서가 없으면 첫 페이지 → null
                    // 있으면 이전 페이지의 마지막 조회수 합계
                    cursorValue,

                    // 동점(조회수 동일)일 때 기준이 되는 id
                    cursorId,
                    pageable
            );
            // 인기순
            case POPULAR -> creationRepository.findByDayOrderByLikesSeek(
                    day,
                    cursorValue,
                    cursorId,
                    pageable
            );
            // 별점순:
            // ratingAverage DESC → ratingCount DESC → id DESC
            case RATING -> creationRepository.findByDayOrderByRatingSeek(
                    day,
                    cursor == null ? null : cursor.value(),
                    cursor == null ? 0L : (cursor.tie() == null ? 0L : cursor.tie()),
                    cursorId,
                    pageable
            );
        };

        // 2. 더 이상 가져올 데이터가 없는 경우
        if (rows.isEmpty()) {
            return new CursorSliceResponse<>(List.of(), false, null);
        }

        // 집계 쿼리는 id + 정렬값만 가져왔기 때문에 실제 화면에 보여줄 title, poster 등을 위해 id 목록 추출
        List<Long> ids = rows.stream().map(CreationSeekRow::getId).toList();

        // 3. Creation 기본 정보 조회 (title 등)
        List<Creation> creations = creationRepository.findByIdIn(ids);
        Map<Long, Creation> creationById = creations.stream()
                .collect(Collectors.toMap(Creation::getId, Function.identity()));

        // 4. 대표 포스터 조회
        List<CreationThumbnail> posters = creationThumbnailRepository
                .findPostersByCreationIds(ids, CreationThumbnailType.POSTER);

        Map<Long, String> posterUrlByCreationId = posters.stream()
                .collect(Collectors.toMap(
                        ct -> ct.getCreation().getId(),
                        ct -> cloudfrontBase + "/" + ct.getFileObject().getStorageKey(),
                        (a, b) -> a
                ));

        // 5. ids 순서대로 DTO 생성(집계 쿼리의 정렬 순서를 그대로 유지하기 위함)
        List<CreationListItem> items = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Creation c = creationById.get(id);
            if (c == null) continue;

            items.add(new CreationListItem(
                    c.getId(),
                    c.getTitle(),
                    posterUrlByCreationId.get(c.getId())
            ));
        }

        // 6. 다음 페이지용 커서 생성
        // 마지막 행이 현재 페이지의 끝
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
