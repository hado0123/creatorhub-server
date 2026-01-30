package com.creatorhub.service;

import com.creatorhub.dto.CreationFavoriteResponse;
import com.creatorhub.dto.FavoriteCreationItem;
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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.NestedExceptionUtils;
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
    public CreationFavoriteResponse addFavorite(Long memberId, Long creationId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Creation creation = creationRepository.findById(creationId)
                .orElseThrow(CreationNotFoundException::new);

        boolean created = false; // 관심등록 됐는지 여부

        // 유니크 + 예외로 멱등 처리
        try {
            // flush로 INSERT를 즉시 실행해 유니크(중복) 실패를 여기서 확정
            creationFavoriteRepository.saveAndFlush(CreationFavorite.create(member, creation));
            created = true;
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateFavorite(e)) throw e; // 중복은 정상 처리하고, 그 외 예외는 전파되어 트랜잭션 자동 롤백
        }

        // 정합성 유지: 관심이 되었을때만 1증가
        if (created) {
            creationRepository.updateFavoriteCountSafely(creationId, 1);
        }

        int count = creationRepository.findFavoriteCount(creationId);
        return CreationFavoriteResponse.of(creationId, count, true, created);
    }

    private boolean isDuplicateFavorite(DataIntegrityViolationException e) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);

        if (root instanceof ConstraintViolationException cve) {
            return "uk_creation_favorite_member_creation".equalsIgnoreCase(cve.getConstraintName());
        }

        return false;
    }

    /**
     * 관심 해제
     */
    @Transactional
    public CreationFavoriteResponse removeFavorite(Long memberId, Long creationId) {
        CreationFavorite favorite = creationFavoriteRepository.findByMemberIdAndCreationId(memberId, creationId)
                .orElseThrow(CreationFavoriteNotFoundException::new);

        creationFavoriteRepository.delete(favorite);

        // favorite_count 컬럼값 감소
        creationRepository.updateFavoriteCountSafely(creationId, -1);

        int newCount = creationRepository.findFavoriteCount(creationId);
        return CreationFavoriteResponse.of(creationId, newCount, false, true);
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
