package com.creatorhub.exception.member;

import com.creatorhub.constant.ErrorCode;

public class InvalidPasswordException extends MemberException {
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }

    public InvalidPasswordException(String message) {
        super(ErrorCode.INVALID_PASSWORD, message);
    }
}
