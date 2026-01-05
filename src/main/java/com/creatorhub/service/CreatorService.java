package com.creatorhub.service;

import com.creatorhub.constant.Role;
import com.creatorhub.dto.CreatorRequest;
import com.creatorhub.dto.CreatorResponse;
import com.creatorhub.entity.Creator;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.AlreadyCreatorException;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.CreatorRepository;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatorService {
    private final CreatorRepository creatorRepository;
    private final MemberRepository memberRepository;

    /**
     * 작가등록
     */
    @Transactional
    public CreatorResponse signup(Long memberId, CreatorRequest creatorRequest) {
        // 회원가입 정보 가져오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 이미 해당 Creator가 있으면 막기
        if (creatorRepository.existsByMemberId(memberId)) {
            throw new AlreadyCreatorException();
        }

        // Creator 테이블 저장
        Creator creator = Creator.createCreator(
                member,
                creatorRequest.creatorName(),
                creatorRequest.introduction()
        );

        Creator savedCreator = creatorRepository.save(creator);

        // Member 테이블에서 Role 정보 바꾸기
        member.changeRole(Role.CREATOR);

        log.info("작가등록 완료 - creatorName: {}, id: {}", savedCreator.getCreatorName(), savedCreator.getId());

        return CreatorResponse.from(savedCreator);
    }
}
