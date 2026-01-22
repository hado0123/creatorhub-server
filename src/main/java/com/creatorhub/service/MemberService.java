package com.creatorhub.service;

import com.creatorhub.dto.member.MemberRequest;
import com.creatorhub.dto.member.MemberResponse;
import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.DuplicateEmailException;
import com.creatorhub.exception.InvalidPasswordException;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.creatorhub.common.logging.LogMasking.maskEmail;


@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    public MemberResponse signup(MemberRequest memberRequest) {
        // 이메일 중복 체크
        validateDuplicateMember(memberRequest);

        String encodedPassword = passwordEncoder.encode(memberRequest.password());

        Member member = Member.createMember(
                memberRequest.email(),
                encodedPassword,
                memberRequest.name(),
                memberRequest.birthday(),
                memberRequest.gender()
        );

        Member savedMember = memberRepository.save(member);

        log.info("회원가입 완료 - email: {}, memberId: {}", maskEmail(savedMember.getEmail()), savedMember.getId());

        return MemberResponse.from(savedMember);
    }


    /**
     * 이메일로 회원 중복 체크
     */
    private void validateDuplicateMember(MemberRequest memberRequest) {
        memberRepository.findByEmail(memberRequest.email())
            .ifPresent(member -> {
                throw new DuplicateEmailException();
            });
    }

    /**
     * 회원삭제
     */
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        memberRepository.delete(member);
        log.info("회원 삭제 완료 - email: {}, memberId: {}", maskEmail(member.getEmail()), member.getId());
    }

    /**
     * 비밀번호 인증 후 회원정보 가져옴
     */
    public TokenPayload authenticate(String email, String rawPassword) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new InvalidPasswordException();
        }

        log.info("회원 인증 완료 - email: {}, memberId: {}", maskEmail(member.getEmail()), member.getId());

        return TokenPayload.from(member);
    }
}
