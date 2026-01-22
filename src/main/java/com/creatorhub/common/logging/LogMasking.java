package com.creatorhub.common.logging;

public final class LogMasking {

    private LogMasking() {}

    /**
     * 이메일 마스킹
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return "(empty)";

        int at = email.indexOf('@');
        if (at <= 0) return "***";          // '@' 없거나 로컬파트가 비어있음
        if (at == 1) return "*@" + email.substring(at + 1); // a@domain -> *@domain

        String local = email.substring(0, at);
        String domain = email.substring(at + 1);

        // local 앞 2글자만 남기고 나머지 마스킹 (2글자 미만이면 1글자만)
        int keep = 2;
        return local.substring(0, keep) + "***@" + domain;
    }
}