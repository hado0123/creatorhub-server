package com.creatorhub.service;

import com.creatorhub.dto.creation.CreationFavoriteResponse;
import com.creatorhub.dto.creation.favorite.FavoriteCreationItem;
import com.creatorhub.entity.Creation;
import com.creatorhub.entity.CreationFavorite;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.CreationFavoriteNotFoundException;
import com.creatorhub.exception.CreationNotFoundException;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.CreationFavoriteRepository;
import com.creatorhub.repository.CreationRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreationFavoriteService {
    private final CreationFavoriteRepository creationFavoriteRepository;
    private final CreationRepository creationRepository;
    private final MemberRepository memberRepository;

    /**
     * 관심 등록
     */
    @Transactional
    public CreationFavoriteResponse favorite(Long memberId, Long creationId) {
        // 존재 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Creation creation = creationRepository.findById(creationId)
                .orElseThrow(CreationNotFoundException::new);

        // 이미 관심이면 그대로 반환
        if (creationFavoriteRepository.existsByMemberIdAndCreationId(memberId, creationId)) {
            Integer count = creation.getFavoriteCount();
            return CreationFavoriteResponse.of(creationId, count == null ? 0 : count, true);
        }

        // 관심 등록
        creationFavoriteRepository.save(CreationFavorite.create(member, creation));

        // favorite_count 컬럼값 증가
        creationRepository.updateFavoriteCountSafely(creationId, +1);

        // 최신 카운트 다시 읽기(정확한 값 응답용)
        Integer newCount = creationRepository.findById(creationId)
                .map(c -> c.getFavoriteCount() == null ? 0 : c.getFavoriteCount())
                .orElse(0);

        return CreationFavoriteResponse.of(creationId, newCount, true);
    }

    /**
     * 관심 해제
     */
    @Transactional
    public CreationFavoriteResponse unfavorite(Long memberId, Long creationId) {
        CreationFavorite favorite = creationFavoriteRepository.findByMemberIdAndCreationId(memberId, creationId)
                .orElseThrow(CreationFavoriteNotFoundException::new);

        creationFavoriteRepository.delete(favorite);

        // favorite_count 컬럼값 감소
        creationRepository.updateFavoriteCountSafely(creationId, -1);

        // 최신 카운트 다시 읽기(정확한 값 응답용)
        Integer newCount = creationRepository.findById(creationId)
                .map(c -> c.getFavoriteCount() == null ? 0 : c.getFavoriteCount())
                .orElse(0);

        return CreationFavoriteResponse.of(creationId, newCount, false);
    }

    /**
     * 내 관심작품 목록 (최신 관심순)
     */
    public Page<FavoriteCreationItem> getMyFavorites(Long memberId, Pageable pageable) {
        return creationFavoriteRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(cf -> {
                    Creation c = cf.getCreation();
                    Integer cnt = c.getFavoriteCount() == null ? 0 : c.getFavoriteCount();
                    return new FavoriteCreationItem(
                            c.getId(),
                            c.getTitle(),
                            c.getPlot(),
                            c.isPublic(),
                            cnt,
                            cf.getCreatedAt()
                    );
                });
    }
}
