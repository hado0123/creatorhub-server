package com.creatorhub.service;

import com.creatorhub.dto.ManuscriptRegisterItem;
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
                    short order = safeToShort(m.displayOrder(), "displayOrder");
                    return ManuscriptImage.create(fo, episode, order);
                })
                .toList();

        return manuscriptImageRepository.saveAll(images);
    }

    private short safeToShort(Integer value, String fieldName) {
        if (value == null) throw new IllegalArgumentException(fieldName + "가 null입니다.");
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException(fieldName + " 범위가 short를 초과합니다: " + value);
        }
        return value.shortValue();
    }
}
