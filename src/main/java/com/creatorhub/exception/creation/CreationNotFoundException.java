package com.creatorhub.exception.creation;

import com.creatorhub.constant.ErrorCode;

public class CreationNotFoundException extends CreationException {

    public CreationNotFoundException() {
        super(ErrorCode.CREATION_NOT_FOUND);
    }

    public CreationNotFoundException(String message) {
        super(ErrorCode.CREATION_NOT_FOUND, message);
    }
}
