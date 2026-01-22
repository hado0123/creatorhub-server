package com.creatorhub.service;

import com.creatorhub.constant.EpisodeThumbnailType;
import com.creatorhub.dto.episode.EpisodeRequest;
import com.creatorhub.dto.episode.EpisodeResponse;
import com.creatorhub.dto.episode.ManuscriptRegisterItem;
import com.creatorhub.entity.*;
import com.creatorhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpisodeService {
    private final CreationRepository creationRepository;
    private final EpisodeRepository episodeRepository;
    private final FileObjectRepository fileObjectRepository;
    private final ManuscriptImageRepository manuscriptImageRepository;
    private final EpisodeThumbnailRepository episodeThumbnailRepository;

    @Transactional
    public EpisodeResponse publishEpisode(EpisodeRequest req) {

        // 1. Creation 조회
        Creation creation = creationRepository.findById(req.creationId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Creation을 찾을 수 없습니다: " + req.creationId()));

        // 2. episodeNum 중복 체크(회차)
        if (episodeRepository.existsByCreationIdAndEpisodeNum(req.creationId(), req.episodeNum())) {
            throw new IllegalArgumentException("이미 존재하는 회차 번호입니다. creationId=" + req.creationId() + ", episodeNum=" + req.episodeNum());
        }

        // 3. 원고 displayOrder 중복 체크
        ensureNoDuplicateDisplayOrder(req.manuscripts());

        // 4. fileObjectId 전체 모아서 한번에 조회 (에피소드 썸네일 + sns 썸네일 2개)
        Set<Long> allFileObjectIds = new HashSet<>();
        req.manuscripts().forEach(m -> allFileObjectIds.add(m.fileObjectId()));
        allFileObjectIds.add(req.episodeFileObjectId());
        allFileObjectIds.add(req.snsFileObjectId());

        Map<Long, FileObject> fileObjectMap = findAllFileObjectsOrThrow(allFileObjectIds);


        // 5. episode 생성/저장
        Episode episode = Episode.create(
                creation,
                req.episodeNum(),
                req.title(),
                req.creatorNote(),
                req.isCommentEnabled(),
                req.isPublic()
        );
        Episode savedEpisode = episodeRepository.save(episode);

        // 6. manuscript_image 생성/저장
        List<ManuscriptImage> manuscriptImages = req.manuscripts().stream()
                .map(m -> {
                    FileObject fo = fileObjectMap.get(m.fileObjectId());
                    short order = safeToShort(m.displayOrder(), "displayOrder");
                    return ManuscriptImage.create(fo, savedEpisode, order);
                })
                .toList();

        List<ManuscriptImage> savedManuscripts = manuscriptImageRepository.saveAll(manuscriptImages);

        // 7. episode_thumbnail 생성/저장
        FileObject episodeThumbFo = fileObjectMap.get(req.episodeFileObjectId());
        FileObject snsThumbFo = fileObjectMap.get(req.snsFileObjectId());

        // 같은 fileObjectId를 둘 다 넣는 실수 방지
        if (Objects.equals(req.episodeFileObjectId(), req.snsFileObjectId())) {
            throw new IllegalArgumentException("episodeFileObjectId와 snsFileObjectId는 서로 달라야 합니다.");
        }

        List<EpisodeThumbnail> thumbnails = List.of(
                EpisodeThumbnail.create(episodeThumbFo, savedEpisode, EpisodeThumbnailType.EPISODE),
                EpisodeThumbnail.create(snsThumbFo, savedEpisode, EpisodeThumbnailType.SNS)
        );

        List<EpisodeThumbnail> savedThumbnails = episodeThumbnailRepository.saveAll(thumbnails);

        // 8. 응답
        return new EpisodeResponse(
                savedEpisode.getId(),
                savedEpisode.getEpisodeNum(),
                savedManuscripts.stream().map(ManuscriptImage::getId).toList(),
                savedThumbnails.stream().map(EpisodeThumbnail::getId).toList()
        );
    }

    private void ensureNoDuplicateDisplayOrder(List<ManuscriptRegisterItem> manuscripts) {
        Set<Integer> seen = new HashSet<>();
        for (ManuscriptRegisterItem m : manuscripts) {
            if (!seen.add(m.displayOrder())) {
                throw new IllegalArgumentException("중복된 displayOrder가 있습니다: " + m.displayOrder());
            }
        }
    }

    private Map<Long, FileObject> findAllFileObjectsOrThrow(Set<Long> ids) {
        List<FileObject> found = fileObjectRepository.findAllById(ids);

        if (found.size() != ids.size()) {
            Set<Long> foundIds = found.stream().map(FileObject::getId).collect(Collectors.toSet());
            List<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).sorted().toList();
            throw new IllegalArgumentException("존재하지 않는 fileObjectId가 있습니다: " + missing);
        }

        return found.stream().collect(Collectors.toMap(FileObject::getId, fo -> fo));
    }

    private short safeToShort(Integer value, String fieldName) {
        if (value == null) throw new IllegalArgumentException(fieldName + "가 null입니다.");
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException(fieldName + " 범위가 short를 초과합니다: " + value);
        }
        return value.shortValue();
    }

}
