package com.creatorhub.service;


import com.creatorhub.dto.episode.ManuscriptRegisterItem;
import com.creatorhub.entity.Episode;
import com.creatorhub.entity.FileObject;
import com.creatorhub.entity.ManuscriptImage;
import com.creatorhub.repository.ManuscriptImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManuscriptImageService {
    private final ManuscriptImageRepository manuscriptImageRepository;

    public void validateDisplayOrders(List<ManuscriptRegisterItem> manuscripts) {
        Set<Integer> seen = new HashSet<>();
        for (ManuscriptRegisterItem m : manuscripts) {
            if (!seen.add(m.displayOrder())) {
                throw new IllegalArgumentException("중복된 displayOrder가 있습니다: " + m.displayOrder());
            }
        }
    }

    public List<ManuscriptImage> createAndSave(
            List<ManuscriptRegisterItem> manuscripts,
            Episode episode,
            Map<Long, FileObject> fileObjectMap
    ) {
        List<ManuscriptImage> images = manuscripts.stream()
                .map(m -> {
                    FileObject fo = fileObjectMap.get(m.fileObjectId());
                    int order = m.displayOrder();
                    return ManuscriptImage.create(fo, episode, order);
                })
                .toList();

        return manuscriptImageRepository.saveAll(images);
    }
}
