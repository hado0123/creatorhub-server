package com.creatorhub.service;


import com.creatorhub.dto.episode.EpisodeRequest;
import com.creatorhub.dto.episode.EpisodeResponse;
import com.creatorhub.entity.*;
import com.creatorhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final ManuscriptImageService manuscriptImageService;
    private final EpisodeThumbnailService episodeThumbnailService;

    @Transactional
    public EpisodeResponse publishEpisode(EpisodeRequest req, Long memberId) {

        // 1. Creation 조회
        Creation creation = creationRepository.findById(req.creationId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Creation을 찾을 수 없습니다: " + req.creationId()));

        // 2. 인가 체크(로그인한 사용자가 해당 작품의 작가인지 확인)
        Long ownerMemberId = creation.getCreator().getMember().getId();
        if (!ownerMemberId.equals(memberId)) {
            throw new AccessDeniedException("해당 작품에 대한 회차 등록 권한이 없습니다.");
        }

        // 3. episodeNum 중복 체크(회차)
        if (episodeRepository.existsByCreationIdAndEpisodeNum(req.creationId(), req.episodeNum())) {
            throw new IllegalArgumentException("이미 존재하는 회차 번호입니다. creationId=" + req.creationId() + ", episodeNum=" + req.episodeNum());
        }

        // 4. 원고 displayOrder 중복 체크
        manuscriptImageService.validateDisplayOrders(req.manuscripts());

        // 5. fileObjectId 한번에 조회 (에피소드 '썸네일 + sns 썸네일 + 원고 이미지')
        Set<Long> allFileObjectIds = new HashSet<>(); // 중복제거
        req.manuscripts().forEach(m -> allFileObjectIds.add(m.fileObjectId()));
        allFileObjectIds.add(req.episodeFileObjectId());
        allFileObjectIds.add(req.snsFileObjectId());

        Map<Long, FileObject> fileObjectMap = findAllFileObjectsOrThrow(allFileObjectIds);


        // 6. episode 생성/저장
        Episode episode = Episode.create(
                creation,
                req.episodeNum(),
                req.title(),
                req.creatorNote(),
                req.isCommentEnabled(),
                req.isPublic()
        );
        Episode savedEpisode = episodeRepository.save(episode);

        // 7. manuscript_image 생성/저장
        List<ManuscriptImage> savedManuscripts = manuscriptImageService.createAndSave(req.manuscripts(), episode, fileObjectMap);

        // 8. episode_thumbnail 생성/저장
        List<EpisodeThumbnail> savedThumbnails = episodeThumbnailService.createAndSave(req, episode, fileObjectMap);

        // 9. 응답
        return new EpisodeResponse(
                savedEpisode.getId(),
                savedEpisode.getEpisodeNum(),
                savedManuscripts.stream().map(ManuscriptImage::getId).toList(),
                savedThumbnails.stream().map(EpisodeThumbnail::getId).toList()
        );
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

}
