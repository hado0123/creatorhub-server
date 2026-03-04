package com.creatorhub.service;

import com.creatorhub.constant.EpisodeThumbnailType;
import com.creatorhub.dto.episode.EpisodeRequest;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.EpisodeThumbnail;
import com.creatorhub.entity.FileObject;
import com.creatorhub.repository.EpisodeThumbnailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EpisodeThumbnailService {
    private final EpisodeThumbnailRepository episodeThumbnailRepository;

    /**
     * 에피소드 섬네일 저장
     */
    public List<EpisodeThumbnail> createAndSave(
            EpisodeRequest req,
            Episode episode,
            Map<Long, FileObject> fileObjectMap
    ) {
        FileObject episodeThumbFo = fileObjectMap.get(req.episodeFileObjectId());
        FileObject snsThumbFo = fileObjectMap.get(req.snsFileObjectId());

        return episodeThumbnailRepository.saveAll(List.of(
                EpisodeThumbnail.create(episodeThumbFo, episode, EpisodeThumbnailType.EPISODE),
                EpisodeThumbnail.create(snsThumbFo, episode, EpisodeThumbnailType.SNS)
        ));
    }
}
