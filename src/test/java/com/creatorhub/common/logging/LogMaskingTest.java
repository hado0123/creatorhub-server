package com.creatorhub.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskingTest {

    @Test
    @DisplayName("null 또는 blank면 (empty) 반환")
    void maskEmail_empty() {
        assertThat(LogMasking.maskEmail(null)).isEqualTo("(empty)");
        assertThat(LogMasking.maskEmail("")).isEqualTo("(empty)");
        assertThat(LogMasking.maskEmail("   ")).isEqualTo("(empty)");
    }

    @ParameterizedTest(name = "email={0} -> {1}")
    @CsvSource({
            // '@' 없거나, '@'가 첫 글자라 로컬파트가 비어있음
            "abc, ***",
            "'@domain.com', ***",

            // 로컬파트 길이 1
            "a@domain.com, *@domain.com",

            // 로컬파트 길이 2 이상: 앞 2글자 + *** + @ + domain
            "ab@domain.com, ab***@domain.com",
            "abcd@domain.com, ab***@domain.com",

            // 도메인이 비어있는 경우도 현재 로직상 허용됨
            "ab@, ab***@",
            "a@, *@"
    })
    @DisplayName("이메일 마스킹 규칙대로 변환")
    void maskEmail_cases(String email, String expected) {
        assertThat(LogMasking.maskEmail(email)).isEqualTo(expected);
    }
}