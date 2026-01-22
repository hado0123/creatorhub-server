package com.creatorhub.exception.member;

import com.creatorhub.constant.ErrorCode;

public class DuplicateEmailException extends MemberException {
    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }

    public DuplicateEmailException(String message) {
        super(ErrorCode.DUPLICATE_EMAIL, message);
    }
}
