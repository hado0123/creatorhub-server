package com.creatorhub.exception.creator;

import com.creatorhub.constant.ErrorCode;

public class CreationNotFoundException extends CreatorException {

    public CreationNotFoundException() {
        super(ErrorCode.CREATION_NOT_FOUND);
    }

    public CreationNotFoundException(String message) {
        super(ErrorCode.CREATION_NOT_FOUND, message);
    }
}
