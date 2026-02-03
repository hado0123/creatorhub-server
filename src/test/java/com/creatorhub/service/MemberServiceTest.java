package com.creatorhub.service;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.constant.Gender;
import com.creatorhub.constant.Role;
import com.creatorhub.dto.MemberRequest;
import com.creatorhub.dto.MemberResponse;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.DuplicateEmailException;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupSuccess() {
        MemberRequest memberRequest = new MemberRequest(
                "test@example.com",
                "test1234!",
                "홍길동",
                LocalDate.of(1990, 1, 15),
                Gender.MALE
        );

        MemberResponse memberResponse = memberService.signup(memberRequest);

        assertThat(memberResponse).isNotNull();
        assertThat(memberResponse.id()).isNotNull();
        assertThat(memberResponse.email()).isEqualTo("test@example.com");
        assertThat(memberResponse.name()).isEqualTo("홍길동");
        assertThat(memberResponse.birthday()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(memberResponse.gender()).isEqualTo(Gender.MALE);
        assertThat(memberResponse.role()).isEqualTo(Role.MEMBER);
        assertThat(memberResponse.createdAt()).isNotNull();

        Member savedMember = memberRepository.findById(memberResponse.id()).orElse(null);
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo("test@example.com");
    }



    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signupFailDuplicateEmail() {
        // 먼저 회원 1명 등록
        MemberRequest firstMember = new MemberRequest(
                "duplicate@example.com",
                "test1234!",
                "첫번째회원",
                LocalDate.of(1990, 1, 1),
                Gender.MALE
        );
        memberService.signup(firstMember);

        // 같은 이메일로 다시 회원가입 시도
        MemberRequest duplicateMember = new MemberRequest(
                "duplicate@example.com", // 중복 이메일
                "test5678!",
                "두번째회원",
                LocalDate.of(1995, 5, 5),
                Gender.FEMALE
        );

        assertThatThrownBy(() -> memberService.signup(duplicateMember))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }


    @Test
    @DisplayName("여러 회원 연속 가입 테스트")
    void signupMultipleMembers() {
        for (int i = 1; i <= 5; i++) {
            MemberRequest memberRequest = new MemberRequest(
                    "user" + i + "@example.com",
                    "test1234!",
                    "회원" + i,
                    LocalDate.of(1990 + i, 1, i),
                    i % 2 == 0 ? Gender.MALE : Gender.FEMALE
            );

            memberService.signup(memberRequest);
        }


        assertThat(memberRepository.findByEmail("user1@example.com")).isPresent();
        assertThat(memberRepository.findByEmail("user2@example.com")).isPresent();
        assertThat(memberRepository.findByEmail("user3@example.com")).isPresent();
        assertThat(memberRepository.findByEmail("user4@example.com")).isPresent();
        assertThat(memberRepository.findByEmail("user5@example.com")).isPresent();
    }

    @Test
    @DisplayName("회원 삭제 성공 테스트")
    void deleteMemberSuccess() {
        MemberRequest memberRequest = new MemberRequest(
                "delete@test.com",
                "test1234!",
                "김유리",
                LocalDate.of(1992, 3, 10),
                Gender.FEMALE
        );

        MemberResponse memberResponse = memberService.signup(memberRequest);
        Long memberId = memberResponse.id();

        memberService.deleteMember(memberId);

        assertThat(memberRepository.findById(memberId)).isEmpty();
    }

    @Test
    @DisplayName("회원 삭제 실패 - 존재하지 않는 회원")
    void deleteMemberFailNotFound() {
        Long invalidId = 9999L;

        assertThatThrownBy(() -> memberService.deleteMember(invalidId))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}
