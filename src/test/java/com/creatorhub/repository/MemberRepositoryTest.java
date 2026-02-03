package com.creatorhub.repository;

import com.creatorhub.constant.Gender;
import com.creatorhub.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("DB 제약 확인: 이메일이 중복이면 저장 시 DataIntegrityViolationException 발생")
    void saveDuplicateEmail() {
        Member m1 = Member.createMember("dup@test.com", "ENC1", "김회원", LocalDate.of(1990, 1, 1), Gender.FEMALE);
        memberRepository.saveAndFlush(m1);

        Member m2 = Member.createMember("dup@test.com", "ENC2", "박회원", LocalDate.of(1990, 2, 1), Gender.MALE);

        assertThatThrownBy(() -> memberRepository.saveAndFlush(m2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
