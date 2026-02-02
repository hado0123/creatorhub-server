package com.creatorhub.service;

import com.creatorhub.dto.CreationFavoriteResponse;
import com.creatorhub.dto.FavoriteCreationItem;
import com.creatorhub.entity.Creation;
import com.creatorhub.entity.CreationFavorite;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.CreationNotFoundException;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.CreationFavoriteRepository;
import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreationFavoriteService {
    private final CreationFavoriteRepository creationFavoriteRepository;
    private final CreationRepository creationRepository;
    private final MemberRepository memberRepository;

    /**
     * 관심 등록
     */
    @Transactional
    public CreationFavoriteResponse favorite(Long memberId, Long creationId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Creation creation = creationRepository.findById(creationId)
                .orElseThrow(CreationNotFoundException::new);

        // 유니크 + 예외로 멱등 처리
        try {
            // flush로 insert를 즉시 실행해 유니크(중복) 실패를 여기서 확정
            creationFavoriteRepository.saveAndFlush(CreationFavorite.create(member, creation));
        } catch (DataIntegrityViolationException e) {
            return CreationFavoriteResponse.of(creationId, true);
        }

        creationRepository.updateFavoriteCount(creationId, 1);

        return CreationFavoriteResponse.of(creationId, true);
    }

    /**
     * 관심 해제
     */
    @Transactional
    public CreationFavoriteResponse unfavorite(Long memberId, Long creationId) {
        // delete row count 기반 멱등 처리 (동시성에서도 count 정합성 보장)
        int deleted = creationFavoriteRepository
                .deleteByMemberIdAndCreationId(memberId, creationId);

        if (deleted > 0) {
            creationRepository.updateFavoriteCount(creationId, -1);
        }

        return CreationFavoriteResponse.of(creationId, false);
    }

    /**
     * 내 관심작품 목록 (최신 관심순)
     */
    public Page<FavoriteCreationItem> getMyFavorites(Long memberId, Pageable pageable) {
        return creationFavoriteRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(cf -> {
                    Creation c = cf.getCreation();
                    return new FavoriteCreationItem(
                            c.getId(),
                            c.getTitle(),
                            c.getPlot(),
                            c.isPublic(),
                            c.getFavoriteCount(),
                            cf.getCreatedAt()
                    );
                });
    }

}
