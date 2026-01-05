package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;

public class CreatorNotFoundException extends CreatorException {

    public CreatorNotFoundException() {
        super(ErrorCode.CREATOR_NOT_FOUND);
    }

    public CreatorNotFoundException(String message) {
        super(ErrorCode.CREATOR_NOT_FOUND, message);
    }
}
