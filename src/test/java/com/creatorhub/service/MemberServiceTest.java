package com.creatorhub.service;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.constant.Gender;
import com.creatorhub.dto.member.MemberRequest;
import com.creatorhub.dto.member.MemberResponse;
import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.member.DuplicateEmailException;
import com.creatorhub.exception.member.InvalidPasswordException;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks MemberService memberService;

    @Test
    @DisplayName("회원가입 실패: 이미 가입된 이메일이면 DuplicateEmailException 발생")
    void signupDuplicateEmail() {
        MemberRequest req = new MemberRequest(
                "dup@test.com",
                "pw1234",
                "김회원",
                LocalDate.of(1990, 1, 1),
                Gender.FEMALE
        );

        given(memberRepository.findByEmail(req.email()))
                .willReturn(Optional.of(mock(Member.class)));

        assertThatThrownBy(() -> memberService.signup(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());

        then(memberRepository).should(never()).save(any(Member.class));
        then(passwordEncoder).should(never()).encode(anyString());
    }

    @Test
    @DisplayName("회원가입 성공: 중복이 아니면 비밀번호 인코딩 후 저장하고 응답을 반환")
    void signupSuccess() {
        MemberRequest req = new MemberRequest(
                "new@test.com",
                "pw1234",
                "김회원",
                LocalDate.of(1990, 1, 1),
                Gender.FEMALE
        );

        given(memberRepository.findByEmail(req.email()))
                .willReturn(Optional.empty());

        given(passwordEncoder.encode(req.password()))
                .willReturn("ENC_PW");

        given(memberRepository.save(any(Member.class)))
                .willAnswer(inv -> inv.getArgument(0));

        MemberResponse resp = memberService.signup(req);

        InOrder inOrder = inOrder(memberRepository, passwordEncoder);
        inOrder.verify(memberRepository).findByEmail(req.email());
        inOrder.verify(passwordEncoder).encode(req.password());
        inOrder.verify(memberRepository).save(argThat(m ->
                req.email().equals(m.getEmail()) &&
                        "ENC_PW".equals(m.getPassword())
        ));

        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("회원인증 실패: 이메일에 해당하는 회원이 없으면 MemberNotFoundException 발생")
    void authenticateNotFound() {
        given(memberRepository.findByEmail("none@test.com"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.authenticate("none@test.com", "pw"))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("회원인증 실패: 비밀번호가 일치하지 않으면 InvalidPasswordException 발생")
    void authenticateWrongPassword() {
        Member member = mock(Member.class);
        given(member.getPassword()).willReturn("ENC_PW");

        given(memberRepository.findByEmail("a@test.com"))
                .willReturn(Optional.of(member));

        given(passwordEncoder.matches("wrong", "ENC_PW"))
                .willReturn(false);

        assertThatThrownBy(() -> memberService.authenticate("a@test.com", "wrong"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    @DisplayName("회원인증 성공: 비밀번호가 일치하면 TokenPayload 반환")
    void authenticateSuccess() {
        Member member = mock(Member.class);
        given(member.getPassword()).willReturn("ENC_PW");

        given(memberRepository.findByEmail("a@test.com"))
                .willReturn(Optional.of(member));

        given(passwordEncoder.matches("pw1234", "ENC_PW"))
                .willReturn(true);

        TokenPayload payload = memberService.authenticate("a@test.com", "pw1234");

        assertThat(payload).isNotNull();
        then(passwordEncoder).should(times(1)).matches("pw1234", "ENC_PW");
    }

    @Test
    @DisplayName("회원삭제 실패: 회원이 없으면 MemberNotFoundException 발생")
    void deleteNotFound() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(999L))
                .isInstanceOf(MemberNotFoundException.class);

        then(memberRepository).should(never()).delete(any(Member.class));
    }

    @Test
    @DisplayName("회원삭제 성공: 회원이 존재하면 delete 호출")
    void deleteSuccess() {
        Member member = mock(Member.class);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.deleteMember(1L);

        then(memberRepository).should(times(1)).delete(member);
    }
}
