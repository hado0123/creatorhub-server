package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;

public class AlreadyCreatorException extends CreatorException {

    public AlreadyCreatorException() {
        super(ErrorCode.ALREADY_CREATOR);
    }

    public AlreadyCreatorException(String message) {
        super(ErrorCode.ALREADY_CREATOR, message);
    }
}
